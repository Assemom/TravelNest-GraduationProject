package com.travel.management.dto;

import com.travel.management.model.Payment;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PaymentDTO {
    private Long id;
    private Long bookingId;
    private Payment.PaymentStatus status;
    private Double amount;
    private LocalDateTime paymentDate;
    private String transactionId;
}
