package com.travel.management.controller;

import com.travel.management.dto.PaymentDTO;
import com.travel.management.dto.PaymentRequest;
import com.travel.management.exception.DuplicatePaymentException;
import com.travel.management.exception.PaymentValidationException;
import com.travel.management.model.Payment;
import com.travel.management.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/payments")
@PreAuthorize("isAuthenticated()")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentDTO> getPayment(
            @PathVariable Long paymentId,
            Authentication authentication) {
        PaymentDTO payment = paymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(payment);
    }

    @GetMapping("/status/{bookingId}")
    public ResponseEntity<PaymentDTO> getPaymentStatus(
            @PathVariable Long bookingId,
            Authentication authentication) {
        return ResponseEntity.ok(paymentService.getPaymentStatus(bookingId, authentication));
    }

    @PostMapping("/process")
    public ResponseEntity<PaymentDTO> processPayment(
            @Valid @RequestBody PaymentRequest request,
            Authentication authentication) {
        PaymentDTO payment = paymentService.processPayment(request, authentication);
        return ResponseEntity.ok(payment);
    }
}

