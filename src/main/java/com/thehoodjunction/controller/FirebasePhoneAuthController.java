package com.thehoodjunction.controller;

import com.google.firebase.auth.FirebaseAuthException;
import com.thehoodjunction.dto.AuthResponse;
import com.thehoodjunction.dto.FirebaseAuthRequest;
import com.thehoodjunction.model.User;
import com.thehoodjunction.service.FirebaseAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for handling Firebase Phone Authentication
 * This replaces the previous OTP functionality using MSG91
 */
@RestController
@RequestMapping("/auth/phone")
@RequiredArgsConstructor
@Slf4j
public class FirebasePhoneAuthController {

    private final FirebaseAuthService firebaseAuthService;

    /**
     * Authenticate a user with Firebase phone authentication
     * The verification process happens on the client side using Firebase SDK
     * The client then sends the ID token to this endpoint
     */
    @PostMapping("/verify")
    public ResponseEntity<AuthResponse> verifyPhoneAuth(@Valid @RequestBody FirebaseAuthRequest request) {
        try {
            User user = firebaseAuthService.authenticateWithFirebase(request.getIdToken());
            String jwt = firebaseAuthService.generateToken(user);
            
            AuthResponse response = AuthResponse.builder()
                    .token(jwt)
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .build();
            
            return ResponseEntity.ok(response);
        } catch (FirebaseAuthException e) {
            log.error("Firebase phone authentication error", e);
            throw new RuntimeException("Phone authentication failed: " + e.getMessage());
        }
    }
}
