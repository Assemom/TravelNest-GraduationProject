package com.travel.management.controller;

import com.travel.management.dto.UserResponseDTO;
import com.travel.management.model.Role;
import com.travel.management.service.UserSearchService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/users")
public class UserSearchController {

    private final UserSearchService userSearchService;

    public UserSearchController(UserSearchService userSearchService) {
        this.userSearchService = userSearchService;
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")  // Only ADMIN and MANAGER can access
    public ResponseEntity<Page<UserResponseDTO>> searchUsers(
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String roleType,
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication) {

        if (StringUtils.hasText(roleType)) {
            try {
                Role.RoleType.valueOf(roleType);
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid role type");
            }
        }

        Page<UserResponseDTO> results = userSearchService.searchUsers(
                firstName, lastName, country, roleType, authentication, pageable);
        return ResponseEntity.ok(results);
    }
}