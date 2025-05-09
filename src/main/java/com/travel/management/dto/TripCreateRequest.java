package com.travel.management.dto;

import jakarta.validation.constraints.NotBlank;
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
public class TripCreateRequest {
    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Destination is required")
    private String destination;

    @NotBlank(message = "Description is required")
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
}
