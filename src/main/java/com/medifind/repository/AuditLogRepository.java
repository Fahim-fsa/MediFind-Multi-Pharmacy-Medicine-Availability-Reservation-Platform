package com.medifind.repository;

import com.medifind.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * SLP: Platform Security & Compliance → "Platform-wide audit logging"
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * {@code LEFT JOIN FETCH a.admin} loads the acting admin alongside
     * each entry instead of leaving it as an un-fetched lazy proxy — the
     * audit log page prints {@code e.admin.fullName} in Thymeleaf, which
     * runs after this method (and its Hibernate session) has already
     * returned, since open-in-view is off. LEFT (not INNER) so a row
     * would still render even if its admin were ever missing. An
     * explicit countQuery avoids Spring Data trying to derive one from
     * the fetch join itself.
     */
    @Query(value = "SELECT a FROM AuditLog a LEFT JOIN FETCH a.admin ORDER BY a.timestamp DESC",
            countQuery = "SELECT COUNT(a) FROM AuditLog a")
    Page<AuditLog> findAllByOrderByTimestampDesc(Pageable pageable);
}
