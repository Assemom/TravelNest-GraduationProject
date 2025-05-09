package com.travel.management.repository;

import com.travel.management.model.Package;
import com.travel.management.model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PackageRepository extends JpaRepository<Package, Long>, JpaSpecificationExecutor<Package> {
    boolean existsByNameIgnoreCase(String name);

    @Query("SELECT COUNT(p) > 0 FROM Package p WHERE p.status = 'PUBLIC' AND p.id = :packageId")
    boolean isPackagePublic(@Param("packageId") Long packageId);

    @Query("SELECT p FROM Package p JOIN p.trips t WHERE t.id = :tripId")
    List<Package> findPackagesByTripId(@Param("tripId") Long tripId);
}
