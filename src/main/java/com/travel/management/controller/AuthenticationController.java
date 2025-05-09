package com.travel.management.controller;

import com.travel.management.Utils.JwtUtils;
import com.travel.management.dto.*;
import com.travel.management.exception.*;
import com.travel.management.dto.RegistrationObjectDto;
import com.travel.management.model.User;
import com.travel.management.service.AuthenticationService;
import com.travel.management.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthenticationController {
    private final AuthenticationService authenticationService;
    private final UserService userService;
    private final JwtUtils jwtUtils;
    @Autowired
    public AuthenticationController(AuthenticationService authenticationService, UserService userService, JwtUtils jwtUtils) {
        this.authenticationService = authenticationService;
        this.userService = userService;
        this.jwtUtils = jwtUtils;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            LoginResponse response = authenticationService.authenticate(loginRequest);
            return ResponseEntity.ok(response);
        } catch (AccountLockedException e) {
            return ResponseEntity.status(HttpStatus.LOCKED)
                    .body(new ApiResponse(false, e.getMessage(), LocalDateTime.now(), null));
        } catch (InvalidCredentialsException | BadCredentialsException e) {
            log.error("Bad credentials: ", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(false, "Invalid email or password",
                            LocalDateTime.now(), null));
        } catch (Exception e) {
            log.error("Login failed: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Login failed: " + e.getMessage(),
                            LocalDateTime.now(), null));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody RegistrationObjectDto registrationObjectDto) {
        try {
            User user = userService.registerUser(registrationObjectDto);
            // Send verification email
            authenticationService.sendVerificationCode(user.getEmail());
            return ResponseEntity.ok(new ApiResponse(
                    true,
                    "Registration successful. Please check your email for verification code.",
                    LocalDateTime.now(),
                    null
            ));
        } catch (EmailAlreadyExistException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiResponse(false, "Email already exists",
                            LocalDateTime.now(), null));
        } catch (PhoneNumberAlreadyExistsException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiResponse(false, "Phone number already exists",
                            LocalDateTime.now(), null));
        } catch (Exception e) {
            log.error("Registration failed: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Registration failed: " +
                            e.getMessage(), LocalDateTime.now(), null));
        }
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse> verifyEmail(@Valid @RequestBody EmailVerificationRequest request) {
        try {
            ApiResponse response = authenticationService.verifyEmail(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Email verification failed: ", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, e.getMessage(), LocalDateTime.now(),
                            null));
        }
    }
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse> forgotPassword(@Valid @RequestBody PasswordResetRequest request) {
        try {
            ApiResponse response = authenticationService.initiatePasswordReset(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Forgot password failed: ", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, e.getMessage(), LocalDateTime.now(),
                            null));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse> resetPassword(@Valid @RequestBody PasswordResetVerificationRequest request) {
        try {
            ApiResponse response = authenticationService.resetPassword(request);
            return ResponseEntity.ok(response);
        } catch (AccountLockedException e) {
            return ResponseEntity.status(HttpStatus.LOCKED)
                    .body(new ApiResponse(false, e.getMessage(), LocalDateTime.now(), null));
        } catch (TokenExpiredException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "Reset code expired", LocalDateTime.now(), null));
        } catch (IncorrectVerificationCodeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "Invalid reset code", LocalDateTime.now(), null));
        } catch (PasswordReusedException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "Cannot reuse previous password", LocalDateTime.now(), null));
        } catch (Exception e) {
            log.error("Reset password failed: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, e.getMessage(), LocalDateTime.now(), null));
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<LoginResponse> refreshToken(@RequestBody Map<String, String> request) {
        try {
            String refreshToken = request.get("refreshToken");
            if (refreshToken == null) {
                throw new InvalidTokenException("Refresh token is required");
            }
            // Rest of your existing logic
            User user = userService.getUserByRefreshToken(refreshToken);
            if (user == null || user.getRefreshTokenExpiry().isBefore(LocalDateTime.now())) {
                throw new TokenExpiredException("Refresh token has expired");
            }
            String newAccessToken = jwtUtils.generateTokenFromUsername(user.getEmail());
            String newRefreshToken = jwtUtils.generateRefreshToken();
            user.setRefreshToken(newRefreshToken);
            user.setRefreshTokenExpiry(LocalDateTime.now().plusDays(7));
            userService.updateUser(user);
            LoginResponse response = new LoginResponse(
                    newAccessToken,
                    newRefreshToken,
                    "Bearer",
                    3600L,
                    user.getEmail(),
                    user.getRoles().stream()
                            .map(role -> role.getRoleType().name())
                            .collect(Collectors.toSet())
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Token refresh failed: ", e);
            throw e;
        }
    }
    @PostMapping("/resend-verification-code")
    public ResponseEntity<ApiResponse> resendVerificationCode(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            if (email == null) {
                throw new IllegalArgumentException("Email is required");
            }
            User user = userService.getUserByEmail(email);
            if (user.isEnabled()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Email is already verified", LocalDateTime.now(), null));
            }
            ApiResponse response = authenticationService.sendVerificationCode(email);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Resend verification code failed: ", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, e.getMessage(), LocalDateTime.now(), null));
        }
    }

    @PostMapping("/resend-password-reset-code")
    public ResponseEntity<ApiResponse> resendPasswordResetCode(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            if (email == null) {
                throw new IllegalArgumentException("Email is required");
            }
            User user = userService.getUserByEmail(email);
            if (user.getResetPasswordTokenExpiry() != null &&
                    user.getResetPasswordTokenExpiry().isAfter(LocalDateTime.now().minusMinutes(5))) {
                throw new TooManyRequestsException("Please wait 5 minutes before requesting another code");
            }
            ApiResponse response = authenticationService.initiatePasswordReset(new PasswordResetRequest(email));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Resend password reset code failed: ", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, e.getMessage(), LocalDateTime.now(), null));
        }
    }
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(@RequestHeader("Authorization") String token) {
        try {
            if (token != null && token.startsWith("Bearer ")) {
                String jwt = token.substring(7);
                String userEmail = jwtUtils.getUserNameFromJwtToken(jwt);
                User user = userService.getUserByEmail(userEmail);
                user.setRefreshToken(null);
                user.setRefreshTokenExpiry(null);
                userService.updateUser(user);
            }
            return ResponseEntity.ok(new ApiResponse(
                    true,
                    "Logged out successfully",
                    LocalDateTime.now(),
                    null
            ));
        } catch (Exception e) {
            log.error("Logout failed: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, e.getMessage(), LocalDateTime.now(), null));
        }
    }
}
