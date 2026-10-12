package com.medifind.service;

import com.medifind.entity.AuditLog;
import com.medifind.entity.User;
import com.medifind.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SLP: Admin Oversight & Complaint Handling → "Confirmation & audit
 * logging for critical admin actions"
 * SLP: Platform Security & Compliance → "Platform-wide audit logging"
 *
 * A thin, single-purpose service: every other admin-facing service calls
 * {@link #record} right after it performs a sensitive action, so all
 * audit-trail writing goes through one code path.
 */
@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void record(User admin, String action, String oldValue, String newValue) {
        AuditLog entry = new AuditLog();
        entry.setAdmin(admin);
        entry.setAction(action);
        entry.setOldValue(oldValue);
        entry.setNewValue(newValue);
        auditLogRepository.save(entry);
    }

    public Page<AuditLog> findAll(Pageable pageable) {
        return auditLogRepository.findAllByOrderByTimestampDesc(pageable);
    }
}

