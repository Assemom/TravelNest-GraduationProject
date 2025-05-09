package com.travel.management.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BookingCreateRequest {
    @NotNull(message = "Item ID is required")
    private Long itemId;

    @NotNull(message = "Booking date and time is required")
    @Future(message = "Booking date must be in the future")
    private LocalDateTime bookingDateTime;

    @NotNull(message = "Booking type is required")
    private BookingType type;

    public enum BookingType {
        TRIP,
        PACKAGE
    }
}
