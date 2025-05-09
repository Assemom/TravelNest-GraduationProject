package com.travel.management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripBasicDTO implements TripDTO {
    private Long id;
    private String title;
    private String destination;
    private String description;
    private Double price;
    private String duration;
    private String locationLink;
    private String tips;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String activity;
    private Double latitude;
    private Double longitude;
    private String imageUrl;
    private boolean available;

    @Override
    public LocalDateTime getCreatedAt() {
        return null; // Tourist view doesn't include createdAt
    }
    @Override
    public UserSummaryDTO getCreatedBy() {
        return null; // Tourist view doesn't include createdBy
    }
    @Override
    public boolean isAvailable() {
        return available;
    }
}
