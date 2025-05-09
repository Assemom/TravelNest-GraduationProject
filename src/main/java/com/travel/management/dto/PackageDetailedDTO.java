package com.travel.management.dto;

import com.travel.management.model.Package;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PackageDetailedDTO implements PackageDTO {
    private Long id;
    private String name;
    private String description;
    private Double price;
    private String category;
    private String totalDuration;
    private Package.PackageStatus status;
    private Set<TripBasicDTO> trips;
    private LocalDateTime createdAt;
    private UserSummaryDTO createdBy;
}
