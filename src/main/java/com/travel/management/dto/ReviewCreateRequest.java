package com.travel.management.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReviewCreateRequest {
    @NotNull
    @Size(min = 10, max = 500)
    private String content;

    @NotNull
    @Min(1) @Max(5)
    private Integer rating;

    @NotNull
    private Long itemId; // either trip_id or package_id

    @NotNull
    private ReviewType type; // TRIP or PACKAGE
    public enum ReviewType {
        TRIP,
        PACKAGE
    }
}


