package com.travel.management.controller;


import com.travel.management.dto.*;
import com.travel.management.exception.ResourceNotFoundException;
import com.travel.management.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/reviews")
@PreAuthorize("isAuthenticated()")
public class ReviewController {
    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/items/{itemId}")
    public ResponseEntity<Page<ReviewDTO>> getReviewsForItem(
            @PathVariable Long itemId,
            @RequestParam(name = "type") String type,
            @PageableDefault(size = 3, sort = "rating", direction = Sort.Direction.DESC)
            Pageable pageable) {
        ReviewCreateRequest.ReviewType reviewType = ReviewCreateRequest.ReviewType.valueOf(type.toUpperCase());
        return ResponseEntity.ok(reviewService.getReviewsForItem(itemId, reviewType, pageable));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<Page<ReviewDTO>> getUserReviews(
            @PathVariable Long userId,
            @PageableDefault(size = 3, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(reviewService.getUserReviews(userId, pageable));
    }

    @PostMapping("/trip")
    public ResponseEntity<ReviewDTO> createTripReview(
            @Valid @RequestBody TripReviewRequest request,
            Authentication authentication) {
        ReviewCreateRequest reviewRequest = ReviewCreateRequest.builder()
                .content(request.getContent())
                .rating(request.getRating())
                .itemId(request.getTripId())
                .type(ReviewCreateRequest.ReviewType.TRIP)
                .build();

        ReviewDTO review = reviewService.createReview(reviewRequest, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(review);
    }

    // Create review for Package
    @PostMapping("/package")
    public ResponseEntity<ReviewDTO> createPackageReview(
            @Valid @RequestBody PackageReviewRequest request,
            Authentication authentication) {
        ReviewCreateRequest reviewRequest = ReviewCreateRequest.builder()
                .content(request.getContent())
                .rating(request.getRating())
                .itemId(request.getPackageId())
                .type(ReviewCreateRequest.ReviewType.PACKAGE)
                .build();

        ReviewDTO review = reviewService.createReview(reviewRequest, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(review);
    }


    @PutMapping("/{reviewId}")
    public ResponseEntity<ReviewDTO> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewUpdateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(reviewService.updateReview(reviewId, request, authentication));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long reviewId,
            Authentication authentication) {
        reviewService.deleteReview(reviewId, authentication);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{reviewId}/highlight")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ReviewDTO> toggleHighlight(
            @PathVariable Long reviewId,
            Authentication authentication) {
        return ResponseEntity.ok(reviewService.toggleHighlight(reviewId, authentication));
    }

    @PutMapping("/{reviewId}/visibility")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ReviewDTO> toggleVisibility(
            @PathVariable Long reviewId,
            Authentication authentication) {
        return ResponseEntity.ok(reviewService.toggleVisibility(reviewId, authentication));
    }
}
