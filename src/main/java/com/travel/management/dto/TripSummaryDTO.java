package com.travel.management.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TripSummaryDTO {
    private Long id;
    private String title;
    private String destination;
    private Double price;
    private String duration;
    private String imageUrl;
}
