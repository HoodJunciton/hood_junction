package com.thehoodjunction.controller;

import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.thehoodjunction.dto.AuthResponse;
import com.thehoodjunction.dto.FirebaseAuthRequest;
import com.thehoodjunction.dto.FirebaseOtpRequest;
import com.thehoodjunction.dto.FirebaseOtpResponse;
import com.thehoodjunction.dto.FirebaseOtpVerifyRequest;
import com.thehoodjunction.dto.ServerOtpVerifyRequest;
import com.thehoodjunction.model.User;
import com.thehoodjunction.service.FirebaseAuthService;
import com.thehoodjunction.service.FirebaseOtpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Controller for handling Firebase OTP operations
 * This controller supports both client-side and server-side OTP verification
 */
@RestController
@RequestMapping("/otp/firebase")
@RequiredArgsConstructor
@Slf4j
public class FirebaseOtpController {

    private final FirebaseOtpService firebaseOtpService;
    private final FirebaseAuthService firebaseAuthService;

    /**
     * Initiate the OTP process for a phone number using client-side Firebase SDK
     * Note: The actual OTP sending is handled by Firebase on the client side
     * This endpoint just checks if the phone number is valid and returns a response
     */
    @PostMapping("/client/send")
    public ResponseEntity<FirebaseOtpResponse> sendClientOtp(@Valid @RequestBody FirebaseOtpRequest request) {
        String phoneNumber = request.getPhoneNumber();
        
        // Check if the phone number is valid
        boolean exists = firebaseOtpService.verifyPhoneNumberExists(phoneNumber);
        
        FirebaseOtpResponse response = FirebaseOtpResponse.builder()
                .success(true)
                .message(exists ? 
                        "Phone number exists. Proceed with verification." : 
                        "New phone number. Proceed with verification.")
                .phoneNumber(phoneNumber)
                .verificationId("client-side-verification-id") // This is actually generated on client side
                .build();
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Initiate the OTP process for a phone number using server-side verification
     * The OTP is generated on the server and would typically be sent via SMS
     * In this implementation, the OTP is logged to the console for testing purposes
     */
    @PostMapping("/send")
    public ResponseEntity<FirebaseOtpResponse> sendOtp(@Valid @RequestBody FirebaseOtpRequest request) {
        String phoneNumber = request.getPhoneNumber();
        
        // Generate and send OTP
        FirebaseOtpResponse response = firebaseOtpService.generateAndSendOtp(phoneNumber);
        
        // Log the response to ensure verification ID is included
        log.info("Sending OTP response: {}", response);
        
        // Double-check that verification ID is set
        if (response.getVerificationId() == null) {
            String verificationId = UUID.randomUUID().toString();
            log.warn("Verification ID was null, generating a new one: {}", verificationId);
            response.setVerificationId(verificationId);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Verify an OTP for a phone number using client-side Firebase SDK
     * The client should send the Firebase ID token after successful verification
     */
    @PostMapping("/client/verify-token")
    public ResponseEntity<AuthResponse> verifyClientOtpWithToken(@Valid @RequestBody FirebaseAuthRequest request) {
        try {
            // Verify the Firebase ID token
            User user = firebaseAuthService.authenticateWithFirebase(request.getIdToken());
            
            // Generate JWT token
            String jwt = firebaseAuthService.generateToken(user);
            
            AuthResponse response = AuthResponse.builder()
                    .token(jwt)
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .build();
            
            return ResponseEntity.ok(response);
        } catch (FirebaseAuthException e) {
            log.error("Firebase authentication error", e);
            throw new RuntimeException("Authentication failed: " + e.getMessage());
        }
    }
    
    /**
     * Verify an OTP for a phone number using server-side verification
     * This endpoint verifies the OTP that was generated and sent by the server
     */
    @PostMapping("/verify")
    public ResponseEntity<AuthResponse> verifyOtp(@Valid @RequestBody ServerOtpVerifyRequest request) {
        String phoneNumber = request.getPhoneNumber();
        String otp = request.getOtp();
        String verificationId = request.getVerificationId();
        
        // Verify OTP
        boolean isValid = firebaseOtpService.verifyOtp(phoneNumber, otp, verificationId);
        
        if (!isValid) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponse.builder()
                            .success(false)
                            .message("Invalid OTP or OTP expired")
                            .build());
        }
        
        try {
            // Authenticate user
            Map<String, Object> authResult = firebaseOtpService.authenticateUserWithPhone(phoneNumber);
            User user = (User) authResult.get("user");
            String token = (String) authResult.get("token");
            
            AuthResponse response = AuthResponse.builder()
                    .token(token)
                    .username(user.getUsername())
                    .phoneNumber(user.getPhoneNumber())
                    .fullName(user.getFullName())
                    .success(true)
                    .message("Phone number verified successfully")
                    .build();
            
            return ResponseEntity.ok(response);
        } catch (FirebaseAuthException e) {
            log.error("Error authenticating user after OTP verification", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AuthResponse.builder()
                            .success(false)
                            .message("Authentication failed: " + e.getMessage())
                            .build());
        }
    }
    
    /**
     * This is a legacy mock endpoint for testing client-side verification
     * It's kept for backward compatibility but should be replaced with the server-side verification
     */
    @PostMapping("/client/verify")
    public ResponseEntity<FirebaseOtpResponse> verifyClientOtp(@Valid @RequestBody FirebaseOtpVerifyRequest request) {
        String phoneNumber = request.getPhoneNumber();
        String verificationCode = request.getVerificationCode();
        String verificationId = request.getVerificationId();
        
        log.info("Received client verification request for phone: {}, code: {}, verificationId: {}", 
                phoneNumber, verificationCode, verificationId);
        
        // This is a mock verification - in reality, this happens on the client side
        // We're just checking if the phone number exists in Firebase
        boolean exists = firebaseOtpService.verifyPhoneNumberExists(phoneNumber);
        
        if (exists) {
            // Get Firebase user
            UserRecord userRecord = firebaseOtpService.getUserByPhoneNumber(phoneNumber);
            
            if (userRecord != null) {
                // Create or update user in our database
                firebaseOtpService.createOrUpdateUserFromPhone(phoneNumber, userRecord.getUid());
            }
        }
        
        // Always return success for testing purposes
        // In production, the client would handle the actual verification
        FirebaseOtpResponse response = FirebaseOtpResponse.builder()
                .success(true)
                .message("OTP verified successfully")
                .phoneNumber(phoneNumber)
                .verificationId(verificationId)
                .build();
        
        return ResponseEntity.ok(response);
    }
}
