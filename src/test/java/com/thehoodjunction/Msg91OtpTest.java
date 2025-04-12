package com.thehoodjunction;

import org.apache.commons.lang3.RandomStringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * A standalone test class for MSG91 OTP verification.
 * This class demonstrates how to send and verify OTPs using MSG91 API.
 */
public class Msg91OtpTest {

    // MSG91 Configuration
    private static final String AUTH_KEY = "445979AHHWnwUg4JXz67f2c9f4P1";
    private static final String SENDER_ID = "HoodJunciton";
    private static final String TEMPLATE_ID = "67f2cac9d6fc05563f58e9b4";
    private static final String ROUTE = "1"; // Transactional route
    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 10;
    
    // In-memory storage for OTPs (in a real application, this would be a database)
    private static final Map<String, OtpInfo> otpStorage = new HashMap<>();

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("===== MSG91 OTP Verification Test =====");
        
        while (true) {
            System.out.println("\nSelect an option:");
            System.out.println("1. Send OTP");
            System.out.println("2. Verify OTP");
            System.out.println("3. List all OTPs");
            System.out.println("4. Exit");
            System.out.print("Enter your choice (1-4): ");
            
            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline
            
            switch (choice) {
                case 1:
                    sendOtp(scanner);
                    break;
                case 2:
                    verifyOtp(scanner);
                    break;
                case 3:
                    listOtps();
                    break;
                case 4:
                    System.out.println("Exiting...");
                    return;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }
    
    private static void sendOtp(Scanner scanner) {
        System.out.print("Enter phone number (with country code, e.g., +919876543210): ");
        String phoneNumber = scanner.nextLine();
        
        // Generate OTP
        String otpValue = generateOtp();
        
        // Save OTP
        LocalDateTime now = LocalDateTime.now();
        OtpInfo otpInfo = new OtpInfo(
                phoneNumber,
                otpValue,
                now,
                now.plusMinutes(OTP_EXPIRY_MINUTES),
                null,
                false
        );
        otpStorage.put(phoneNumber, otpInfo);
        
        // In a real application, we would call MSG91 API here
        boolean sent = mockSendOtpViaMSG91(phoneNumber, otpValue);
        
        if (sent) {
            System.out.println("OTP sent successfully to " + phoneNumber);
            System.out.println("OTP: " + otpValue + " (This would be sent to the user's phone in a real application)");
            System.out.println("Expires in " + OTP_EXPIRY_MINUTES + " minutes");
        } else {
            System.out.println("Failed to send OTP");
        }
    }
    
    private static void verifyOtp(Scanner scanner) {
        System.out.print("Enter phone number: ");
        String phoneNumber = scanner.nextLine();
        
        System.out.print("Enter OTP: ");
        String otpValue = scanner.nextLine();
        
        boolean isValid = false;
        
        if (otpStorage.containsKey(phoneNumber)) {
            OtpInfo otpInfo = otpStorage.get(phoneNumber);
            
            if (otpInfo.otpValue.equals(otpValue) && !otpInfo.used) {
                if (LocalDateTime.now().isBefore(otpInfo.expiresAt)) {
                    // Mark OTP as used
                    otpInfo.verifiedAt = LocalDateTime.now();
                    otpInfo.used = true;
                    otpStorage.put(phoneNumber, otpInfo);
                    
                    isValid = true;
                } else {
                    System.out.println("OTP has expired");
                }
            } else {
                System.out.println("Invalid OTP");
            }
        } else {
            System.out.println("No OTP found for this phone number");
        }
        
        if (isValid) {
            System.out.println("OTP verified successfully!");
        } else {
            System.out.println("OTP verification failed");
        }
    }
    
    private static void listOtps() {
        if (otpStorage.isEmpty()) {
            System.out.println("No OTPs have been generated yet");
            return;
        }
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        System.out.println("\n===== OTP List =====");
        System.out.printf("%-15s %-10s %-20s %-20s %-20s %-5s\n", 
                "Phone", "OTP", "Created At", "Expires At", "Verified At", "Used");
        
        for (Map.Entry<String, OtpInfo> entry : otpStorage.entrySet()) {
            OtpInfo otp = entry.getValue();
            System.out.printf("%-15s %-10s %-20s %-20s %-20s %-5s\n",
                    otp.phoneNumber,
                    otp.otpValue,
                    otp.createdAt.format(formatter),
                    otp.expiresAt.format(formatter),
                    otp.verifiedAt != null ? otp.verifiedAt.format(formatter) : "N/A",
                    otp.used);
        }
    }
    
    /**
     * Generate a random OTP
     */
    private static String generateOtp() {
        return RandomStringUtils.randomNumeric(OTP_LENGTH);
    }
    
    /**
     * Mock sending OTP via MSG91 API
     * In a real application, this would make an actual API call to MSG91
     */
    private static boolean mockSendOtpViaMSG91(String phoneNumber, String otpValue) {
        try {
            // In a real application, we would build the URL and make the request to MSG91
            System.out.println("Simulating MSG91 API call with the following parameters:");
            System.out.println("- URL: https://api.msg91.com/api/v5/otp");
            System.out.println("- template_id: " + TEMPLATE_ID);
            System.out.println("- mobile: " + phoneNumber);
            System.out.println("- authkey: " + AUTH_KEY);
            System.out.println("- otp: " + otpValue);
            System.out.println("- sender: " + SENDER_ID);
            System.out.println("- route: " + ROUTE);
            
            // In a real implementation, we would use OkHttpClient to make the request:
            // HttpUrl url = HttpUrl.parse("https://api.msg91.com/api/v5/otp")
            //     .newBuilder()
            //     .addQueryParameter("template_id", TEMPLATE_ID)
            //     .addQueryParameter("mobile", phoneNumber)
            //     .addQueryParameter("authkey", AUTH_KEY)
            //     .addQueryParameter("otp", otpValue)
            //     .addQueryParameter("sender", SENDER_ID)
            //     .addQueryParameter("route", ROUTE)
            //     .build();
            // 
            // Request request = new Request.Builder().url(url).build();
            // Response response = httpClient.newCall(request).execute();
            
            // Simulate successful response
            return true;
        } catch (Exception e) {
            System.err.println("Error sending OTP: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Class to store OTP information
     */
    private static class OtpInfo {
        String phoneNumber;
        String otpValue;
        LocalDateTime createdAt;
        LocalDateTime expiresAt;
        LocalDateTime verifiedAt;
        boolean used;
        
        public OtpInfo(String phoneNumber, String otpValue, LocalDateTime createdAt, 
                       LocalDateTime expiresAt, LocalDateTime verifiedAt, boolean used) {
            this.phoneNumber = phoneNumber;
            this.otpValue = otpValue;
            this.createdAt = createdAt;
            this.expiresAt = expiresAt;
            this.verifiedAt = verifiedAt;
            this.used = used;
        }
    }
}
