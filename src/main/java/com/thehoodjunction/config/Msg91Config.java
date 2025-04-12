package com.thehoodjunction.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "msg91")
@Data
public class Msg91Config {
    private String authKey;
    private String senderId;
    private String route;
    private String otpTemplateId;
    private int otpLength;
    private int otpExpiryMinutes;
}
