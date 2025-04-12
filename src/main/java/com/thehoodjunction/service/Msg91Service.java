package com.thehoodjunction.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thehoodjunction.config.Msg91Config;
import com.thehoodjunction.model.Otp;
import com.thehoodjunction.repository.jpa.OtpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class Msg91Service {

    private final Msg91Config msg91Config;
    private final OtpRepository otpRepository;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient = new OkHttpClient();

    /**
     * Generate and send OTP to the provided phone number
     * 
     * @param phoneNumber Phone number to send OTP to (with country code)
     * @return The generated OTP entity
     */
    public Otp generateAndSendOtp(String phoneNumber) {
        // Generate OTP
        String otpValue = generateOtp();
        
        // Save OTP to database
        Otp otp = Otp.builder()
                .phoneNumber(phoneNumber)
                .otpValue(otpValue)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(msg91Config.getOtpExpiryMinutes()))
                .used(false)
                .build();
        
        otpRepository.save(otp);
        
        // Send OTP via MSG91
        boolean sent = sendOtpViaMSG91(phoneNumber, otpValue);
        
        if (!sent) {
            log.error("Failed to send OTP to {}", phoneNumber);
            throw new RuntimeException("Failed to send OTP");
        }
        
        return otp;
    }
    
    /**
     * Verify the OTP provided by the user
     * 
     * @param phoneNumber Phone number
     * @param otpValue OTP value to verify
     * @return true if OTP is valid, false otherwise
     */
    public boolean verifyOtp(String phoneNumber, String otpValue) {
        Optional<Otp> otpOptional = otpRepository.findByPhoneNumberAndOtpValueAndUsedFalse(phoneNumber, otpValue);
        
        if (otpOptional.isPresent()) {
            Otp otp = otpOptional.get();
            
            if (otp.isExpired()) {
                return false;
            }
            
            // Mark OTP as used
            otp.setVerifiedAt(LocalDateTime.now());
            otp.setUsed(true);
            otpRepository.save(otp);
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Generate a random OTP
     * 
     * @return Generated OTP
     */
    private String generateOtp() {
        return RandomStringUtils.randomNumeric(msg91Config.getOtpLength());
    }
    
    /**
     * Send OTP via MSG91 API
     * 
     * @param phoneNumber Phone number to send OTP to
     * @param otpValue OTP value to send
     * @return true if OTP was sent successfully, false otherwise
     */
    private boolean sendOtpViaMSG91(String phoneNumber, String otpValue) {
        try {
            // For sending OTP via MSG91
            HttpUrl url = HttpUrl.parse("https://api.msg91.com/api/v5/otp")
                    .newBuilder()
                    .addQueryParameter("template_id", msg91Config.getOtpTemplateId())
                    .addQueryParameter("mobile", phoneNumber)
                    .addQueryParameter("authkey", msg91Config.getAuthKey())
                    .addQueryParameter("otp", otpValue)
                    .build();
            
            Request request = new Request.Builder()
                    .url(url)
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Error sending OTP: {}", response);
                    return false;
                }
                
                ResponseBody responseBody = response.body();
                if (responseBody != null) {
                    String responseString = responseBody.string();
                    JsonNode jsonNode = objectMapper.readTree(responseString);
                    String type = jsonNode.get("type").asText();
                    return "success".equalsIgnoreCase(type);
                }
            }
            
            return false;
        } catch (IOException e) {
            log.error("Error sending OTP", e);
            return false;
        }
    }
    
    /**
     * Resend OTP to the provided phone number
     * 
     * @param phoneNumber Phone number to resend OTP to
     * @return The new OTP entity
     */
    public Otp resendOtp(String phoneNumber) {
        // Invalidate any existing OTPs
        Optional<Otp> existingOtp = otpRepository.findTopByPhoneNumberOrderByCreatedAtDesc(phoneNumber);
        existingOtp.ifPresent(otp -> {
            otp.setUsed(true);
            otpRepository.save(otp);
        });
        
        // Generate and send new OTP
        return generateAndSendOtp(phoneNumber);
    }
}
