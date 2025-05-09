package com.travel.management.dto;

import lombok.*;


import java.time.LocalDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripDetailedDTO implements TripDTO {
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
    private LocalDateTime createdAt;
    private UserSummaryDTO createdBy;
    private boolean available;

    @Override
    public boolean isAvailable() {
        return available;
    }
}
