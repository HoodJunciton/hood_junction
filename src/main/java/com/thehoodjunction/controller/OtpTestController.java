package com.thehoodjunction.controller;

import com.thehoodjunction.dto.OtpRequest;
import com.thehoodjunction.dto.OtpResponse;
import com.thehoodjunction.dto.OtpVerificationRequest;
import com.thehoodjunction.model.Otp;
import com.thehoodjunction.repository.jpa.OtpRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Test controller for OTP functionality.
 * This controller is only active in the "dev" profile and should not be used in production.
 */
@RestController
@RequestMapping("/test/otp")
@RequiredArgsConstructor
@Profile("dev")
@Slf4j
public class OtpTestController {

    private final OtpRepository otpRepository;
    private final Random random = new Random();

    /**
     * Test endpoint to simulate sending an OTP without actually calling MSG91.
     */
    @PostMapping("/mock-send")
    public ResponseEntity<OtpResponse> mockSendOtp(@Valid @RequestBody OtpRequest request) {
        log.info("Mock sending OTP to: {}", request.getPhoneNumber());
        
        // Generate a random 6-digit OTP
        String otpValue = String.format("%06d", random.nextInt(1000000));
        
        // Save OTP to database
        Otp otp = Otp.builder()
                .phoneNumber(request.getPhoneNumber())
                .otpValue(otpValue)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .build();
        
        otpRepository.save(otp);
        
        long expiresInSeconds = Duration.between(LocalDateTime.now(), otp.getExpiresAt()).getSeconds();
        
        OtpResponse response = OtpResponse.builder()
                .message("OTP sent successfully (MOCK)")
                .success(true)
                .phoneNumber(request.getPhoneNumber())
                .expiresInSeconds(expiresInSeconds)
                .build();
        
        // For testing purposes, log the OTP
        log.info("Generated OTP: {} for phone: {}", otpValue, request.getPhoneNumber());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Test endpoint to verify an OTP.
     */
    @PostMapping("/verify")
    public ResponseEntity<OtpResponse> verifyOtp(@Valid @RequestBody OtpVerificationRequest request) {
        log.info("Verifying OTP: {} for phone: {}", request.getOtp(), request.getPhoneNumber());
        
        Optional<Otp> otpOptional = otpRepository.findByPhoneNumberAndOtpValueAndUsedFalse(
                request.getPhoneNumber(), request.getOtp());
        
        boolean isValid = false;
        
        if (otpOptional.isPresent()) {
            Otp otp = otpOptional.get();
            
            if (!otp.isExpired()) {
                // Mark OTP as used
                otp.setVerifiedAt(LocalDateTime.now());
                otp.setUsed(true);
                otpRepository.save(otp);
                
                isValid = true;
            }
        }
        
        OtpResponse response = OtpResponse.builder()
                .message(isValid ? "OTP verified successfully" : "Invalid OTP")
                .success(isValid)
                .phoneNumber(request.getPhoneNumber())
                .expiresInSeconds(0)
                .build();
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get all OTPs for a phone number (for testing purposes only).
     */
    @GetMapping("/list/{phoneNumber}")
    public ResponseEntity<List<Otp>> getOtpsForPhone(@PathVariable String phoneNumber) {
        List<Otp> otps = otpRepository.findAll().stream()
                .filter(otp -> otp.getPhoneNumber().equals(phoneNumber))
                .toList();
        
        return ResponseEntity.ok(otps);
    }
}
