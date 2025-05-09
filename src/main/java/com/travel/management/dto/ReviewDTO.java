package com.travel.management.dto;

import com.travel.management.model.Review;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReviewDTO {
    private Long id;
    private String content;
    private Integer rating;
    private UserSummaryDTO user;
    private Long itemId;
    private String itemName;
    private ReviewCreateRequest.ReviewType type;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean highlighted;
    private Review.ReviewStatus status;
}
