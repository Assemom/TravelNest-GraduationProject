package com.travel.management.model;

import jakarta.persistence.*;
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
@Table(name = "trips")
public class Trip {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false,name = "name")
    private String title;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String description;

    @Column(name = "Entry_Fee")
    private Double price;

    @Column
    private String duration;

    @Column
    private String locationLink;

    @Column(name = "cultural_tip")
    private String tips;

    @Column(name = "open_time")
    private LocalDateTime startTime;

    @Column(name = "close_time")
    private LocalDateTime endTime;

    @Column(name = "category")
    private String activity;

    @Column(name = "Place_Latitude")
    private Double latitude;

    @Column(name = "Place_Longitude")
    private Double longitude;

    @Column
    private String image;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column
    private boolean available;

    @ManyToMany(mappedBy = "trips")
    private Set<Package> packages = new HashSet<>();
}
