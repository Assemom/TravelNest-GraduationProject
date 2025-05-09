package com.travel.management.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PasswordResetResponse {
    private String message;
    private LocalDateTime expiresAt;
}
