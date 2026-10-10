package com.medifind.repository;

import com.medifind.entity.Pharmacy;
import com.medifind.enums.VerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * SLP: Pharmacy Onboarding & Verification
 */
public interface PharmacyRepository extends JpaRepository<Pharmacy, Long> {

    boolean existsByLicenseNumber(String licenseNumber);

    Optional<Pharmacy> findByLicenseNumber(String licenseNumber);

    /** SLP: Pharmacy Onboarding & Verification → "Admin approval workflow before publishing listings" */
    List<Pharmacy> findByVerificationStatusOrderByCreatedAtAsc(VerificationStatus status);

    long countByVerificationStatus(VerificationStatus status);

    /**
     * SLP: Admin Oversight & Complaint Handling → "View all active
     * listings with search and filter"
     */
    @Query("SELECT p FROM Pharmacy p WHERE (:status IS NULL OR p.verificationStatus = :status) " +
            "AND (:search IS NULL OR :search = '' " +
            "     OR LOWER(p.pharmacyName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "     OR LOWER(p.address) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY p.createdAt DESC")
    Page<Pharmacy> search(@Param("status") VerificationStatus status,
                          @Param("search") String search,
                          Pageable pageable);
}

