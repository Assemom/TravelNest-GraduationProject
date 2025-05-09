package com.travel.management.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

public interface TripDTO {
    Long getId();
    String getTitle();
    String getDestination();
    String getDescription();
    Double getPrice();
    String getDuration();
    String getLocationLink();
    String getTips();
    LocalDateTime getStartTime();
    LocalDateTime getEndTime();
    String getActivity();
    Double getLatitude();
    Double getLongitude();
    String getImageUrl();
    LocalDateTime getCreatedAt();
    UserSummaryDTO getCreatedBy();
    boolean isAvailable();
}
