package com.travel.management.controller;

import com.travel.management.dto.SelfDeletionRequestDto;
import com.travel.management.exception.*;
import com.travel.management.model.User;
import com.travel.management.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "User Management", description = "APIs for managing users in the travel system")
@RestController
@RequestMapping("/api")
@ControllerAdvice
@Slf4j
public class UserController {
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @Operation(summary = "Get all users", description = "Retrieves a list of all users. Accessible to Admin and Managers.")
    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<List<User>> getAllUsers() {
        try {
            List<User> users = userService.getAllUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error("Error fetching users: ", e);
            throw new RuntimeException("Failed to fetch users");
        }
    }


    @Operation(summary = "Get user by ID", description = "Retrieves user details based on the provided ID. TOURIST can only view their own profile.")
    @GetMapping("/users/{userId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER') or @userSecurity.isCurrentUser(#userId)")
    public ResponseEntity<User> getUserById(@PathVariable Long userId) {
        try {
            User user = userService.getUserById(userId);
            if (user == null) {
                throw new UserDoesNotExistException();
            }
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            log.error("Error fetching user: ", e);
            throw new UserDoesNotExistException();
        }
    }


    @Operation(summary = "Add a new user", description = "Creates a new user. Accessible to Admin and Managers.")
    @PostMapping("/users")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<?> addUser(@RequestBody @Valid User user) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String creatorRole = authentication
                    .getAuthorities()
                    .stream()
                    .map(Object::toString)
                    .filter(role -> role.startsWith("ROLE_"))
                    .findFirst()
                    .orElse("ROLE_TOURIST"); // Default fallback, though shouldn't happen
            User createdUser = userService.addUser(user, creatorRole);
            return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
        } catch (EmailAlreadyExistException e) {
            return new ResponseEntity<>(Map.of("message", "Email already exists"), HttpStatus.CONFLICT);
        } catch (PhoneNumberAlreadyExistsException e) {
            return new ResponseEntity<>(Map.of("message", "Phone number already exists"), HttpStatus.CONFLICT);
        } catch (AccessDeniedException e) {
            return new ResponseEntity<>(Map.of("message", "You are not authorized to assign this role"), HttpStatus.FORBIDDEN);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(Map.of("message", e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Error adding user: ", e);
            return new ResponseEntity<>(Map.of("message", "Failed to add user"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @Operation(summary = "Update user details", description = "Updates a user's details. ADMIN can update any user, others can only update their own profile.")
    @PutMapping("/users/{userId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER') or @userSecurity.isCurrentUser(#userId)")
    public ResponseEntity<?> updateUser(@PathVariable Long userId, @RequestBody User user) {
        try {
            user.setId(userId);
            User updatedUser = userService.updateUser(user);
            if (updatedUser != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Updated successfully");
                response.put("user", updatedUser);
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                Map<String, String> response = new HashMap<>();
                response.put("message", "User not found");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }
        } catch (EmailAlreadyExistException e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Failed to update: Email already exists");
            return new ResponseEntity<>(response, HttpStatus.CONFLICT);
        } catch (PhoneNumberAlreadyExistsException e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Failed to update: Phone number already exists");
            return new ResponseEntity<>(response, HttpStatus.CONFLICT);
        } catch (InvalidEmailFormatException e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Failed to update: Invalid email format");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Failed to update: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(summary = "Update own password", description = "Allows a user to update their own password after confirming current password.")
    @PostMapping("/users/self/password")
    public ResponseEntity<Map<String, String>> updateOwnPassword(
            @RequestBody Map<String, String> passwordRequest) {
        Map<String, String> response = new HashMap<>();
        try {
            String currentPassword = passwordRequest.get("currentPassword");
            String newPassword = passwordRequest.get("newPassword");

            if (currentPassword == null || newPassword == null) {
                response.put("message", "Current and new passwords are required");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUserEmail = authentication.getName(); // Assuming email from JWT
            User currentUser = userService.getUserByEmail(currentUserEmail);

            // Verify current password
            if (!passwordEncoder.matches(currentPassword, currentUser.getPassword())) {
                response.put("message", "Incorrect current password");
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }

            // Update password with reuse check
            userService.setPassword(currentUserEmail, newPassword);
            response.put("message", "Password updated successfully");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (UserDoesNotExistException e) {
            response.put("message", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        } catch (PasswordReusedException e) {
            response.put("message", "Cannot reuse the previous password");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            response.put("message", "Failed to update password: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(summary = "Delete user by ID", description = "Deletes a user profile. ADMIN can delete any user, MANAGER can delete TOURIST only.")
    @DeleteMapping("/users/{userId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN') or (hasRole('ROLE_MANAGER') and @userSecurity.isTouristUser(#userId))")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long userId) {
        Map<String, String> response = new HashMap<>();
        try {
            userService.deleteById(userId);
            response.put("message", "User deleted successfully");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (UserDoesNotExistException e) {
            response.put("message", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        } catch (AccessDeniedException e) {
            response.put("message", "Access denied: You don't have permission to delete this user");
            return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
        } catch (LastAdminException e) {
            response.put("message", "Cannot delete the last admin user");
            return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            response.put("message", "Failed to delete user: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @Operation(summary = "Delete own account", description = "Allows any user to delete their own account after password confirmation.")
    @DeleteMapping("/users/self")
    public ResponseEntity<Map<String, String>> deleteSelf(@RequestBody @Valid SelfDeletionRequestDto request) {
        Map<String, String> response = new HashMap<>();
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUserEmail = authentication.getName(); // Assuming email from JWT
            User currentUser = userService.getUserByEmail(currentUserEmail);
            if (currentUser == null) {
                response.put("message", "User not found");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }
            userService.deleteSelf(currentUser.getId(), request.getPassword());
            response.put("message", "Account deleted successfully");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (UserDoesNotExistException e) {
            response.put("message", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        } catch (InvalidCredentialsException e) {
            response.put("message", "Incorrect password provided for account deletion");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        } catch (LastAdminException e) {
            response.put("message", "Cannot delete the last admin user");
            return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            response.put("message", "Failed to delete account: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @Operation(summary = "Update user role", description = "Changes a user's role. Accessible to Admin only.")
    @PutMapping("/users/{userId}/role")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> updateUserRole(@PathVariable Long userId, @RequestBody Map<String, String> roleRequest) {
        try {
            String roleType = roleRequest.get("roleType");
            if (roleType == null || roleType.isEmpty()) {
                return new ResponseEntity<>(Map.of("message", "Role type is required"), HttpStatus.BAD_REQUEST);
            }
            User updatedUser = userService.updateUserRole(userId, roleType);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Role updated successfully");
            response.put("user", updatedUser);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (UserDoesNotExistException e) {
            return new ResponseEntity<>(Map.of("message", "User not found"), HttpStatus.NOT_FOUND);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(Map.of("message", e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Error updating user role: ", e);
            return new ResponseEntity<>(Map.of("message", "Failed to update role"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
