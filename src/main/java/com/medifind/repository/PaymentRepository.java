package com.medifind.repository;

import com.medifind.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * SLP: Core Platform & Shared Engine (Payment structure, MVP scope)
 */
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByReservationId(Long reservationId);
}
