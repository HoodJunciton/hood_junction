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

@RestController
@RequestMapping("/auth/firebase")
@RequiredArgsConstructor
@Slf4j
public class FirebaseAuthController {

    private final FirebaseAuthService firebaseAuthService;
    @PostMapping("/authenticate")
    public ResponseEntity<AuthResponse> authenticateWithFirebase(@Valid @RequestBody FirebaseAuthRequest request) {
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
            log.error("Firebase authentication error", e);
            throw new RuntimeException("Authentication failed: " + e.getMessage());
        }
    }
}
