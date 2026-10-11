package com.medifind.repository;

import com.medifind.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * SLP: Notifications & 3rd-Party Integrations → "Build shared
 * Notification Service"
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findTop20ByUserIdOrderByCreatedDateDesc(Long userId);

    long countByUserIdAndIsReadFalse(Long userId);

    /**
     * {@code @Modifying} tells Spring Data this query changes data (an
     * UPDATE) rather than returning entities, so it must run inside a
     * transaction — see NotificationService, which is annotated
     * {@code @Transactional}.
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    int markAllAsRead(@Param("userId") Long userId);

    /**
     * SLP: Admin Oversight & Complaint Handling → "full CRUD" — clears a
     * departing pharmacy's staff notifications first, since {@code
     * Notification.user} is a non-nullable foreign key and would
     * otherwise block deleting those staff accounts.
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.user.id IN :userIds")
    void deleteByUserIdIn(@Param("userIds") List<Long> userIds);
}
