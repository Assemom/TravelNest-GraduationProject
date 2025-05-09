package com.travel.management.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "bookings")
public class Booking {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "booking_id")
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "user_id", nullable = false)
        private User user;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "trip_id")
        private Trip trip;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "package_id")
        private Package bookedPackage;

        @Column(nullable = false)
        private LocalDateTime bookingDateTime;

        @Column(nullable = false)
        private BigDecimal totalPrice;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        private BookingStatus status = BookingStatus.CONFIRMED;

        @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL)
        private Payment payment;

        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        @PrePersist
        protected void onCreate() {
                createdAt = LocalDateTime.now();
        }

        @PreUpdate
        protected void onUpdate() {
                updatedAt = LocalDateTime.now();
        }
        public enum BookingStatus {
                CONFIRMED,
                CANCELLED
        }
}

