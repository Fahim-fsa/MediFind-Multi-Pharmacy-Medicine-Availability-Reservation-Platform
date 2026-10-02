package com.medifind.service;

import com.medifind.entity.Payment;
import com.medifind.entity.Reservation;
import com.medifind.enums.PaymentMethod;
import com.medifind.enums.PaymentStatus;
import com.medifind.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * SLP: Core Platform & Shared Engine (Payment structure, MVP scope)
 */
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public Payment createForReservation(Reservation reservation, PaymentMethod method, BigDecimal amount) {
        Payment payment = new Payment();
        payment.setReservation(reservation);
        payment.setAmount(amount);
        payment.setPaymentMethod(method);
        payment.setPaymentStatus(method == PaymentMethod.ONLINE ? PaymentStatus.COMPLETED : PaymentStatus.PENDING);
        return paymentRepository.save(payment);
    }

    public Optional<Payment> findByReservationId(Long reservationId) {
        return paymentRepository.findByReservationId(reservationId);
    }

    @Transactional
    public void markCollectedPaymentComplete(Long reservationId) {
        paymentRepository.findByReservationId(reservationId).ifPresent(payment -> {
            if (payment.getPaymentStatus() != PaymentStatus.COMPLETED) {
                payment.setPaymentStatus(PaymentStatus.COMPLETED);
                paymentRepository.save(payment);
            }
        });
    }

    @Transactional
    public void markCancelledPaymentFailed(Long reservationId) {
        paymentRepository.findByReservationId(reservationId).ifPresent(payment -> {
            if (payment.getPaymentStatus() == PaymentStatus.PENDING) {
                payment.setPaymentStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
            }
        });
    }
}
