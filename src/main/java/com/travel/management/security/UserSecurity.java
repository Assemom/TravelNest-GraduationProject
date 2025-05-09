package com.travel.management.security;

import com.travel.management.model.Role;
import com.travel.management.model.User;
import com.travel.management.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("userSecurity")
public class UserSecurity {

    private final UserService userService;

    @Autowired
    public UserSecurity(UserService userService) {
        this.userService = userService;
    }

    public boolean isCurrentUser(Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        String currentUserEmail = authentication.getName(); // Assuming email is the principal from JWT
        User currentUser = userService.getUserByEmail(currentUserEmail);
        return currentUser != null && currentUser.getId().equals(userId);
    }

    public boolean isTouristUser(Long userId) {
        User user = userService.getUserById(userId);
        if (user == null) {
            return false;
        }
        return user.getRoles().stream()
                .anyMatch(role -> role.getRoleType().equals(Role.RoleType.ROLE_TOURIST));
    }
}