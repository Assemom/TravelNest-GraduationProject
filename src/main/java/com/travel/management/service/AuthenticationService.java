package com.travel.management.service;

import com.travel.management.Utils.JwtUtils;
import com.travel.management.dto.*;
import com.travel.management.exception.*;
import com.travel.management.model.User;
import com.travel.management.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.auth.InvalidCredentialsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AuthenticationService {
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Autowired
    public AuthenticationService(AuthenticationManager authenticationManager,
                                 UserService userService,
                                 UserRepository userRepository, JwtUtils jwtUtils,
                                 PasswordEncoder passwordEncoder
                                 , EmailService emailService) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.userRepository = userRepository;
        this.jwtUtils = jwtUtils;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public LoginResponse authenticate(LoginRequest loginRequest) throws AccountLockedException, InvalidCredentialsException {
        try {
            log.info("Attempting authentication for email: {}", loginRequest.getEmail());

            User user = userService.getUserByEmail(loginRequest.getEmail());

            // Check if account is locked due to failed login attempts
            if (!user.isAccountNonLocked()) {
                if (user.getLockoutTime() != null &&
                        user.getLockoutTime().isAfter(LocalDateTime.now())) {
                    throw new AccountLockedException("Account is temporarily locked. Please try again later.");
                }
                // Reset lockout if time has expired
                user.resetFailedAttempts();
                userService.updateUser(user);
            }

            // Test password match before authentication
            boolean passwordMatches = passwordEncoder.matches(loginRequest.getPassword(), user.getPassword());
            log.info("Password match result: {}", passwordMatches);

            if (!passwordMatches) {
                user.incrementFailedAttempts();
                userService.updateUser(user);
                throw new InvalidCredentialsException("Invalid email or password");
            }

            // Reset failed attempts on successful login
            if (user.getFailedLoginAttempts() > 0) {
                user.resetFailedAttempts();
                userService.updateUser(user);
            }

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

            log.info("Authentication successful");
            SecurityContextHolder.getContext().setAuthentication(authentication);

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String jwt = jwtUtils.generateJwtToken(authentication);
            String refreshToken = jwtUtils.generateRefreshToken();

            user.setRefreshToken(refreshToken);
            user.setRefreshTokenExpiry(LocalDateTime.now().plusDays(7));
            userService.updateUser(user);

            Set<String> authorities = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());

            log.info("User authorities: {}", authorities);

            return new LoginResponse(
                    jwt,
                    refreshToken,
                    "Bearer",
                    3600L,
                    user.getEmail(),
                    authorities
            );
        } catch (Exception e) {
            log.error("Authentication failed: ", e);
            throw e;
        }
    }

    public ApiResponse sendVerificationCode(String email) {
        User user = userService.getUserByEmail(email);
        String verificationCode = generateVerificationCode();

        user.setVerification(Long.parseLong(verificationCode));
        user.setVerificationCodeExpiry(LocalDateTime.now().plusHours(1));
        userService.updateUser(user);

        emailService.sendVerificationCode(email, verificationCode);

        return new ApiResponse(
                true,
                "Verification code sent successfully. Valid for 1 hour.",
                LocalDateTime.now(),
                Map.of("expiresAt", user.getVerificationCodeExpiry())
        );
    }


    public ApiResponse verifyEmail(EmailVerificationRequest request) {
        User user = userService.getUserByEmail(request.getEmail());

        if (user.getVerificationCodeExpiry().isBefore(LocalDateTime.now())) {
            throw new TokenExpiredException("Verification code has expired");
        }

        if (user.getVerification() != Long.parseLong(request.getCode())) {
            throw new IncorrectVerificationCodeException();
        }

        user.setEnabled(true);
        user.setVerification(0);
        userService.updateUser(user);

        return new ApiResponse(true, "Email verified successfully",
                LocalDateTime.now(), null);
    }

    public ApiResponse initiatePasswordReset(PasswordResetRequest request) {
        User user = userService.getUserByEmail(request.getEmail());
        String resetCode = generateVerificationCode();

        user.setResetPasswordToken(resetCode);
        user.setResetPasswordTokenExpiry(LocalDateTime.now().plusMinutes(15));
        user.setResetPasswordAttempts(0);
        userService.updateUser(user);

        emailService.sendPasswordResetCode(request.getEmail(), resetCode);

        return new ApiResponse(
                true,
                "Password reset code sent successfully. Valid for 15 minutes.",
                LocalDateTime.now(),
                Map.of("expiresAt", user.getResetPasswordTokenExpiry())
        );
    }

    public ApiResponse resetPassword(PasswordResetVerificationRequest request) {
        User user = userService.getUserByEmail(request.getEmail());

        // Check for lockout due to too many reset attempts
        if (user.getResetPasswordAttempts() >= 5) {
            user.setLockoutTime(LocalDateTime.now().plusMinutes(15));
            userService.updateUser(user);
            throw new AccountLockedException("Too many reset attempts. Please try again after 15 minutes.");
        }

        // Check if account is locked
        if (!user.isAccountNonLocked()) {
            if (user.getLockoutTime() != null &&
                    user.getLockoutTime().isAfter(LocalDateTime.now())) {
                throw new AccountLockedException("Account is temporarily locked. Please try again later.");
            }
        }

        if (user.getResetPasswordTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new TokenExpiredException("Reset code has expired");
        }

        if (!user.getResetPasswordToken().equals(request.getCode())) {
            user.setResetPasswordAttempts(user.getResetPasswordAttempts() + 1);
            userService.updateUser(user);
            throw new IncorrectVerificationCodeException();
        }

        // Use setPassword to update password with history check
        userService.setPassword(user.getEmail(), request.getNewPassword());

        // Reset security fields
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiry(null);
        user.setResetPasswordAttempts(0);
        user.setLockoutTime(null);
        userService.updateUser(user);

        return new ApiResponse(true, "Password reset successfully", LocalDateTime.now(), null);
    }

    private String generateVerificationCode() {
        return String.format("%06d", new Random().nextInt(999999));
    }
}
