package com.travel.management.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "packages")
public class Package {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Package name cannot be blank")
    @Column(nullable = false)
    private String name;

    private String description;

    @PositiveOrZero(message = "Price must be zero or positive")
    private Double price;

    @Column
    private String category;// For categorizing packages (e.g., "Adventure", "Cultural")

    private String totalDuration;

    @Enumerated(EnumType.STRING)
    private PackageStatus status;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToMany
    @JoinTable(
            name = "package_trips",
            joinColumns = @JoinColumn(name = "package_id"),
            inverseJoinColumns = @JoinColumn(name = "trip_id"),
            uniqueConstraints = @UniqueConstraint(
                    columnNames = {"package_id", "trip_id"} // Prevent duplicate trips in the same package
            )
    )
    private Set<Trip> trips = new HashSet<>();

    public enum PackageStatus {
        PUBLIC, PRIVATE
    }
}