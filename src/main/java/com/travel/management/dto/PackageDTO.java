package com.travel.management.dto;

import com.travel.management.model.Package;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

public interface PackageDTO {
    Long getId();
    String getName();
    String getDescription();
    Double getPrice();
    String getCategory();
    String getTotalDuration();
    Package.PackageStatus getStatus();
    Set<TripBasicDTO> getTrips();
}
