package com.thehoodjunction.controller;

import com.thehoodjunction.dto.OtpRequest;
import com.thehoodjunction.dto.OtpResponse;
import com.thehoodjunction.dto.OtpVerificationRequest;
import com.thehoodjunction.model.Otp;
import com.thehoodjunction.service.Msg91Service;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/otp")
@RequiredArgsConstructor
public class OtpController {

    private final Msg91Service msg91Service;

    @PostMapping("/send")
    public ResponseEntity<OtpResponse> sendOtp(@Valid @RequestBody OtpRequest request) {
        Otp otp = msg91Service.generateAndSendOtp(request.getPhoneNumber());
        
        long expiresInSeconds = Duration.between(LocalDateTime.now(), otp.getExpiresAt()).getSeconds();
        
        OtpResponse response = OtpResponse.builder()
                .message("OTP sent successfully")
                .success(true)
                .phoneNumber(request.getPhoneNumber())
                .expiresInSeconds(expiresInSeconds)
                .build();
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/verify")
    public ResponseEntity<OtpResponse> verifyOtp(@Valid @RequestBody OtpVerificationRequest request) {
        boolean isValid = msg91Service.verifyOtp(request.getPhoneNumber(), request.getOtp());
        
        OtpResponse response = OtpResponse.builder()
                .message(isValid ? "OTP verified successfully" : "Invalid OTP")
                .success(isValid)
                .phoneNumber(request.getPhoneNumber())
                .expiresInSeconds(0)
                .build();
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/resend")
    public ResponseEntity<OtpResponse> resendOtp(@Valid @RequestBody OtpRequest request) {
        Otp otp = msg91Service.resendOtp(request.getPhoneNumber());
        
        long expiresInSeconds = Duration.between(LocalDateTime.now(), otp.getExpiresAt()).getSeconds();
        
        OtpResponse response = OtpResponse.builder()
                .message("OTP resent successfully")
                .success(true)
                .phoneNumber(request.getPhoneNumber())
                .expiresInSeconds(expiresInSeconds)
                .build();
        
        return ResponseEntity.ok(response);
    }
}
