package com.travel.management.repository;

import com.travel.management.dto.ReviewCreateRequest;
import com.travel.management.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    @Query("SELECT r FROM Review r WHERE " +
            "(r.trip.id = :itemId AND :type = 'TRIP') OR " +
            "(r.pkg.id = :itemId AND :type = 'PACKAGE')")
    Page<Review> findByItemIdAndType(
            @Param("itemId") Long itemId,
            @Param("type") String type,
            Pageable pageable);
    default boolean existsByUserIdAndItemId(Long userId, Long itemId, ReviewCreateRequest.ReviewType type) {
        if (type == ReviewCreateRequest.ReviewType.TRIP) {
            return existsByUserIdAndTripId(userId, itemId);
        } else {
            return existsByUserIdAndPackageId(userId, itemId);
        }
    }

    @Query("SELECT COUNT(b) > 0 FROM Booking b WHERE b.user.id = :userId AND b.trip.id = :tripId")
    boolean existsByUserIdAndTripId(@Param("userId") Long userId, @Param("tripId") Long tripId);

    @Query("SELECT COUNT(b) > 0 FROM Booking b WHERE b.user.id = :userId AND b.bookedPackage.id = :packageId")
    boolean existsByUserIdAndPackageId(@Param("userId") Long userId, @Param("packageId") Long packageId);
    Page<Review> findByUserId(Long userId, Pageable pageable);
}
