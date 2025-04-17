package com.thehoodjunction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerOtpVerifyRequest {
    
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+[0-9]{10,15}$", message = "Phone number must be in international format (e.g., +1234567890)")
    private String phoneNumber;
    
    @NotBlank(message = "OTP is required")
    private String otp;
    
    @NotBlank(message = "Verification ID is required")
    private String verificationId;
}
