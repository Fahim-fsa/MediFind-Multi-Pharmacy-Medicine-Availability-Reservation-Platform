package com.medifind.repository;

import com.medifind.entity.User;
import com.medifind.enums.Role;
import com.medifind.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * SLP: Core Platform & Shared Engine → "Configure JPA/Hibernate data
 * access layer"

 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByResetToken(String resetToken);

    List<User> findByRole(Role role);

    long countByRole(Role role);

    long countByRoleAndStatus(Role role, UserStatus status);

    /**
     * SLP: Admin Account & Verification Mgmt → "View and suspend patient

     */
    @Query("SELECT u FROM User u WHERE u.role = :role " +
            "AND (:status IS NULL OR u.status = :status) " +
            "AND (:search IS NULL OR :search = '' " +
            "     OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "     OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY u.createdAt DESC")
    Page<User> search(@Param("role") Role role,
                      @Param("status") UserStatus status,
                      @Param("search") String search,
                      Pageable pageable);
}