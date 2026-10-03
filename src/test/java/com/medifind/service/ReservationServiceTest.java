package com.medifind.service;

import com.medifind.dto.ReservationRequest;
import com.medifind.entity.*;
import com.medifind.enums.*;
import com.medifind.exception.BusinessRuleException;
import com.medifind.repository.InventoryRepository;
import com.medifind.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * SLP: Development Style → "Testing" (step 10 of the requirement brief)
 */
@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock private ReservationRepository reservationRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private PaymentService paymentService;
    @Mock private NotificationService notificationService;
    @Mock private SystemSettingService systemSettingService;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private ReservationService reservationService;

    private User patient;
    private Pharmacy pharmacy;
    private Medicine medicine;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        patient = new User();
        patient.setId(1L);
        patient.setFullName("Test Patient");
        patient.setPhone("+8801700000000");

        pharmacy = new Pharmacy();
        pharmacy.setId(10L);
        pharmacy.setPharmacyName("Test Pharmacy");
        pharmacy.setVerificationStatus(VerificationStatus.APPROVED);

        medicine = new Medicine();
        medicine.setId(20L);
        medicine.setName("Napa");

        inventory = new Inventory();
        inventory.setId(30L);
        inventory.setPharmacy(pharmacy);
        inventory.setMedicine(medicine);
        inventory.setQuantity(10);
        inventory.setPrice(new BigDecimal("3.50"));
        inventory.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
    }

    @Test
    void reserve_holdsStockImmediatelyAndCreatesReservation() {
        ReservationRequest request = new ReservationRequest();
        request.setPharmacyId(10L);
        request.setMedicineId(20L);
        request.setQuantity(3);
        request.setPaymentMethod(PaymentMethod.CASH_ON_PICKUP);

        when(inventoryRepository.findByPharmacyIdAndMedicineId(10L, 20L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(inv -> inv.getArgument(0));
        when(systemSettingService.getInt(eq("reservation.pickup.window.hours"), anyInt())).thenReturn(24);
        when(reservationRepository.findByConfirmationCode(any())).thenReturn(Optional.empty());
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        Reservation result = reservationService.reserve(patient, request);

        // Stock is held the moment the reservation is placed — see
        // ReservationService's class Javadoc for why.
        assertThat(inventory.getQuantity()).isEqualTo(7);
        assertThat(result.getStatus()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(result.getQuantity()).isEqualTo(3);
        assertThat(result.getConfirmationCode()).isNotBlank();
        verify(paymentService).createForReservation(eq(result), eq(PaymentMethod.CASH_ON_PICKUP), eq(new BigDecimal("10.50")));
        verify(notificationService).notifyReservationConfirmation(result);
        verify(notificationService).notifyNewReservationToPharmacyStaff(result);
    }

    @Test
    void reserve_rejectsWhenNotEnoughStock() {
        inventory.setQuantity(2);
        ReservationRequest request = new ReservationRequest();
        request.setPharmacyId(10L);
        request.setMedicineId(20L);
        request.setQuantity(5);
        request.setPaymentMethod(PaymentMethod.CASH_ON_PICKUP);

        when(inventoryRepository.findByPharmacyIdAndMedicineId(10L, 20L)).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> reservationService.reserve(patient, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Only 2 unit(s) left");

        verify(inventoryRepository, never()).save(any());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void reserve_rejectsWhenPharmacyNotApproved() {
        pharmacy.setVerificationStatus(VerificationStatus.PENDING);
        ReservationRequest request = new ReservationRequest();
        request.setPharmacyId(10L);
        request.setMedicineId(20L);
        request.setQuantity(1);
        request.setPaymentMethod(PaymentMethod.CASH_ON_PICKUP);

        when(inventoryRepository.findByPharmacyIdAndMedicineId(10L, 20L)).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> reservationService.reserve(patient, request))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void cancel_restoresStockWhenOwnerCancels() {
        Reservation reservation = new Reservation();
        reservation.setId(99L);
        reservation.setPatient(patient);
        reservation.setPharmacy(pharmacy);
        reservation.setMedicine(medicine);
        reservation.setQuantity(4);
        reservation.setStatus(ReservationStatus.RESERVED);
        reservation.setPickupDeadline(LocalDateTime.now().plusHours(24));

        when(reservationRepository.findById(99L)).thenReturn(Optional.of(reservation));
        when(inventoryRepository.findByPharmacyIdAndMedicineId(10L, 20L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        Reservation cancelled = reservationService.cancel(99L, 1L);

        assertThat(cancelled.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(inventory.getQuantity()).isEqualTo(14); // 10 (setUp) + 4 restored
        verify(paymentService).markCancelledPaymentFailed(99L);
    }

    @Test
    void cancel_rejectsWhenNotTheOwningPatient() {
        Reservation reservation = new Reservation();
        reservation.setId(99L);
        reservation.setPatient(patient); // patient.getId() == 1L
        reservation.setStatus(ReservationStatus.RESERVED);

        when(reservationRepository.findById(99L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.cancel(99L, 999L))
                .isInstanceOf(AccessDeniedException.class);
    }
}
