package com.travel.management.dto;

import com.travel.management.validation.PasswordConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RegistrationObjectDto {
    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;

    @PasswordConstraint
    private String password;

    @Pattern(regexp = "^\\+(?:[0-9] ?){6,14}[0-9]$",
            message = "Invalid phone number format. Use international format: +XXXXXXXXXXX")
    private String phoneNumber;

    private String country;
}
