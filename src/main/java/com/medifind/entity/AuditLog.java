package com.medifind.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * SLP: Admin Oversight & Complaint Handling → "Confirmation & audit
 * logging for critical admin actions"
 * SLP: Platform Security & Compliance → "Platform-wide audit logging"
 *
 * Matches the requirement brief's "AuditLog: id, adminId, action,
 * timestamp" entity, extended with {@link #oldValue}/{@link #newValue} so
 * a reviewer can see exactly what changed (the "Log configuration changes
 * with before/after values" story), not just that *something* changed.
 * Written by AuditLogService whenever an admin approves/rejects a
 * pharmacy, suspends a user, cancels a disputed reservation, removes a
 * listing, or edits a system setting.
 */
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_admin", columnList = "admin_id"),
        @Index(name = "idx_audit_timestamp", columnList = "timestamp")
})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private User admin;

    @Column(nullable = false, length = 150)
    private String action;

    @Lob
    @Column(name = "old_value")
    private String oldValue;

    @Lob
    @Column(name = "new_value")
    private String newValue;

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        this.timestamp = LocalDateTime.now();
    }

    public AuditLog() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getAdmin() {
        return admin;
    }

    public void setAdmin(User admin) {
        this.admin = admin;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuditLog)) return false;
        AuditLog other = (AuditLog) o;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

