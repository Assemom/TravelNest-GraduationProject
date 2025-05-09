package com.travel.management.controller;

import com.travel.management.dto.TripCreateRequest;
import com.travel.management.dto.TripDTO;
import com.travel.management.dto.TripUpdateRequest;
import com.travel.management.service.TripService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;


@RestController
@RequestMapping("/api/trips")
public class TripController {
    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<TripDTO>> getAllTrips(
            @PageableDefault(size = 15) Pageable pageable,
            Authentication authentication) {
        return ResponseEntity.ok(tripService.getAllTrips(pageable, authentication));
    }
    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<TripDTO>> searchTrips(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) String activity,
            @RequestParam(required = false) String category,
            @PageableDefault(size = 15) Pageable pageable,
            Authentication authentication) {
        return ResponseEntity.ok(tripService.searchTrips(
                title, destination, activity, category, authentication, pageable));
    }
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<TripDTO> createTrip(
            @Valid @RequestPart("trip") TripCreateRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image,
            Authentication authentication) throws IOException {
        TripDTO trip = tripService.createTrip(request, image, authentication);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(trip);
    }
    @PutMapping(value = "/{tripId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<TripDTO> updateTrip(
            @PathVariable Long tripId,
            @Valid @RequestPart("trip") TripUpdateRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image,
            Authentication authentication) {
        return ResponseEntity.ok(tripService.updateTrip(tripId, request, image, authentication));
    }
    @DeleteMapping("/{tripId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> deleteTrip(
            @PathVariable Long tripId,
            Authentication authentication) {
        tripService.deleteTrip(tripId, authentication);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/{tripId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TripDTO> getTripById(
            @PathVariable Long tripId,
            Authentication authentication) {
        return ResponseEntity.ok(tripService.getTripById(tripId, authentication));
    }
}
