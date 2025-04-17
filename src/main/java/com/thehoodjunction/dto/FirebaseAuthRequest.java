package com.thehoodjunction.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FirebaseAuthRequest {
    
    @NotBlank(message = "Firebase ID token is required")
    private String idToken;
    
    private String authProvider; // "google", "phone", etc.
}
