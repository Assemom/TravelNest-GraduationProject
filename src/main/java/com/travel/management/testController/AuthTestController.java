package com.travel.management.testController;

import com.travel.management.model.User;
import com.travel.management.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthTestController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final UserDetailsService userDetailsService;

    public AuthTestController(UserService userService,
                              PasswordEncoder passwordEncoder,
                              @Qualifier("userDetailsServiceImpl") UserDetailsService userDetailsService) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.userDetailsService = userDetailsService;
    }

    @PostMapping("/debug-user")
    public ResponseEntity<?> debugUser(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String password = request.get("password");

            // Get user from database
            User user = userService.getUserByEmail(email);

            // Test password
            boolean passwordMatches = passwordEncoder.matches(password, user.getPassword());

            // Get UserDetails
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            Map<String, Object> response = new HashMap<>();
            response.put("userFound", true);
            response.put("email", user.getEmail());
            response.put("passwordHash", user.getPassword());
            response.put("passwordMatches", passwordMatches);
            response.put("enabled", user.isEnabled());
            response.put("roles", user.getRoles());
            response.put("userDetailsEnabled", userDetails.isEnabled());
            response.put("userDetailsAccountNonLocked", userDetails.isAccountNonLocked());
            response.put("userDetailsAuthorities", userDetails.getAuthorities());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("type", e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    @PostMapping("/reset-password-test")
    public ResponseEntity<?> resetPasswordTest(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String newPassword = request.get("password");

            // Get user and original hash
            User user = userService.getUserByEmail(email);
            String originalHash = user.getPassword();

            // Update password
            user = userService.updatePassword(email, newPassword);
            String finalHash = user.getPassword();

            // Verify the password
            boolean matches = passwordEncoder.matches(newPassword, finalHash);

            Map<String, Object> response = new HashMap<>();
            response.put("email", email);
            response.put("originalHash", originalHash);
            response.put("finalHash", finalHash);
            response.put("passwordMatches", matches);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error in reset-password-test", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "type", e.getClass().getSimpleName()
            ));
        }
    }
}
