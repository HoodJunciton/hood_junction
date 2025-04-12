package com.thehoodjunction.repository.jpa;

import com.thehoodjunction.model.Otp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<Otp, Long> {
    Optional<Otp> findTopByPhoneNumberOrderByCreatedAtDesc(String phoneNumber);
    Optional<Otp> findByPhoneNumberAndOtpValueAndUsedFalse(String phoneNumber, String otpValue);
}
