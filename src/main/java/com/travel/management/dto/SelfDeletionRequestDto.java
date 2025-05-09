package com.travel.management.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SelfDeletionRequestDto {
    @NotBlank(message = "Password is required for confirmation")
    private String password;
}
