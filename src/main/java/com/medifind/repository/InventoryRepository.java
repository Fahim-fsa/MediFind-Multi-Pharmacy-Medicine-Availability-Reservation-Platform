package com.medifind.repository;

import com.medifind.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * SLP: Core Platform & Shared Engine → "Implement Multi-Pharmacy
 * Inventory Index service"
 * SLP: Pharmacy Inventory & Listing Mgmt
 */
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByPharmacyIdAndMedicineId(Long pharmacyId, Long medicineId);

    /**
     * SLP: Pharmacy Inventory & Listing Mgmt → "Per-pharmacy inventory dashboard"
     */
    @Query("SELECT i FROM Inventory i JOIN FETCH i.medicine WHERE i.pharmacy.id = :pharmacyId " +
            "ORDER BY i.lastUpdated DESC")
    List<Inventory> findByPharmacyIdOrderByLastUpdatedDesc(@Param("pharmacyId") Long pharmacyId);

    /**
     * SLP: Pharmacy Reservation Fulfilment → "Notify staff of stock nearing zero"
     *
     * Same JOIN FETCH reasoning as above — the pharmacist dashboard's
     * low-stock table also prints .
     */
    @Query("SELECT i FROM Inventory i JOIN FETCH i.medicine " +
            "WHERE i.pharmacy.id = :pharmacyId AND i.quantity <= :threshold")
    List<Inventory> findByPharmacyIdAndQuantityLessThanEqual(@Param("pharmacyId") Long pharmacyId,
                                                             @Param("threshold") int threshold);

    /** SLP: Admin Oversight & Complaint Handling → "full CRUD" — removes every listing for a pharmacy being deleted. */
    long deleteByPharmacyId(Long pharmacyId);

    /**
     * SLP: Medicine Search & Pharmacy Discovery → "Display pharmacies
     * reporting availability"
     */
    @Query("SELECT i FROM Inventory i " +
            "WHERE i.medicine.id = :medicineId AND i.pharmacy.verificationStatus = 'APPROVED' " +
            "ORDER BY i.price ASC")
    List<Inventory> findAvailabilityForMedicine(@Param("medicineId") Long medicineId);
}
