package com.thehoodjunction.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "otps")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Otp {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String phoneNumber;
    
    @Column(nullable = false)
    private String otpValue;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime expiresAt;
    
    private LocalDateTime verifiedAt;
    
    private boolean used;
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
