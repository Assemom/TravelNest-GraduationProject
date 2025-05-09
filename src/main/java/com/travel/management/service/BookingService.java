package com.travel.management.service;

import com.travel.management.dto.*;
import com.travel.management.exception.DuplicateBookingException;
import com.travel.management.exception.EmailFailedToSendException;
import com.travel.management.exception.ResourceNotFoundException;
import com.travel.management.model.*;
import com.travel.management.model.Package;
import com.travel.management.repository.*;
import jakarta.xml.bind.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;

@Service
@Transactional
@Slf4j
public class BookingService {
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final PackageRepository packageRepository;
    private final PaymentRepository paymentRepository;
    private final EmailService emailService;

    public BookingService(BookingRepository bookingRepository,
                          UserRepository userRepository,
                          TripRepository tripRepository,
                          PackageRepository packageRepository,
                          PaymentRepository paymentRepository,
                          EmailService emailService) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.tripRepository = tripRepository;
        this.packageRepository = packageRepository;
        this.paymentRepository = paymentRepository;
        this.emailService = emailService;
    }

    public BookingDTO createTripBooking(BookingCreateRequest request, Authentication authentication) {
        User user = getUserFromAuthentication(authentication);
        Trip trip = tripRepository.findById(request.getItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));

        validateBookingDateTime(request.getBookingDateTime());
        validateDuplicateBooking(user, trip, request.getBookingDateTime());

        // Create booking
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setTrip(trip);
        booking.setBookingDateTime(request.getBookingDateTime());
        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        booking.setTotalPrice(BigDecimal.valueOf(trip.getPrice()));

        // Save booking first
        Booking savedBooking = bookingRepository.save(booking);

        // Create payment
        Payment payment = new Payment();
        payment.setBooking(savedBooking);
        payment.setAmount(trip.getPrice());
        payment.setStatus(Payment.PaymentStatus.PENDING);  // Use enum here

        // Set payment to booking and save again
        savedBooking.setPayment(payment);
        savedBooking = bookingRepository.save(savedBooking);

        // Send notifications
        try {
            sendBookingConfirmationEmail(savedBooking);
            sendAdminNotificationEmail(savedBooking);
        } catch (EmailFailedToSendException e) {
            log.error("Failed to send booking notification emails", e);
        }

        return convertToDTO(savedBooking);
    }

    public BookingDTO createPackageBooking(BookingCreateRequest request, Authentication authentication) {
        User user = getUserFromAuthentication(authentication);
        Package pkg = packageRepository.findById(request.getItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Package not found"));

        validateBookingDateTime(request.getBookingDateTime());
        validateDuplicatePackageBooking(user, pkg, request.getBookingDateTime());

        // Create booking
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setBookedPackage(pkg);
        booking.setBookingDateTime(request.getBookingDateTime());
        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        booking.setTotalPrice(BigDecimal.valueOf(pkg.getPrice()));

        // Save booking first
        Booking savedBooking = bookingRepository.save(booking);

        // Create payment
        Payment payment = new Payment();
        payment.setBooking(savedBooking);
        payment.setAmount(pkg.getPrice());
        payment.setStatus(Payment.PaymentStatus.PENDING);  // Use enum here

        // Set payment to booking and save again
        savedBooking.setPayment(payment);
        savedBooking = bookingRepository.save(savedBooking);

        // Send notifications
        try {
            sendBookingConfirmationEmail(savedBooking);
            sendAdminNotificationEmail(savedBooking);
        } catch (EmailFailedToSendException e) {
            log.error("Failed to send booking notification emails", e);
        }

        return convertToDTO(savedBooking);
    }

    public Page<BookingDTO> getUserBookings(Authentication authentication, Pageable pageable) {
        User user = getUserFromAuthentication(authentication);
        return bookingRepository.findByUserId(user.getId(), pageable)
                .map(this::convertToDTO);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public Page<BookingAdminDTO> getAllBookings(Booking.BookingStatus status,
                                                LocalDateTime startDate,
                                                LocalDateTime endDate,
                                                Pageable pageable) {
        return bookingRepository.findAllWithFilters(status, startDate, endDate, pageable)
                .map(this::convertToAdminDTO);
    }

    public BookingDTO updateBookingStatus(Long bookingId,
                                          Booking.BookingStatus newStatus,
                                          Authentication authentication) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        User user = getUserFromAuthentication(authentication);
        validateBookingStatusUpdate(booking, user, newStatus);

        booking.setStatus(newStatus);
        Booking updatedBooking = bookingRepository.save(booking);

        // Send status update email
        sendBookingStatusUpdateEmail(updatedBooking);

        return convertToDTO(updatedBooking);
    }

    private void sendBookingConfirmationEmail(Booking booking) {
        String subject = "Booking Confirmation - Travel Nest";
        String htmlContent = emailService.generateBookingConfirmationEmail(booking);
        emailService.sendEmail(booking.getUser().getEmail(), subject, htmlContent);
    }

    private void sendAdminNotificationEmail(Booking booking) {
        String subject = "New Booking Notification - Travel Nest";
        String htmlContent = emailService.generateAdminNotificationEmail(booking);
        emailService.sendEmail("admin@travelnest.com", subject, htmlContent);
    }

    private void validateBookingDateTime(LocalDateTime bookingDateTime) {
        if (bookingDateTime.isBefore(LocalDateTime.now())) {
            try {
                throw new ValidationException("Booking date must be in the future");
            } catch (ValidationException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void validateDuplicateBooking(User user, Trip trip, LocalDateTime bookingDateTime) {
        if (bookingRepository.existsByUserAndTripAndBookingDateTime(user, trip, bookingDateTime)) {
            throw new DuplicateBookingException("You already have a booking for this trip at this time");
        }
    }

    private void validateDuplicatePackageBooking(User user, Package pkg, LocalDateTime bookingDateTime) {
        if (bookingRepository.existsByUserAndBookedPackageAndBookingDateTime(user, pkg, bookingDateTime)) {
            throw new DuplicateBookingException("You already have a booking for this package at this time");
        }
    }

    private void validateBookingStatusUpdate(Booking booking, User user, Booking.BookingStatus newStatus) {
        boolean isAdminOrManager = user.getRoles().stream()
                .anyMatch(role -> role.getRoleType() == Role.RoleType.ROLE_ADMIN ||
                        role.getRoleType() == Role.RoleType.ROLE_MANAGER);

        if (!isAdminOrManager && !booking.getUser().getId().equals(user.getId())) {
            try {
                throw new AccessDeniedException("You don't have permission to update this booking");
            } catch (AccessDeniedException e) {
                throw new RuntimeException(e);
            }
        }

        if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            try {
                throw new ValidationException("Cannot update a cancelled booking");
            } catch (ValidationException e) {
                throw new RuntimeException(e);
            }
        }
    }
    private UserSummaryDTO convertToUserSummary(User user) {
        return UserSummaryDTO.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .build();
    }
    private BookingDTO convertToDTO(Booking booking) {
        return BookingDTO.builder()
                .id(booking.getId())
                .user(convertToUserSummary(booking.getUser()))
                .itemName(getItemName(booking))
                .itemType(getItemType(booking))
                .bookingDateTime(booking.getBookingDateTime())
                .totalPrice(booking.getTotalPrice())
                .status(booking.getStatus())
                .createdAt(booking.getCreatedAt())
                .payment(convertToPaymentDTO(booking.getPayment()))
                .build();
    }

    private BookingAdminDTO convertToAdminDTO(Booking booking) {
        return BookingAdminDTO.adminBuilder()
                .id(booking.getId())
                .user(convertToUserSummary(booking.getUser()))
                .itemName(getItemName(booking))
                .itemType(getItemType(booking))
                .bookingDateTime(booking.getBookingDateTime())
                .totalPrice(booking.getTotalPrice())
                .status(booking.getStatus())
                .createdAt(booking.getCreatedAt())
                .payment(convertToPaymentDTO(booking.getPayment()))
                .userEmail(booking.getUser().getEmail())
                .userPhone(booking.getUser().getPhoneNumber())
                .userFullName(booking.getUser().getFirstName() + " " + booking.getUser().getLastName())
                .build();
    }

    private PaymentDTO convertToPaymentDTO(Payment payment) {
        if (payment == null) return null;
        return PaymentDTO.builder()
                .id(payment.getId())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .build();
    }

    private String getItemName(Booking booking) {
        return booking.getTrip() != null ?
                booking.getTrip().getTitle() :
                booking.getBookedPackage().getName();
    }
    private void sendBookingStatusUpdateEmail(Booking booking) {
        String subject = "Booking Status Update - Travel Nest";
        String htmlContent = emailService.generateStatusUpdateEmail(booking);
        emailService.sendEmail(booking.getUser().getEmail(), subject, htmlContent);
    }
    private String getItemType(Booking booking) {
        return booking.getTrip() != null ? "Trip" : "Package";
    }

    private User getUserFromAuthentication(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}


