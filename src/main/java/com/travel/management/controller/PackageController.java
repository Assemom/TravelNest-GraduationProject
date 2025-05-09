package com.travel.management.controller;

import com.travel.management.dto.PackageCreateRequest;
import com.travel.management.dto.PackageDTO;
import com.travel.management.dto.PackageUpdateRequest;
import com.travel.management.model.Package;
import com.travel.management.service.PackageService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/packages")
public class PackageController {
    private final PackageService packageService;

    public PackageController(PackageService packageService) {
        this.packageService = packageService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<PackageDTO>> getAllPackages(
            @PageableDefault(size = 15) Pageable pageable,
            Authentication authentication) {
        return ResponseEntity.ok(packageService.getAllPackages(pageable, authentication));
    }
    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<PackageDTO>> searchPackages(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category,
            @PageableDefault(size = 15) Pageable pageable,
            Authentication authentication) {
        return ResponseEntity.ok(packageService.searchPackages(
                name, category, authentication, pageable));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<PackageDTO> createPackage(
            @Valid @RequestBody PackageCreateRequest request,
            Authentication authentication) {
        PackageDTO created = packageService.createPackage(request, authentication);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(created);
    }

    @PutMapping("/{packageId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<PackageDTO> updatePackage(
            @PathVariable Long packageId,
            @Valid @RequestBody PackageUpdateRequest request,
            Authentication authentication) {
        PackageDTO updated = packageService.updatePackage(packageId, request, authentication);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{packageId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> deletePackage(
            @PathVariable Long packageId,
            Authentication authentication) {
        packageService.deletePackage(packageId, authentication);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{packageId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PackageDTO> getPackageById(
            @PathVariable Long packageId,
            Authentication authentication) {
        return ResponseEntity.ok(packageService.getPackageById(packageId, authentication));
    }
    @GetMapping("/{packageId}/availability")
    public ResponseEntity<Boolean> checkPackageAvailability(
            @PathVariable Long packageId) {
        return ResponseEntity.ok(packageService.isPackageAvailableForBooking(packageId));
    }
}
