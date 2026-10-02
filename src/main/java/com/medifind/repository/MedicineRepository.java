package com.medifind.repository;

import com.medifind.entity.Medicine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * SLP: Medicine Search & Pharmacy Discovery → "Search medicine by name,
 * generic name or brand"
 */
public interface MedicineRepository extends JpaRepository<Medicine, Long> {

    boolean existsByNameIgnoreCaseAndBrandIgnoreCase(String name, String brand);

    /**
     * Case-insensitive match against name, generic name, or brand — the
     * three fields the "Search medicine by name, generic name or brand"
     * story asks for.
     */
    @Query("SELECT m FROM Medicine m WHERE " +
            "LOWER(m.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(m.genericName) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(m.brand) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Medicine> searchByNameOrGenericOrBrand(@Param("query") String query);

    /** SLP: Admin Reporting & Configuration → "Generate platform-wide reports" */
    List<Medicine> findTop10ByOrderBySearchCountDesc();

    Page<Medicine> findAllByOrderByNameAsc(Pageable pageable);
}

