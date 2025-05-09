package com.travel.management.service;

import com.travel.management.dto.ReviewCreateRequest;
import com.travel.management.dto.ReviewDTO;
import com.travel.management.dto.ReviewUpdateRequest;
import com.travel.management.dto.UserSummaryDTO;
import com.travel.management.exception.ResourceNotFoundException;
import com.travel.management.model.Package;
import com.travel.management.model.Review;
import com.travel.management.model.Role;
import com.travel.management.model.Trip;
import com.travel.management.model.User;
import com.travel.management.repository.*;
import jakarta.xml.bind.ValidationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.nio.file.AccessDeniedException;


@Service
@Transactional
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final PackageRepository packageRepository;
    private final BookingRepository bookingRepository;

    public ReviewService(ReviewRepository reviewRepository,
                         UserRepository userRepository,
                         TripRepository tripRepository,
                         PackageRepository packageRepository,
                         BookingRepository bookingRepository) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.tripRepository = tripRepository;
        this.packageRepository = packageRepository;
        this.bookingRepository = bookingRepository;
    }

    public Page<ReviewDTO> getReviewsForItem(Long itemId,
                                             ReviewCreateRequest.ReviewType type,
                                             Pageable pageable) {
        return reviewRepository.findByItemIdAndType(itemId, type.name(), pageable)
                .map(this::convertToDTO);
    }

    public Page<ReviewDTO> getUserReviews(Long userId, Pageable pageable) {
        return reviewRepository.findByUserId(userId, pageable)
                .map(this::convertToDTO);
    }

    public ReviewDTO createReview(ReviewCreateRequest request,
                                  Authentication authentication) {
        User user = getUserFromAuthentication(authentication);

        // Verify user has booked the item
        validateUserBooking(user.getId(), request.getItemId(), request.getType());

        Review review = new Review();
        review.setContent(request.getContent());
        review.setRating(request.getRating());
        review.setUser(user);

        if (request.getType() == ReviewCreateRequest.ReviewType.TRIP) {
            Trip trip = tripRepository.findById(request.getItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
            review.setTrip(trip);
        } else {
            Package pkg = packageRepository.findById(request.getItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Package not found"));
            review.setPkg(pkg);
        }

        Review savedReview = reviewRepository.save(review);
        return convertToDTO(savedReview);
    }

    public ReviewDTO updateReview(Long reviewId,
                                  ReviewUpdateRequest request,
                                  Authentication authentication) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        User user = getUserFromAuthentication(authentication);
        validateReviewOwnership(review, user);

        if (request.getContent() != null) review.setContent(request.getContent());
        if (request.getRating() != null) review.setRating(request.getRating());

        Review updatedReview = reviewRepository.save(review);
        return convertToDTO(updatedReview);
    }

    public void deleteReview(Long reviewId, Authentication authentication) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        User user = getUserFromAuthentication(authentication);
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getRoleType() == Role.RoleType.ROLE_ADMIN);

        if (!isAdmin && !review.getUser().getId().equals(user.getId())) {
            try {
                throw new AccessDeniedException("Not authorized to delete this review");
            } catch (AccessDeniedException e) {
                throw new RuntimeException(e);
            }
        }

        reviewRepository.delete(review);
    }

    public ReviewDTO toggleHighlight(Long reviewId, Authentication authentication) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        User user = getUserFromAuthentication(authentication);
        validateManagerOrAdmin(user);

        review.setHighlighted(!review.isHighlighted());
        Review updatedReview = reviewRepository.save(review);
        return convertToDTO(updatedReview);
    }

    public ReviewDTO toggleVisibility(Long reviewId, Authentication authentication) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        User user = getUserFromAuthentication(authentication);
        validateManagerOrAdmin(user);

        review.setStatus(review.getStatus() == Review.ReviewStatus.VISIBLE ?
                Review.ReviewStatus.HIDDEN : Review.ReviewStatus.VISIBLE);
        Review updatedReview = reviewRepository.save(review);
        return convertToDTO(updatedReview);
    }

    private void validateUserBooking(Long userId, Long itemId, ReviewCreateRequest.ReviewType type) {
        boolean hasBookingWithCompletedPayment = bookingRepository
                .existsByUserIdAndItemId(userId, itemId, type.name());

        if (!hasBookingWithCompletedPayment) {
            try {
                throw new ValidationException(
                        "You must have a completed booking and payment for this item before reviewing it");
            } catch (ValidationException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void validateReviewOwnership(Review review, User user) {
        if (!review.getUser().getId().equals(user.getId())) {
            try {
                throw new AccessDeniedException("Not authorized to update this review");
            } catch (AccessDeniedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void validateManagerOrAdmin(User user) {
        boolean isManagerOrAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getRoleType() == Role.RoleType.ROLE_ADMIN ||
                        role.getRoleType() == Role.RoleType.ROLE_MANAGER);
        if (!isManagerOrAdmin) {
            try {
                throw new AccessDeniedException("Only managers and admins can perform this action");
            } catch (AccessDeniedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private ReviewDTO convertToDTO(Review review) {
        return ReviewDTO.builder()
                .id(review.getId())
                .content(review.getContent())
                .rating(review.getRating())
                .user(convertToUserSummary(review.getUser()))
                .itemId(review.getTrip() != null ?
                        review.getTrip().getId() : review.getPkg().getId())
                .itemName(review.getTrip() != null ?
                        review.getTrip().getTitle() : review.getPkg().getName())
                .type(review.getTrip() != null ? ReviewCreateRequest.ReviewType.TRIP :
                        ReviewCreateRequest.ReviewType.PACKAGE)
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .highlighted(review.isHighlighted())
                .status(review.getStatus())
                .build();
    }
    private UserSummaryDTO convertToUserSummary(User user) {
        return UserSummaryDTO.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .build();
    }
    private User getUserFromAuthentication(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
