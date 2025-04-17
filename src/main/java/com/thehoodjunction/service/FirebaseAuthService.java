package com.thehoodjunction.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import com.thehoodjunction.model.User;
import com.thehoodjunction.repository.jpa.UserRepository;
import com.thehoodjunction.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FirebaseAuthService {

    private final FirebaseAuth firebaseAuth;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    /**
     * Authenticate a user with Firebase ID token
     * 
     * @param idToken Firebase ID token
     * @return User if authentication is successful
     * @throws FirebaseAuthException if token is invalid
     */
    @Transactional
    public User authenticateWithFirebase(String idToken) throws FirebaseAuthException {
        // Verify the ID token
        FirebaseToken decodedToken = firebaseAuth.verifyIdToken(idToken);
        String uid = decodedToken.getUid();
        String email = decodedToken.getEmail();
        // Firebase token doesn't directly expose phone number in this way
        String phoneNumber = null;
        // Try to get phone number from claims if available
        if (decodedToken.getClaims().containsKey("phone_number")) {
            phoneNumber = (String) decodedToken.getClaims().get("phone_number");
        }
        String name = decodedToken.getName();
        
        // Check if user exists by email (if provided)
        Optional<User> userOptional = Optional.empty();
        if (email != null && !email.isEmpty()) {
            userOptional = userRepository.findByEmail(email);
        }
        
        // If not found by email and phone number is provided, try to find by phone number
        if (userOptional.isEmpty() && phoneNumber != null && !phoneNumber.isEmpty()) {
            userOptional = userRepository.findByPhoneNumber(phoneNumber);
        }
        
        User user;
        if (userOptional.isPresent()) {
            // Update existing user
            user = userOptional.get();
            // Update user information if needed
            if (name != null && !name.isEmpty() && (user.getFullName() == null || user.getFullName().isEmpty())) {
                user.setFullName(name);
            }
            
            // Set Firebase user ID if not already set
            if (user.getFirebaseUserId() == null || user.getFirebaseUserId().isEmpty()) {
                user.setFirebaseUserId(uid);
            }
        } else {
            // Create new user
            String username = email != null ? email : (phoneNumber != null ? phoneNumber : uid);
            String randomPassword = UUID.randomUUID().toString();
            
            Set<String> roles = new HashSet<>();
            roles.add("USER");
            
            user = User.builder()
                    .username(username)
                    .email(email)
                    .phoneNumber(phoneNumber)
                    .firebaseUserId(uid)
                    .password(passwordEncoder.encode(randomPassword))
                    .fullName(name != null ? name : "")
                    .roles(roles)
                    .build();
        }
        
        // Save or update user
        user = userRepository.save(user);
        
        // Set authentication in security context
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        return user;
    }
    
    /**
     * Generate a JWT token for the authenticated user
     * 
     * @param user The authenticated user
     * @return JWT token
     */
    public String generateToken(User user) {
        return jwtTokenProvider.generateToken(user);
    }
    
    /**
     * Get user information from Firebase
     * 
     * @param uid Firebase user ID
     * @return UserRecord containing user information
     * @throws FirebaseAuthException if user doesn't exist or other Firebase error
     */
    public UserRecord getUserInfo(String uid) throws FirebaseAuthException {
        return firebaseAuth.getUser(uid);
    }
}
