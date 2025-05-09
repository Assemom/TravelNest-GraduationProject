package com.travel.management.service;

import com.travel.management.dto.RoleDTO;
import com.travel.management.dto.UserResponseDTO;
import com.travel.management.model.User;
import com.travel.management.model.Role;
import com.travel.management.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;

import java.nio.file.AccessDeniedException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserSearchService {

    private final UserRepository userRepository;

    public UserSearchService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Page<UserResponseDTO> searchUsers(String firstName, String lastName, String country,
                                             String roleType, Authentication authentication,
                                             Pageable pageable) {
        // Get current user from database
        User currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify user is ADMIN or MANAGER
        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(role -> role.getRoleType() == Role.RoleType.ROLE_ADMIN);
        boolean isManager = currentUser.getRoles().stream()
                .anyMatch(role -> role.getRoleType() == Role.RoleType.ROLE_MANAGER);

        if (!isAdmin && !isManager) {
            try {
                throw new AccessDeniedException("Only ADMIN and MANAGER roles can search users");
            } catch (AccessDeniedException e) {
                throw new RuntimeException(e);
            }
        }

        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            List<Predicate> searchPredicates = new ArrayList<>();

            // Add search criteria
            if (StringUtils.hasText(firstName)) {
                searchPredicates.add(cb.like(cb.lower(root.get("firstName")),
                        "%" + firstName.toLowerCase() + "%"));
            }

            if (StringUtils.hasText(lastName)) {
                searchPredicates.add(cb.like(cb.lower(root.get("lastName")),
                        "%" + lastName.toLowerCase() + "%"));
            }

            if (StringUtils.hasText(country)) {
                searchPredicates.add(cb.like(cb.lower(root.get("country")),
                        "%" + country.toLowerCase() + "%"));
            }

            // Add role filter if provided
            if (StringUtils.hasText(roleType)) {
                Join<User, Role> roleJoin = root.join("roles");
                predicates.add(cb.equal(roleJoin.get("roleType"),
                        Role.RoleType.valueOf(roleType)));
            }

            // Apply role-based visibility restrictions
            if (!isAdmin && isManager) {
                // Managers can only see TOURIST users
                Join<User, Role> roleJoin = root.join("roles");
                predicates.add(cb.equal(roleJoin.get("roleType"),
                        Role.RoleType.ROLE_TOURIST));
            }

            // Combine search predicates with OR logic
            if (!searchPredicates.isEmpty()) {
                predicates.add(cb.or(searchPredicates.toArray(new Predicate[0])));
            }

            // Combine all predicates with AND logic
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return userRepository.findAll(spec, pageable).map(this::convertToDTO);
    }

    private UserResponseDTO convertToDTO(User user) {
        return UserResponseDTO.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .country(user.getCountry())
                .createdAt(user.getCreatedAt())
                .roles(user.getRoles().stream()
                        .map(role -> RoleDTO.builder()
                                .id(role.getId())
                                .name(role.getRoleType().name())
                                .build())
                        .collect(Collectors.toSet()))
                .enabled(user.isEnabled())
                .build();
    }
}