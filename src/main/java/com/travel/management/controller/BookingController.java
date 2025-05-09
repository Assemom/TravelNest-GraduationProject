package com.travel.management.controller;

import com.travel.management.dto.BookingAdminDTO;
import com.travel.management.dto.BookingCreateRequest;
import com.travel.management.dto.BookingDTO;
import com.travel.management.model.Booking;
import com.travel.management.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@PreAuthorize("isAuthenticated()")
public class BookingController {
    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/trip")
    public ResponseEntity<BookingDTO> createTripBooking(
            @Valid @RequestBody BookingCreateRequest request,
            Authentication authentication) {
        BookingDTO booking = bookingService.createTripBooking(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(booking);
    }

    @PostMapping("/package")
    public ResponseEntity<BookingDTO> createPackageBooking(
            @Valid @RequestBody BookingCreateRequest request,
            Authentication authentication) {
        BookingDTO booking = bookingService.createPackageBooking(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(booking);
    }

    @GetMapping("/user")
    public ResponseEntity<Page<BookingDTO>> getUserBookings(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable,
            Authentication authentication) {
        return ResponseEntity.ok(bookingService.getUserBookings(authentication, pageable));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Page<BookingAdminDTO>> getAllBookings(
            @RequestParam(required = false) Booking.BookingStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endDate,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(bookingService.getAllBookings(status, startDate, endDate, pageable));
    }

    @PutMapping("/{bookingId}/status")
    public ResponseEntity<BookingDTO> updateBookingStatus(
            @PathVariable Long bookingId,
            @RequestParam Booking.BookingStatus status,
            Authentication authentication) {
        return ResponseEntity.ok(bookingService.updateBookingStatus(bookingId, status, authentication));
    }
}
