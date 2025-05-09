package com.travel.management.dto;

import com.travel.management.model.Booking;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class BookingAdminDTO extends BookingDTO {
    private String userEmail;
    private String userPhone;
    private String userFullName;

    @Builder(builderMethodName = "adminBuilder")
    public BookingAdminDTO(Long id, UserSummaryDTO user, String itemName,
                           String itemType, LocalDateTime bookingDateTime,
                           BigDecimal totalPrice, Booking.BookingStatus status,
                           LocalDateTime createdAt, PaymentDTO payment,
                           String userEmail, String userPhone, String userFullName) {
        super(id, user, itemName, itemType, bookingDateTime, totalPrice,
                status, createdAt, payment);
        this.userEmail = userEmail;
        this.userPhone = userPhone;
        this.userFullName = userFullName;
    }

}
