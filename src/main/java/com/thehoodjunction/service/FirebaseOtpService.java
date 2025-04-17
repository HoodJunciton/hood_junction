package com.thehoodjunction.service;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.SessionCookieOptions;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.auth.UserRecord.CreateRequest;
import com.thehoodjunction.dto.FirebaseOtpResponse;
import com.thehoodjunction.model.User;
import com.thehoodjunction.repository.jpa.UserRepository;
import com.thehoodjunction.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class FirebaseOtpService {

    private final FirebaseAuth firebaseAuth;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    
    // In-memory storage for OTP verification codes (in production, use Redis or another distributed cache)
    private final Map<String, OtpData> otpStorage = new ConcurrentHashMap<>();
    
    // OTP expiration time in minutes
    private static final int OTP_EXPIRATION_MINUTES = 10;
    
    // Class to store OTP data
    private static class OtpData {
        private final String otp;
        private final long expirationTime;
        
        public OtpData(String otp) {
            this.otp = otp;
            this.expirationTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(OTP_EXPIRATION_MINUTES);
        }
        
        public boolean isValid(String otpToVerify) {
            return otp.equals(otpToVerify) && System.currentTimeMillis() < expirationTime;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() >= expirationTime;
        }
    }

    /**
     * Verify if a phone number exists in Firebase
     * 
     * @param phoneNumber The phone number to check
     * @return true if the phone number exists, false otherwise
     */
    public boolean verifyPhoneNumberExists(String phoneNumber) {
        try {
            UserRecord userRecord = firebaseAuth.getUserByPhoneNumber(phoneNumber);
            return userRecord != null;
        } catch (FirebaseAuthException e) {
            log.debug("Phone number not found in Firebase: {}", phoneNumber);
            return false;
        }
    }
    
    /**
     * Generate and send OTP to a phone number
     * 
     * @param phoneNumber The phone number to send OTP to
     * @return FirebaseOtpResponse with verification details
     */
    public FirebaseOtpResponse generateAndSendOtp(String phoneNumber) {
        // Generate a random 6-digit OTP
        String otp = String.format("%06d", (int)(Math.random() * 1000000));
        
        // Store OTP in memory with expiration time
        otpStorage.put(phoneNumber, new OtpData(otp));
        
        // In a real implementation, you would send the OTP via SMS using a service like Twilio
        // For this example, we'll just log it
        log.info("Generated OTP {} for phone number {}", otp, phoneNumber);
        
        // Check if user already exists
        boolean exists = verifyPhoneNumberExists(phoneNumber);
        
        // Generate a verification ID (in a real implementation, this would be more secure)
        String verificationId = UUID.randomUUID().toString();
        
        return FirebaseOtpResponse.builder()
                .success(true)
                .message(exists ? 
                        "OTP sent to existing user." : 
                        "OTP sent to new user.")
                .phoneNumber(phoneNumber)
                .verificationId(verificationId)
                .build();
    }
    
    /**
     * Verify OTP for a phone number
     * 
     * @param phoneNumber The phone number
     * @param otp The OTP to verify
     * @param verificationId The verification ID
     * @return true if OTP is valid, false otherwise
     */
    public boolean verifyOtp(String phoneNumber, String otp, String verificationId) {
        OtpData otpData = otpStorage.get(phoneNumber);
        
        if (otpData == null) {
            log.warn("No OTP found for phone number: {}", phoneNumber);
            return false;
        }
        
        if (otpData.isExpired()) {
            log.warn("OTP expired for phone number: {}", phoneNumber);
            otpStorage.remove(phoneNumber);
            return false;
        }
        
        boolean isValid = otpData.isValid(otp);
        
        if (isValid) {
            // Remove OTP from storage after successful verification
            otpStorage.remove(phoneNumber);
            log.info("OTP verified successfully for phone number: {}", phoneNumber);
        } else {
            log.warn("Invalid OTP for phone number: {}", phoneNumber);
        }
        
        return isValid;
    }
    
    /**
     * Create a custom token for a phone number after OTP verification
     * 
     * @param phoneNumber The verified phone number
     * @return Custom token for Firebase authentication
     * @throws FirebaseAuthException if token creation fails
     */
    public String createCustomToken(String phoneNumber) throws FirebaseAuthException {
        // Check if user exists in Firebase
        UserRecord userRecord;
        
        try {
            // Try to get existing user
            userRecord = firebaseAuth.getUserByPhoneNumber(phoneNumber);
        } catch (FirebaseAuthException e) {
            // User doesn't exist, create a new one
            CreateRequest request = new CreateRequest()
                    .setPhoneNumber(phoneNumber);
            
            userRecord = firebaseAuth.createUser(request);
            log.info("Created new Firebase user with phone: {}, UID: {}", phoneNumber, userRecord.getUid());
        }
        
        // Create custom claims if needed
        Map<String, Object> claims = new HashMap<>();
        claims.put("phone_verified", true);
        
        // Create and return custom token
        return firebaseAuth.createCustomToken(userRecord.getUid(), claims);
    }

    /**
     * Create or update a user after successful phone authentication
     * 
     * @param phoneNumber The verified phone number
     * @param firebaseUid The Firebase UID of the user
     * @return The created or updated user
     */
    @Transactional
    public User createOrUpdateUserFromPhone(String phoneNumber, String firebaseUid) {
        // Check if user exists by phone number
        Optional<User> existingUser = userRepository.findByPhoneNumber(phoneNumber);
        
        if (existingUser.isPresent()) {
            // Update existing user with Firebase UID if not already set
            User user = existingUser.get();
            if (user.getFirebaseUserId() == null) {
                user.setFirebaseUserId(firebaseUid);
                return userRepository.save(user);
            }
            return user;
        } else {
            // Create new user
            String username = phoneNumber;
            String randomPassword = UUID.randomUUID().toString();
            
            Set<String> roles = new HashSet<>();
            roles.add("USER");
            
            User newUser = User.builder()
                    .username(username)
                    .phoneNumber(phoneNumber)
                    .firebaseUserId(firebaseUid)
                    .password(passwordEncoder.encode(randomPassword))
                    .fullName("")
                    .roles(roles)
                    .build();
            
            return userRepository.save(newUser);
        }
    }
    
    /**
     * Authenticate a user after successful OTP verification
     * 
     * @param phoneNumber The verified phone number
     * @return The authenticated user with JWT token
     * @throws FirebaseAuthException if authentication fails
     */
    @Transactional
    public Map<String, Object> authenticateUserWithPhone(String phoneNumber) throws FirebaseAuthException {
        // Get or create Firebase user
        UserRecord userRecord;
        try {
            userRecord = firebaseAuth.getUserByPhoneNumber(phoneNumber);
        } catch (FirebaseAuthException e) {
            // Create new Firebase user if not exists
            CreateRequest request = new CreateRequest()
                    .setPhoneNumber(phoneNumber);
            userRecord = firebaseAuth.createUser(request);
        }
        
        // Create or update user in our database
        User user = createOrUpdateUserFromPhone(phoneNumber, userRecord.getUid());
        
        // Generate JWT token
        String jwtToken = jwtTokenProvider.generateToken(user);
        
        // Return authentication result
        Map<String, Object> result = new HashMap<>();
        result.put("token", jwtToken);
        result.put("user", user);
        result.put("firebaseUid", userRecord.getUid());
        
        return result;
    }
    
    /**
     * Get user information from Firebase by phone number
     * 
     * @param phoneNumber The phone number to look up
     * @return The Firebase UserRecord or null if not found
     */
    public UserRecord getUserByPhoneNumber(String phoneNumber) {
        try {
            return firebaseAuth.getUserByPhoneNumber(phoneNumber);
        } catch (FirebaseAuthException e) {
            log.error("Error getting user by phone number: {}", phoneNumber, e);
            return null;
        }
    }
}
