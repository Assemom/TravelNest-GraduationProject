package com.travel.management.dto;
import com.travel.management.model.Package;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PackageUpdateRequest {
    private String name;
    private String description;
    @PositiveOrZero(message = "Price must be zero or positive")
    private Double price;
    private String category;
    private String totalDuration;
    private Package.PackageStatus status;
    private Set<Long> tripIds;
}