package com.travel.management.dto;

import com.travel.management.model.Booking;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingDTO {
    private Long id;
    private UserSummaryDTO user;
    private String itemName; // Trip title or Package name
    private String itemType; // "Trip" or "Package"
    private LocalDateTime bookingDateTime;
    private BigDecimal totalPrice;
    private Booking.BookingStatus status;
    private LocalDateTime createdAt;
    private PaymentDTO payment;
}
