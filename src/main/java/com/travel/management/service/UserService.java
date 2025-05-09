package com.travel.management.service;

import com.travel.management.exception.*;
import com.travel.management.dto.RegistrationObjectDto;
import com.travel.management.model.Role;
import com.travel.management.model.User;
import com.travel.management.repository.RoleRepository;
import com.travel.management.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }
    public List<User> getAllUsers(){
        return userRepository.findAll();
    }
    public User getUserById(Long id){
        return userRepository.findById(id).orElse(null);
    }
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(UserDoesNotExistException::new);
    }
    @Transactional
    public User registerUser(RegistrationObjectDto registrationObjectDto) {
        // Validate email uniqueness
        Optional<User> existingUserByEmail = userRepository.findByEmail(registrationObjectDto.getEmail());
        if (existingUserByEmail.isPresent()) {
            throw new EmailAlreadyExistException();
        }

        // Validate phone number uniqueness if provided
        if (registrationObjectDto.getPhoneNumber() != null && !registrationObjectDto.getPhoneNumber().isEmpty()) {
            Optional<User> existingUserByPhone = userRepository.findByPhoneNumber(registrationObjectDto.getPhoneNumber());
            if (existingUserByPhone.isPresent()) {
                throw new PhoneNumberAlreadyExistsException("Phone number already exists");
            }
        }

        User user = new User();
        user.setFirstName(registrationObjectDto.getFirstName());
        user.setLastName(registrationObjectDto.getLastName());
        user.setEmail(registrationObjectDto.getEmail());
        user.setPhoneNumber(registrationObjectDto.getPhoneNumber());
        user.setCountry(registrationObjectDto.getCountry());

        // Encode and set the password
        String encodedPassword = passwordEncoder.encode(registrationObjectDto.getPassword());
        user.setPassword(encodedPassword);
        user.setLastPassword(encodedPassword); // Store for password history

        // Set up the default role to be ROLE_TOURIST
        Set<Role> roles = new HashSet<>();
        roles.add(roleRepository.findByRoleType(Role.RoleType.ROLE_TOURIST)
                .orElseThrow(() -> new RuntimeException("Default role not found")));
        user.setRoles(roles);

        return userRepository.save(user);
    }
    @Transactional
    public User addUser(User user, String creatorRole) {
        // Validate email uniqueness
        Optional<User> existingUserByEmail = userRepository.findByEmail(user.getEmail());
        if (existingUserByEmail.isPresent()) {
            throw new EmailAlreadyExistException();
        }
        // Validate phone number uniqueness if provided
        if (user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty()) {
            Optional<User> existingUserByPhone = userRepository.findByPhoneNumber(user.getPhoneNumber());
            if (existingUserByPhone.isPresent()) {
                throw new PhoneNumberAlreadyExistsException("Phone number already exists");
            }
        }
        // Hash the password before saving
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setEnabled(true);

        // Handle roles based on creator's role
        Set<Role> roles = user.getRoles();
        if (roles == null || roles.isEmpty()) {
            // Default to TOURIST if no role specified
            Role defaultRole = roleRepository.findByRoleType(Role.RoleType.ROLE_TOURIST)
                    .orElseThrow(() -> new IllegalArgumentException("Default role ROLE_TOURIST not found"));
            roles = new HashSet<>(Set.of(defaultRole));
        } else if (!creatorRole.equals("ROLE_ADMIN")) {
            // MANAGER cannot assign ADMIN role
            boolean isAdminRole = roles.stream()
                    .anyMatch(role -> role.getRoleType().equals(Role.RoleType.ROLE_ADMIN));
            if (isAdminRole) {
                throw new AccessDeniedException("MANAGER cannot assign ADMIN role");
            }
            // Fetch roles from DB to ensure validity
            roles = roles.stream()
                    .map(role -> roleRepository.findByRoleType(role.getRoleType())
                            .orElseThrow(() -> new IllegalArgumentException("Role not found: " + role.getRoleType())))
                    .collect(Collectors.toSet());
        } else {
            // ADMIN can assign any role, fetch from DB to ensure validity
            roles = roles.stream()
                    .map(role -> roleRepository.findByRoleType(role.getRoleType())
                            .orElseThrow(() -> new IllegalArgumentException("Role not found: " + role.getRoleType())))
                    .collect(Collectors.toSet());
        }
        user.setRoles(roles);

        return userRepository.save(user);
    }
    @Transactional
    public User updateUser(User user) {
        User existingUser = getUserById(user.getId());
        if (existingUser == null) {
            return null;
        }
        // Update non-sensitive fields
        if (user.getFirstName() != null) {
            existingUser.setFirstName(user.getFirstName());
        }
        if (user.getLastName() != null) {
            existingUser.setLastName(user.getLastName());
        }
        // Validate phone number uniqueness before update
        if (user.getPhoneNumber() != null &&
                !user.getPhoneNumber().equals(existingUser.getPhoneNumber()) &&
                !user.getPhoneNumber().isEmpty()) {
            Optional<User> phoneCheck = userRepository.findByPhoneNumber(user.getPhoneNumber());
            if (phoneCheck.isPresent()) {
                throw new PhoneNumberAlreadyExistsException("Phone number already exists");
            }
            existingUser.setPhoneNumber(user.getPhoneNumber());
        }
        if (user.getCountry() != null) {
            existingUser.setCountry(user.getCountry());
        }
        // Handle security-related updates
        if (user.getFailedLoginAttempts() != existingUser.getFailedLoginAttempts()) {
            existingUser.setFailedLoginAttempts(user.getFailedLoginAttempts());
            existingUser.setLockoutTime(user.getLockoutTime());
        }
        // Handle refresh token updates if provided
        if (user.getRefreshToken() != null) {
            existingUser.setRefreshToken(user.getRefreshToken());
            existingUser.setRefreshTokenExpiry(user.getRefreshTokenExpiry());
        }
        return userRepository.save(existingUser);
    }
    @Transactional
    public User updateUserRole(Long userId, String roleType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserDoesNotExistException("User not found with id: "
                        + userId));
        // Map string roleType to enum
        Role.RoleType newRoleType;
        try {
            newRoleType = Role.RoleType.valueOf(roleType);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role type: " + roleType);
        }

        Role newRole = roleRepository.findByRoleType(newRoleType)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleType));

        Set<Role> roles = new HashSet<>();
        roles.add(newRole);
        user.setRoles(roles);
        return userRepository.save(user);
    }
    public User updatePassword(String email, String newPassword) {
        // Delegate to setPassword to enforce password reuse check
        return setPassword(email, newPassword);
    }
    @Transactional
    public User setPassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(UserDoesNotExistException::new);

        // Check if the new password is the same as the last password
        if (user.getLastPassword() != null && passwordEncoder.matches(newPassword, user.getLastPassword())) {
            throw new PasswordReusedException("Cannot reuse the previous password");
        }

        // Store the current password as last password
        user.setLastPassword(user.getPassword());

        // Set the new password
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);

        // Save and return the updated user
        return userRepository.save(user);
    }
    public User getUserByRefreshToken(String refreshToken) {
        return userRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));
    }
    @Transactional
    public void deleteById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserDoesNotExistException("User not found with id: " + id));

        // Check if the user to be deleted is the last ADMIN
        if (isAdmin(user) && isLastAdmin()) {
            throw new LastAdminException("Cannot delete the last admin user");
        }

        try {
            userRepository.delete(user);
        } catch (Exception e) {
            throw new RuntimeException("Error deleting user: " + e.getMessage());
        }
    }
    @Transactional
    public void deleteSelf(Long id, String password) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserDoesNotExistException("User not found with id: " + id));

        // Verify password for self-deletion
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidCredentialsException("Incorrect password provided for account deletion");
        }

        // Check if the user to be deleted is the last ADMIN
        if (isAdmin(user) && isLastAdmin()) {
            throw new LastAdminException("Cannot delete the last admin user");
        }

        try {
            userRepository.delete(user);
        } catch (Exception e) {
            throw new RuntimeException("Error deleting user: " + e.getMessage());
        }
    }

    private boolean isAdmin(User user) {
        return user.getRoles().stream()
                .anyMatch(role -> role.getRoleType().equals(Role.RoleType.ROLE_ADMIN));
    }
    private boolean isLastAdmin() {
        long adminCount = userRepository.findAll().stream()
                .filter(user -> user.getRoles().stream()
                        .anyMatch(role -> role.getRoleType().equals(Role.RoleType.ROLE_ADMIN)))
                .count();
        return adminCount <= 1;
    }
}
