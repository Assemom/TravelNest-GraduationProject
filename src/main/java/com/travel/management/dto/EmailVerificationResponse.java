package com.travel.management.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EmailVerificationResponse {
    private String message;
    private boolean verified;
    private LocalDateTime verifiedAt;
}
