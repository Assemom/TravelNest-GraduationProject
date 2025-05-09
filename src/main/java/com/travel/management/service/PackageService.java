package com.travel.management.service;

import com.travel.management.dto.*;
import com.travel.management.exception.DuplicateResourceException;
import com.travel.management.exception.PackageNotFoundException;
import com.travel.management.exception.PackageValidationException;
import com.travel.management.exception.ResourceNotFoundException;
import com.travel.management.model.Package;
import com.travel.management.repository.PackageRepository;
import com.travel.management.repository.TripRepository;
import com.travel.management.repository.UserRepository;
import jakarta.xml.bind.ValidationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.travel.management.model.Role;
import com.travel.management.model.Trip;
import com.travel.management.model.User;
import org.springframework.data.domain.Page;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.nio.file.AccessDeniedException;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class PackageService {
    private final PackageRepository packageRepository;
    private final TripRepository tripRepository;
    private final UserRepository userRepository;

    public PackageService(PackageRepository packageRepository,
                          TripRepository tripRepository,
                          UserRepository userRepository) {
        this.packageRepository = packageRepository;
        this.tripRepository = tripRepository;
        this.userRepository = userRepository;
    }
    public Page<PackageDTO> getAllPackages(Pageable pageable, Authentication authentication) {
        User currentUser = getUserFromAuthentication(authentication);
        boolean isAdminOrManager = isAdminOrManager(currentUser);

        Specification<Package> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!isAdminOrManager) {
                predicates.add(cb.equal(root.get("status"), Package.PackageStatus.PUBLIC));
            }
            return predicates.isEmpty() ? null : cb.and(predicates.toArray(new Predicate[0]));
        };

        return packageRepository.findAll(spec, pageable)
                .map(pack -> convertToDTO(pack, isAdminOrManager));
    }
    public Page<PackageDTO> searchPackages(String name, String category,
                                           Authentication authentication,
                                           Pageable pageable) {
        User currentUser = getUserFromAuthentication(authentication);
        boolean isAdminOrManager = isAdminOrManager(currentUser);

        Specification<Package> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            List<Predicate> searchPredicates = new ArrayList<>();

            // Search criteria
            if (StringUtils.hasText(name)) {
                searchPredicates.add(cb.like(cb.lower(root.get("name")),
                        "%" + name.toLowerCase() + "%"));
            }
            if (StringUtils.hasText(category)) {
                searchPredicates.add(cb.like(cb.lower(root.get("category")),
                        "%" + category.toLowerCase() + "%"));
            }

            // Role-based visibility
            if (!isAdminOrManager) {
                predicates.add(cb.equal(root.get("status"), Package.PackageStatus.PUBLIC));
            }

            // Combine search predicates with OR logic
            if (!searchPredicates.isEmpty()) {
                predicates.add(cb.or(searchPredicates.toArray(new Predicate[0])));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return packageRepository.findAll(spec, pageable)
                .map(pack -> convertToDTO(pack, isAdminOrManager));
    }

    public PackageDTO createPackage(PackageCreateRequest request, Authentication authentication) {
        User currentUser = getUserFromAuthentication(authentication);
        validateUserPermission(currentUser);
        checkDuplicatePackageName(request.getName());
        validateTrips(request.getTripIds());

        Set<Trip> trips = new HashSet<>(tripRepository.findAllById(request.getTripIds()));

        Package newPackage = new Package();
        newPackage.setName(request.getName());
        newPackage.setDescription(request.getDescription());
        newPackage.setPrice(request.getPrice());
        newPackage.setCategory(request.getCategory());
        newPackage.setStatus(request.getStatus());
        newPackage.setCreatedBy(currentUser);
        newPackage.setTrips(trips);
        newPackage.setTotalDuration(calculateTotalDuration(trips));

        Package savedPackage = packageRepository.save(newPackage);
        return convertToDetailedDTO(savedPackage);
    }

    public PackageDTO updatePackage(Long packageId, PackageUpdateRequest request,
                                    Authentication authentication) {
        Package existingPackage = packageRepository.findById(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found"));

        User currentUser = getUserFromAuthentication(authentication);
        validateUpdatePermission(currentUser, existingPackage);

        if (request.getName() != null && !request.getName().equals(existingPackage.getName())) {
            checkDuplicatePackageName(request.getName());
        }

        if (request.getStatus() != null &&
                request.getStatus() != existingPackage.getStatus()) {
            validateStatusChange(existingPackage, request.getStatus());
        }

        if (request.getTripIds() != null) {
            validateTrips(request.getTripIds());
            Set<Trip> newTrips = new HashSet<>(tripRepository.findAllById(request.getTripIds()));
            existingPackage.setTrips(newTrips);
            existingPackage.setTotalDuration(calculateTotalDuration(newTrips));
        }

        updatePackageFields(existingPackage, request);

        Package updatedPackage = packageRepository.save(existingPackage);
        return convertToDetailedDTO(updatedPackage);
    }

    public void deletePackage(Long packageId, Authentication authentication) {
        Package packageToDelete = packageRepository.findById(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found"));

        User currentUser = getUserFromAuthentication(authentication);
        validateDeletePermission(currentUser, packageToDelete);

        packageRepository.delete(packageToDelete);
    }

    public PackageDTO getPackageById(Long packageId, Authentication authentication) {
        Package newpackage = packageRepository.findById(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found"));

        User currentUser = getUserFromAuthentication(authentication);
        boolean isAdminOrManager = isAdminOrManager(currentUser);

        if (!isAdminOrManager && newpackage.getStatus() != Package.PackageStatus.PUBLIC) {
            try {
                throw new AccessDeniedException("This package is not available");
            } catch (AccessDeniedException e) {
                throw new RuntimeException(e);
            }
        }

        return convertToDTO(newpackage, isAdminOrManager);
    }

    private void checkDuplicatePackageName(String name) {
        if (packageRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Package with name '" + name + "' already exists");
        }
    }

    private void validateTrips(Set<Long> tripIds) {
        if (tripIds == null || tripIds.isEmpty()) {
            try {
                throw new ValidationException("Package must contain at least one trip");
            } catch (ValidationException e) {
                throw new RuntimeException(e);
            }
        }

        Set<Trip> trips = new HashSet<>(tripRepository.findAllById(tripIds));
        if (trips.size() != tripIds.size()) {
            try {
                throw new ValidationException("Some trip IDs are invalid");
            } catch (ValidationException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private String calculateTotalDuration(Set<Trip> trips) {
        // Extract numbers from duration strings and sum them
        int totalDays = trips.stream()
                .map(Trip::getDuration)
                .map(this::extractDaysFromDuration)
                .mapToInt(Integer::intValue)
                .sum();

        return formatDuration(totalDays);
    }

    private Integer extractDaysFromDuration(String duration) {
        try {
            // Remove non-digits and parse the number
            String numberOnly = duration.replaceAll("[^0-9]", "");
            return Integer.parseInt(numberOnly);
        } catch (NumberFormatException | NullPointerException e) {
            try {
                throw new ValidationException("Invalid duration format in trip: " + duration);
            } catch (ValidationException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private String formatDuration(int days) {
        if (days == 1) {
            return "1 day";
        }
        return days + " days";
    }

    private void validateStatusChange(Package existingPackage, Package.PackageStatus newStatus) {
        // Add any specific status change validation rules here
        if (newStatus == null) {
            try {
                throw new ValidationException("Package status cannot be null");
            } catch (ValidationException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void validateUserPermission(User user) {
        if (!isAdminOrManager(user)) {
            try {
                throw new AccessDeniedException("Only ADMIN and MANAGER can manage packages");
            } catch (AccessDeniedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void validateUpdatePermission(User user, Package newpackage) {
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getRoleType() == Role.RoleType.ROLE_ADMIN);
        boolean isCreator = newpackage.getCreatedBy().getId().equals(user.getId());

        if (!isAdmin && !isCreator) {
            try {
                throw new AccessDeniedException("You don't have permission to update this package");
            } catch (AccessDeniedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void validateDeletePermission(User user, Package newpackage) {
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getRoleType() == Role.RoleType.ROLE_ADMIN);
        boolean isCreator = newpackage.getCreatedBy().getId().equals(user.getId());

        if (!isAdmin && !isCreator) {
            try {
                throw new AccessDeniedException("You don't have permission to delete this package");
            } catch (AccessDeniedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private boolean isAdminOrManager(User user) {
        return user.getRoles().stream()
                .anyMatch(role -> role.getRoleType() == Role.RoleType.ROLE_ADMIN ||
                        role.getRoleType() == Role.RoleType.ROLE_MANAGER);
    }

    private User getUserFromAuthentication(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private void updatePackageFields(Package existingPackage, PackageUpdateRequest request) {
        if (request.getName() != null) existingPackage.setName(request.getName());
        if (request.getDescription() != null) existingPackage.setDescription(request.getDescription());
        if (request.getPrice() != null) existingPackage.setPrice(request.getPrice());
        if (request.getCategory() != null) existingPackage.setCategory(request.getCategory());
        if (request.getStatus() != null) existingPackage.setStatus(request.getStatus());
    }

    private PackageDTO convertToDTO(Package newpackage, boolean isAdminOrManager) {
        return isAdminOrManager ? convertToDetailedDTO(newpackage) : convertToBasicDTO(newpackage);
    }
    private PackageBasicDTO convertToBasicDTO(Package newpackage) {
        return PackageBasicDTO.builder()
                .id(newpackage.getId())
            .name(newpackage.getName())
            .description(newpackage.getDescription())
            .price(newpackage.getPrice())
            .category(newpackage.getCategory())
            .totalDuration(newpackage.getTotalDuration())
            .status(newpackage.getStatus())
            .trips(convertTripsToBasicDTO(newpackage.getTrips()))
            .build();
    }

    private PackageDetailedDTO convertToDetailedDTO(Package newpackage) {
        return PackageDetailedDTO.builder()
                .id(newpackage.getId())
            .name(newpackage.getName())
            .description(newpackage.getDescription())
            .price(newpackage.getPrice())
            .category(newpackage.getCategory())
            .totalDuration(newpackage.getTotalDuration())
            .status(newpackage.getStatus())
            .trips(convertTripsToBasicDTO(newpackage.getTrips()))
            .createdAt(newpackage.getCreatedAt())
            .createdBy(UserSummaryDTO.builder()
                .id(newpackage.getCreatedBy().getId())
                    .firstName(newpackage.getCreatedBy().getFirstName())
                    .lastName(newpackage.getCreatedBy().getLastName())
                    .email(newpackage.getCreatedBy().getEmail())
                    .build())
            .build();
    }

    private Set<TripBasicDTO> convertTripsToBasicDTO(Set<Trip> trips) {
        return trips.stream()
                .map(trip -> TripBasicDTO.builder()
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
                        .imageUrl(trip.getImage())
                        .available(trip.isAvailable())
                        .build())
                .collect(Collectors.toSet());
    }
    public boolean isPackageAvailableForBooking(Long packageId) {
        Package newpackage = packageRepository.findById(packageId)
                .orElseThrow(() -> new PackageNotFoundException(packageId));

        // Check if package is public and all trips are available
        return newpackage.getStatus() == Package.PackageStatus.PUBLIC &&
                newpackage.getTrips().stream().allMatch(Trip::isAvailable);
    }
    public void validatePackageForBooking(Long packageId) {
        if (!isPackageAvailableForBooking(packageId)) {
            throw new PackageValidationException("Package is not available for booking");
        }
    }
    public List<PackageDTO> getRelatedPackages(Long packageId, Authentication authentication) {
        Package newpackage = packageRepository.findById(packageId)
                .orElseThrow(() -> new PackageNotFoundException(packageId));

        User currentUser = getUserFromAuthentication(authentication);
        boolean isAdminOrManager = isAdminOrManager(currentUser);

        // Find packages with similar category or overlapping trips
        Specification<Package> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.notEqual(root.get("id"), packageId));
            predicates.add(cb.equal(root.get("category"), newpackage.getCategory()));

            if (!isAdminOrManager) {
                predicates.add(cb.equal(root.get("status"), Package.PackageStatus.PUBLIC));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return packageRepository.findAll(spec)
                .stream()
                .limit(5)  // Limit to 5 related packages
                .map(p -> convertToDTO(p, isAdminOrManager))
                .collect(Collectors.toList());
    }
}
