package com.travel.management.repository;

import com.travel.management.dto.ReviewCreateRequest;
import com.travel.management.model.Booking;
import com.travel.management.model.Package;
import com.travel.management.model.Trip;
import com.travel.management.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("SELECT COUNT(b) > 0 FROM Booking b JOIN Payment p ON b.id = p.booking.id " +
            "WHERE b.user.id = :userId AND p.status = 'COMPLETED' AND " +
            "((b.trip.id = :itemId AND :type = 'TRIP') OR " +
            "(b.bookedPackage.id = :itemId AND :type = 'PACKAGE'))")
    boolean existsByUserIdAndItemId(@Param("userId") Long userId,
                                    @Param("itemId") Long itemId,
                                    @Param("type") String type);
    @Query("SELECT COUNT(b) > 0 FROM Booking b JOIN Payment p ON b.id = p.booking.id " +
            "WHERE b.user.id = :userId AND b.trip.id = :tripId AND p.status = 'COMPLETED'")
    boolean existsByUserIdAndTripIdWithCompletedPayment(
            @Param("userId") Long userId,
            @Param("tripId") Long tripId);

    boolean existsByUserAndBookedPackageAndBookingDateTime(
            User user,
            Package bookedPackage,
            LocalDateTime bookingDateTime);

    List<Booking> findByUser(User user);

    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId AND " +
            "((b.trip.id = :itemId AND :type = 'TRIP') OR " +
            "(b.bookedPackage.id = :itemId AND :type = 'PACKAGE'))")
    List<Booking> findByUserIdAndItemId(@Param("userId") Long userId,
                                        @Param("itemId") Long itemId,
                                        @Param("type") ReviewCreateRequest.ReviewType type);

    @Query("SELECT b FROM Booking b WHERE " +
            "(:status IS NULL OR b.status = :status) AND " +
            "(:startDate IS NULL OR b.bookingDateTime >= :startDate) AND " +
            "(:endDate IS NULL OR b.bookingDateTime <= :endDate)")
    Page<Booking> findAllWithFilters(
            @Param("status") Booking.BookingStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    Page<Booking> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId AND b.status = :status")
    Page<Booking> findByUserIdAndStatus(
            @Param("userId") Long userId,
            @Param("status") Booking.BookingStatus status,
            Pageable pageable);

    boolean existsByUserAndTripAndBookingDateTime(User user, Trip trip,LocalDateTime bookingDateTime);
}


