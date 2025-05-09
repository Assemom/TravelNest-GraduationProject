package com.travel.management.service;

import com.travel.management.dto.*;
import com.travel.management.exception.DuplicateResourceException;
import com.travel.management.exception.ResourceNotFoundException;
import com.travel.management.model.Role;
import com.travel.management.model.Trip;
import com.travel.management.model.User;
import com.travel.management.repository.TripRepository;
import com.travel.management.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.ArrayList;
import java.util.List;


@Service
@Transactional
public class TripService {
    private final TripRepository tripRepository;
    private final ImageService imageService;
    private final UserRepository userRepository;

    public TripService(TripRepository tripRepository,
                       ImageService imageService,
                       UserRepository userRepository) {
        this.tripRepository = tripRepository;
        this.imageService = imageService;
        this.userRepository = userRepository;
    }
    public Page<TripDTO> getAllTrips(Pageable pageable, Authentication authentication) {
        User currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isAdminOrManager = currentUser.getRoles().stream()
                .anyMatch(role -> role.getRoleType() == Role.RoleType.ROLE_ADMIN ||
                        role.getRoleType() == Role.RoleType.ROLE_MANAGER);

        Specification<Trip> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!isAdminOrManager) {
                predicates.add(cb.isTrue(root.get("available")));
            }
            return predicates.isEmpty() ? null : cb.and(predicates.toArray(new Predicate[0]));
        };

        return tripRepository.findAll(spec, pageable)
                .map(trip -> convertToDTO(trip, isAdminOrManager));
    }

    public Page<TripDTO> searchTrips(String title, String destination, String activity,
                                     String category, Authentication authentication,
                                     Pageable pageable) {
        User currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isAdminOrManager = currentUser.getRoles().stream()
                .anyMatch(role -> role.getRoleType() == Role.RoleType.ROLE_ADMIN ||
                        role.getRoleType() == Role.RoleType.ROLE_MANAGER);

        Specification<Trip> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            List<Predicate> searchPredicates = new ArrayList<>();

            if (StringUtils.hasText(title)) {
                searchPredicates.add(cb.like(cb.lower(root.get("title")),
                        "%" + title.toLowerCase() + "%"));
            }
            if (StringUtils.hasText(destination)) {
                searchPredicates.add(cb.like(cb.lower(root.get("destination")),
                        "%" + destination.toLowerCase() + "%"));
            }
            if (StringUtils.hasText(activity)) {
                searchPredicates.add(cb.like(cb.lower(root.get("activity")),
                        "%" + activity.toLowerCase() + "%"));
            }
            if (StringUtils.hasText(category)) {
                searchPredicates.add(cb.like(cb.lower(root.get("category")),
                        "%" + category.toLowerCase() + "%"));
            }

            if (!isAdminOrManager) {
                predicates.add(cb.isTrue(root.get("available")));
            }

            if (!searchPredicates.isEmpty()) {
                predicates.add(cb.or(searchPredicates.toArray(new Predicate[0])));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return tripRepository.findAll(spec, pageable)
                .map(trip -> convertToDTO(trip, isAdminOrManager));
    }


    public TripDTO createTrip(TripCreateRequest request, MultipartFile image,
                              Authentication authentication) throws IOException {
        checkDuplicateTripName(request.getTitle());

        User currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Trip trip = new Trip();
        trip.setTitle(request.getTitle());
        trip.setAddress(request.getDestination());
        trip.setDescription(request.getDescription());
        trip.setPrice(request.getPrice());
        trip.setDuration(request.getDuration());
        trip.setLocationLink(request.getLocationLink());
        trip.setTips(request.getTips());
        trip.setStartTime(request.getStartTime());
        trip.setEndTime(request.getEndTime());
        trip.setActivity(request.getActivity());
        trip.setCreatedBy(currentUser);
        trip.setAvailable(true);

        if (image != null && !image.isEmpty()) {
            String imageUrl = imageService.saveImage(image);
            trip.setImage(imageUrl);
        }

        Trip savedTrip = tripRepository.save(trip);
        return convertToDetailedDTO(savedTrip);
    }

    public TripDTO updateTrip(Long tripId, TripUpdateRequest request,
                              MultipartFile image, Authentication authentication) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));

        User currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(role -> role.getRoleType() == Role.RoleType.ROLE_ADMIN);
        boolean isCreator = trip.getCreatedBy().getId().equals(currentUser.getId());

        if (!isAdmin && !isCreator) {
            try {
                throw new AccessDeniedException("You don't have permission to update this trip");
            } catch (AccessDeniedException e) {
                throw new RuntimeException(e);
            }
        }

        // Check for duplicate name only if name is being changed
        if (request.getTitle() != null && !request.getTitle().equals(trip.getTitle())) {
            checkDuplicateTripName(request.getTitle());
        }

        if (request.getTitle() != null) trip.setTitle(request.getTitle());
        if (request.getDestination() != null) trip.setAddress(request.getDestination());
        if (request.getDescription() != null) trip.setDescription(request.getDescription());
        if (request.getPrice() != null) trip.setPrice(request.getPrice());
        if (request.getDuration() != null) trip.setDuration(request.getDuration());
        if (request.getLocationLink() != null) trip.setLocationLink(request.getLocationLink());
        if (request.getTips() != null) trip.setTips(request.getTips());
        if (request.getStartTime() != null) trip.setStartTime(request.getStartTime());
        if (request.getEndTime() != null) trip.setEndTime(request.getEndTime());
        if (request.getActivity() != null) trip.setActivity(request.getActivity());

        try {
            if (image != null && !image.isEmpty()) {
                String imageUrl = imageService.saveImage(image);
                trip.setImage(imageUrl);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to update image", e);
        }

        Trip updatedTrip = tripRepository.save(trip);
        return convertToDetailedDTO(updatedTrip);
    }

    public void deleteTrip(Long tripId, Authentication authentication) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));

        User currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(role -> role.getRoleType() == Role.RoleType.ROLE_ADMIN);
        boolean isCreator = trip.getCreatedBy().getId().equals(currentUser.getId());

        if (!isAdmin && !isCreator) {
            try {
                throw new AccessDeniedException("You don't have permission to delete this trip");
            } catch (AccessDeniedException e) {
                throw new RuntimeException(e);
            }
        }

        tripRepository.delete(trip);
    }

    public TripDTO getTripById(Long tripId, Authentication authentication) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));

        User currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isAdminOrManager = currentUser.getRoles().stream()
                .anyMatch(role -> role.getRoleType() == Role.RoleType.ROLE_ADMIN ||
                        role.getRoleType() == Role.RoleType.ROLE_MANAGER);

        if (!isAdminOrManager && !trip.isAvailable()) {
            try {
                throw new AccessDeniedException("This trip is not available");
            } catch (AccessDeniedException e) {
                throw new RuntimeException(e);
            }
        }

        return convertToDTO(trip, isAdminOrManager);
    }

    private TripDTO convertToDTO(Trip trip, boolean isAdminOrManager) {
        return isAdminOrManager ? convertToDetailedDTO(trip) : convertToBasicDTO(trip);
    }

    private TripBasicDTO convertToBasicDTO(Trip trip) {
        return TripBasicDTO.builder()
                .id(trip.getId())
                .title(trip.getTitle())
                .destination(trip.getAddress())
                .description(trip.getDescription())
                .price(trip.getPrice())
                .duration(trip.getDuration())
                .locationLink(trip.getLocationLink())
                .tips(trip.getTips())
                .startTime(trip.getStartTime())
                .endTime(trip.getEndTime())
                .activity(trip.getActivity())
                .latitude(trip.getLatitude())
                .longitude(trip.getLongitude())
                .imageUrl(trip.getImage())
                .available(trip.isAvailable())
                .build();
    }
    private TripDetailedDTO convertToDetailedDTO(Trip trip) {
        return TripDetailedDTO.builder()
                .id(trip.getId())
                .title(trip.getTitle())
                .destination(trip.getAddress())
                .description(trip.getDescription())
                .price(trip.getPrice())
                .duration(trip.getDuration())
                .locationLink(trip.getLocationLink())
                .tips(trip.getTips())
                .startTime(trip.getStartTime())
                .endTime(trip.getEndTime())
                .activity(trip.getActivity())
                .latitude(trip.getLatitude())
                .longitude(trip.getLongitude())
                .imageUrl(trip.getImage())
                .available(trip.isAvailable())
                .createdAt(trip.getCreatedAt())
                .createdBy(UserSummaryDTO.builder()
                        .id(trip.getCreatedBy().getId())
                        .firstName(trip.getCreatedBy().getFirstName())
                        .lastName(trip.getCreatedBy().getLastName())
                        .email(trip.getCreatedBy().getEmail())
                        .build())
                .build();
    }
    private void checkDuplicateTripName(String title) {
        if (tripRepository.existsByTitleIgnoreCase(title)) {
            throw new DuplicateResourceException("Trip with name '" + title + "' already exists");
        }
    }
}
