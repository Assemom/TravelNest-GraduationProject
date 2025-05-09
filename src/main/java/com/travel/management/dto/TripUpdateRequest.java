package com.travel.management.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripUpdateRequest {
    private String title;
    private String destination;
    private String description;

    @PositiveOrZero(message = "Price must be zero or positive")
    private Double price;

    private String duration;

    @Pattern(regexp = "^https?://maps\\.google\\.com/.*|^https?://goo\\.gl/maps/.*",
            message = "Please provide a valid Google Maps link")
    private String locationLink;

    private String tips;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String activity;
    private Boolean available;
}
