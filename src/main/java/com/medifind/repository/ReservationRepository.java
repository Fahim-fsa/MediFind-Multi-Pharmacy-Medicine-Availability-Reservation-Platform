package com.medifind.repository;

import com.medifind.entity.Reservation;
import com.medifind.enums.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * SLP: Core Platform & Shared Engine → "Implement Reservation Ledger core
 * service"
 */
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Optional<Reservation> findByConfirmationCode(String confirmationCode);

    /**
     * SLP: Patient Reservation Management → "Reservation history with
     * date/status filters"
     */
    @Query(value = "SELECT r FROM Reservation r JOIN FETCH r.pharmacy JOIN FETCH r.medicine " +
            "WHERE r.patient.id = :patientId " +
            "AND (:status IS NULL OR r.status = :status) " +
            "AND (:from IS NULL OR r.reservedAt >= :from) " +
            "AND (:to IS NULL OR r.reservedAt <= :to) " +
            "ORDER BY r.reservedAt DESC",
            countQuery = "SELECT COUNT(r) FROM Reservation r WHERE r.patient.id = :patientId " +
                    "AND (:status IS NULL OR r.status = :status) " +
                    "AND (:from IS NULL OR r.reservedAt >= :from) " +
                    "AND (:to IS NULL OR r.reservedAt <= :to)")
    Page<Reservation> findHistoryForPatient(@Param("patientId") Long patientId,
                                            @Param("status") ReservationStatus status,
                                            @Param("from") LocalDateTime from,
                                            @Param("to") LocalDateTime to,
                                            Pageable pageable);

    /**
     * SLP: Pharmacy Reservation Fulfilment → "View incoming reservations
     * in real time", "Reservation queue in chronological order"
     */
    List<Reservation> findByPharmacyIdAndStatusOrderByReservedAtAsc(Long pharmacyId, ReservationStatus status);

    /**
     * SLP: Admin Oversight & Complaint Handling → "View and cancel a disputed reservation"
     */
    @Query(value = "SELECT r FROM Reservation r JOIN FETCH r.patient JOIN FETCH r.pharmacy JOIN FETCH r.medicine " +
            "WHERE (:status IS NULL OR r.status = :status) " +
            "ORDER BY r.reservedAt DESC",
            countQuery = "SELECT COUNT(r) FROM Reservation r WHERE (:status IS NULL OR r.status = :status)")
    Page<Reservation> searchForAdmin(@Param("status") ReservationStatus status, Pageable pageable);

    /** SLP: Admin Oversight & Complaint Handling → "full CRUD" — guards pharmacy deletion so reservation history is never silently destroyed. */
    boolean existsByPharmacyId(Long pharmacyId);

    /**
     * SLP: Patient Reservation Management → "Auto-expire reservations
     */
    List<Reservation> findByStatusAndPickupDeadlineBefore(ReservationStatus status, LocalDateTime cutoff);

    long countByStatus(ReservationStatus status);

    /**
     * SLP: Admin Reporting & Configuration → "Generate platform-wide
     */
    @Query(value = "SELECT DATE(reserved_at) AS reservation_date, COUNT(*) AS total " +
            "FROM reservations " +
            "WHERE reserved_at >= :since " +
            "GROUP BY DATE(reserved_at) " +
            "ORDER BY reservation_date ASC", nativeQuery = true)
    List<Object[]> countDailyReservationsSince(@Param("since") LocalDateTime since);

    long countByPharmacyIdAndStatus(Long pharmacyId, ReservationStatus status);
}