package com.travel.management.dto;

import com.travel.management.validation.PasswordConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PasswordResetVerificationRequest {
    private String email;
    private String code;

    @PasswordConstraint
    private String newPassword;
}
