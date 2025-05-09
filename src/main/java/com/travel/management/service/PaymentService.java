package com.travel.management.service;

import com.travel.management.dto.PaymentDTO;
import com.travel.management.dto.PaymentRequest;
import com.travel.management.exception.DuplicatePaymentException;
import com.travel.management.exception.PaymentNotFoundException;
import com.travel.management.exception.PaymentValidationException;
import com.travel.management.exception.ResourceNotFoundException;
import com.travel.management.model.Booking;
import com.travel.management.model.Payment;
import com.travel.management.model.Role;
import com.travel.management.model.User;
import com.travel.management.repository.BookingRepository;
import com.travel.management.repository.PaymentRepository;
import com.travel.management.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

@Service
@Transactional
@Slf4j
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final EmailService emailService;
    private final UserRepository userRepository;

    public PaymentService(PaymentRepository paymentRepository,
                          BookingRepository bookingRepository,
                          EmailService emailService, UserRepository userRepository) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.emailService = emailService;
        this.userRepository = userRepository;
    }

    public PaymentDTO processPayment(PaymentRequest request, Authentication authentication) {
        // 1. Get the booking and its existing payment
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        Payment payment = paymentRepository.findByBookingId(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        // 2. Get the authenticated user
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 3. Validate user owns the booking
        if (!booking.getUser().getId().equals(user.getId())) {
            try {
                throw new AccessDeniedException("You are not authorized to make payment for this booking");
            } catch (AccessDeniedException e) {
                throw new RuntimeException(e);
            }
        }

        // 4. Validate payment status
        if (payment.getStatus() == Payment.PaymentStatus.COMPLETED) {
            throw new DuplicatePaymentException("Payment is already completed for this booking");
        }

        // 5. Validate payment amount (using BigDecimal for precise comparison)
        BigDecimal requestAmount = BigDecimal.valueOf(request.getAmount());
        if (booking.getTotalPrice().compareTo(requestAmount) != 0) {
            throw new PaymentValidationException(
                    String.format("Payment amount %s does not match booking total %s",
                            requestAmount, booking.getTotalPrice())
            );
        }

        // 6. Update payment
        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        payment.setPaymentDate(LocalDateTime.now());
        payment.setTransactionId(generateTransactionId());

        // 7. Update booking status
        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        // 8. Save payment
        Payment savedPayment = paymentRepository.save(payment);

        // 9. Send confirmation email
        try {
            sendPaymentConfirmationEmail(savedPayment);
        } catch (Exception e) {
            log.error("Failed to send payment confirmation email", e);
        }

        return convertToDTO(savedPayment);
    }

    public PaymentDTO getPaymentById(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));

        return convertToDTO(payment);
    }


    private String generateTransactionId() {
        return "TXN" + System.currentTimeMillis() +
                String.format("%04d", new Random().nextInt(10000));
    }
    private void validatePaymentAccess(Payment payment, Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isAdminOrManager = user.getRoles().stream()
                .anyMatch(role -> role.getRoleType() == Role.RoleType.ROLE_ADMIN ||
                        role.getRoleType() == Role.RoleType.ROLE_MANAGER);

        if (!isAdminOrManager && !payment.getBooking().getUser().getId().equals(user.getId())) {
            try {
                throw new AccessDeniedException("You don't have permission to view this payment");
            } catch (AccessDeniedException e) {
                throw new RuntimeException(e);
            }
        }
    }


    public PaymentDTO getPaymentStatus(Long bookingId, Authentication authentication) {
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        validatePaymentAccess(payment, authentication);

        return convertToDTO(payment);
    }
    private void sendPaymentConfirmationEmail(Payment payment) {
        String subject = "Payment Confirmation - Travel Nest";
        String htmlContent = generatePaymentConfirmationEmail(payment);
        emailService.sendEmail(payment.getBooking().getUser().getEmail(), subject, htmlContent);
    }

    private String generatePaymentConfirmationEmail(Payment payment) {
        Booking booking = payment.getBooking();
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    .email-container {
                        font-family: Arial, sans-serif;
                        max-width: 600px;
                        margin: 0 auto;
                        padding: 20px;
                        background-color: #ffffff;
                    }
                    .header {
                        background-color: #2c3e50;
                        color: white;
                        padding: 20px;
                        text-align: center;
                        border-radius: 5px 5px 0 0;
                    }
                    .content {
                        padding: 20px;
                        background-color: #ffffff;
                        border: 1px solid #f0c17a;
                        border-radius: 0 0 5px 5px;
                    }
                    .payment-details {
                        background-color: #f8f9fa;
                        padding: 15px;
                        margin: 20px 0;
                        border-radius: 5px;
                    }
                    .highlight {
                        color: #f0c17a;
                        font-weight: bold;
                    }
                    .footer {
                        text-align: center;
                        margin-top: 20px;
                        color: #666;
                        font-size: 12px;
                    }
                </style>
            </head>
            <body>
                <div class="email-container">
                    <div class="header">
                        <h1>Payment Confirmation</h1>
                    </div>
                    <div class="content">
                        <h2>Thank you for your payment!</h2>
                        <div class="payment-details">
                            <p><strong>Transaction ID:</strong> %s</p>
                            <p><strong>Amount Paid:</strong> $%.2f</p>
                            <p><strong>Payment Date:</strong> %s</p>
                            <p><strong>Booking Reference:</strong> %s</p>
                            <p><strong>Status:</strong> %s</p>
                        </div>
                        <p>Your booking has been confirmed.</p>
                    </div>
                    <div class="footer">
                        <p>© 2024 Travel Nest. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                payment.getTransactionId(),
                payment.getAmount(),
                payment.getPaymentDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")),
                payment.getBooking().getId(),
                payment.getStatus()
        );
    }

    private PaymentDTO convertToDTO(Payment payment) {
        return PaymentDTO.builder()
                .id(payment.getId())
                .bookingId(payment.getBooking().getId())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .paymentDate(payment.getPaymentDate())
                .transactionId(payment.getTransactionId())
                .build();
    }
}

