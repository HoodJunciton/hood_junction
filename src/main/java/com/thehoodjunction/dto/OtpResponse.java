package com.thehoodjunction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpResponse {
    private String message;
    private boolean success;
    private String phoneNumber;
    private long expiresInSeconds;
}
