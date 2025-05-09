package com.travel.management.dto;

import com.travel.management.model.Package;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PackageBasicDTO implements PackageDTO {
    private Long id;
    private String name;
    private String description;
    private Double price;
    private String category;
    private String totalDuration;
    private Package.PackageStatus status;
    private Set<TripBasicDTO> trips;
}
