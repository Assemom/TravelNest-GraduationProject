package com.travel.management.dto;

import lombok.Data;

@Data
public class EmailVerificationRequest {
    private String email;
    private String code;
}
