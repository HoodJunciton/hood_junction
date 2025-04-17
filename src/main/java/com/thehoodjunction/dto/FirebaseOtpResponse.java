package com.thehoodjunction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FirebaseOtpResponse {
    
    private boolean success;
    private String message;
    private String phoneNumber;
    private String verificationId;
}
