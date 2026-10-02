package com.medifind.service;

import com.medifind.dto.ReservationQueueItem;
import com.medifind.dto.ReservationRequest;
import com.medifind.entity.Inventory;
import com.medifind.entity.Reservation;
import com.medifind.entity.User;
import com.medifind.enums.ReservationStatus;
import com.medifind.enums.VerificationStatus;
import com.medifind.exception.BusinessRuleException;
import com.medifind.exception.ResourceNotFoundException;
import com.medifind.repository.InventoryRepository;
import com.medifind.repository.ReservationRepository;
import com.medifind.util.CodeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SLP: Core Platform & Shared Engine → "Implement Reservation Ledger
 * core service"
 * SLP: Patient Reservation Management / Pharmacy Reservation Fulfilment
 */
@Service
public class ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);

    private final ReservationRepository reservationRepository;
    private final InventoryRepository inventoryRepository;
    private final PaymentService paymentService;
    private final NotificationService notificationService;
    private final SystemSettingService systemSettingService;
    private final AuditLogService auditLogService;

    public ReservationService(ReservationRepository reservationRepository, InventoryRepository inventoryRepository,
                              PaymentService paymentService, NotificationService notificationService,
                              SystemSettingService systemSettingService, AuditLogService auditLogService) {
        this.reservationRepository = reservationRepository;
        this.inventoryRepository = inventoryRepository;
        this.paymentService = paymentService;
        this.notificationService = notificationService;
        this.systemSettingService = systemSettingService;
        this.auditLogService = auditLogService;
    }

    public Reservation getById(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + id));
    }

    /**
     * SLP: Patient Reservation Management → "Reserve medicine at a
     * chosen pharmacy", "Generate & display reservation confirmation
     * code", "Real-time listing status update to Reserved"
     */
    @Transactional
    public Reservation reserve(User patient, ReservationRequest request) {
        Inventory inventory = inventoryRepository.findByPharmacyIdAndMedicineId(request.getPharmacyId(), request.getMedicineId())
                .orElseThrow(() -> new ResourceNotFoundException("That medicine is not listed at this pharmacy."));

        if (inventory.getPharmacy().getVerificationStatus() != VerificationStatus.APPROVED) {
            throw new BusinessRuleException("This pharmacy is not currently accepting reservations.");
        }
        if (!inventory.isPurchasable()) {
            throw new BusinessRuleException("This medicine is currently unavailable at this pharmacy.");
        }
        if (inventory.getQuantity() < request.getQuantity()) {
            throw new BusinessRuleException("Only " + inventory.getQuantity() + " unit(s) left — reduce the quantity and try again.");
        }

        // Hold the stock immediately — see class Javadoc above for why.
        inventory.setQuantity(inventory.getQuantity() - request.getQuantity());
        inventoryRepository.save(inventory);

        Reservation reservation = new Reservation();
        reservation.setPatient(patient);
        reservation.setPharmacy(inventory.getPharmacy());
        reservation.setMedicine(inventory.getMedicine());
        reservation.setQuantity(request.getQuantity());
        reservation.setConfirmationCode(generateUniqueConfirmationCode());
        reservation.setStatus(ReservationStatus.RESERVED);

        int windowHours = systemSettingService.getInt("reservation.pickup.window.hours", 24);
        reservation.setPickupDeadline(LocalDateTime.now().plusHours(windowHours));

        reservation = reservationRepository.save(reservation);

        var unitPrice = inventory.getPrice();
        paymentService.createForReservation(reservation, request.getPaymentMethod(),
                unitPrice.multiply(java.math.BigDecimal.valueOf(request.getQuantity())));

        notificationService.notifyReservationConfirmation(reservation);
        notificationService.notifyNewReservationToPharmacyStaff(reservation);

        return reservation;
    }

    /** SLP: Patient Reservation Management → "Cancel reservation before pickup window expires" */
    @Transactional
    public Reservation cancel(Long reservationId, Long requestingPatientId) {
        Reservation reservation = getById(reservationId);
        if (!reservation.getPatient().getId().equals(requestingPatientId)) {
            throw new AccessDeniedException("This is not your reservation.");
        }
        return doCancel(reservation, "cancelled by patient");
    }

    /**
     * SLP: Pharmacy Reservation Fulfilment (pharmacist-side counterpart
     * to "Mark reservation as Collected"
     */
    @Transactional
    public Reservation pharmacistCancel(Long reservationId, Long requestingPharmacyId) {
        Reservation reservation = getById(reservationId);
        if (!reservation.getPharmacy().getId().equals(requestingPharmacyId)) {
            throw new AccessDeniedException("This reservation belongs to a different pharmacy.");
        }
        return doCancel(reservation, "cancelled by the pharmacy");
    }

    /** SLP: Admin Oversight & Complaint Handling → "View and cancel a disputed reservation" */
    @Transactional
    public Reservation adminCancel(Long reservationId, User admin, String reason) {
        Reservation reservation = getById(reservationId);
        Reservation cancelled = doCancel(reservation, "cancelled by admin" + (reason != null && !reason.isBlank() ? ": " + reason : ""));
        auditLogService.record(admin, "CANCEL_RESERVATION: " + cancelled.getConfirmationCode(),
                "RESERVED", "CANCELLED (" + reason + ")");
        return cancelled;
    }

    private Reservation doCancel(Reservation reservation, String reasonForNotification) {
        if (reservation.getStatus() != ReservationStatus.RESERVED) {
            throw new BusinessRuleException("Only an active reservation can be cancelled.");
        }
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancelledAt(LocalDateTime.now());
        restoreStock(reservation);
        reservation = reservationRepository.save(reservation);
        paymentService.markCancelledPaymentFailed(reservation.getId());
        notificationService.notifyReservationStatusUpdate(reservation, reasonForNotification);
        return reservation;
    }

    /**
     * SLP: Pharmacy Reservation Fulfilment → "Mark reservation as Collected"
     */
    @Transactional
    public Reservation markCollected(Long reservationId, Long requestingPharmacyId) {
        Reservation reservation = getById(reservationId);
        if (!reservation.getPharmacy().getId().equals(requestingPharmacyId)) {
            throw new AccessDeniedException("This reservation belongs to a different pharmacy.");
        }
        if (reservation.getStatus() != ReservationStatus.RESERVED) {
            throw new BusinessRuleException("Only an active reservation can be marked as collected.");
        }
        reservation.setStatus(ReservationStatus.COLLECTED);
        reservation.setCollectedAt(LocalDateTime.now());
        reservation = reservationRepository.save(reservation);

        paymentService.markCollectedPaymentComplete(reservation.getId());
        notificationService.notifyReservationStatusUpdate(reservation, "collected — thank you!");
        return reservation;
    }

    /** SLP: Patient Reservation Management → "Reservation history with date/status filters" */
    public Page<Reservation> historyForPatient(Long patientId, ReservationStatus status,
                                               LocalDateTime from, LocalDateTime to, Pageable pageable) {
        return reservationRepository.findHistoryForPatient(patientId, status, from, to, pageable);
    }

    /**
     * SLP: Pharmacy Reservation Fulfilment → "View incoming reservations
     * in real time", "Reservation queue in chronological order"
     * SLP: Platform Security & Compliance → "Limit patient data exposed
     * during reservation" — see {@link #toQueueItem}.
     */
    @Transactional(readOnly = true)
    public List<ReservationQueueItem> incomingQueueForPharmacy(Long pharmacyId) {
        return reservationRepository.findByPharmacyIdAndStatusOrderByReservedAtAsc(pharmacyId, ReservationStatus.RESERVED)
                .stream().map(this::toQueueItem).toList();
    }

    /** SLP: Admin Oversight & Complaint Handling → "View and cancel a disputed reservation" */
    public Page<Reservation> adminSearch(ReservationStatus status, Pageable pageable) {
        return reservationRepository.searchForAdmin(status, pageable);
    }

    /**
     * SLP: Patient Reservation Management → "Auto-expire reservations
     * past the pickup window"
     */
    @Scheduled(fixedDelayString = "${medifind.reservation.expiry-check-interval-ms:900000}")
    @Transactional
    public void expireOverdueReservations() {
        List<Reservation> overdue = reservationRepository.findByStatusAndPickupDeadlineBefore(
                ReservationStatus.RESERVED, LocalDateTime.now());
        if (overdue.isEmpty()) {
            return;
        }
        for (Reservation reservation : overdue) {
            reservation.setStatus(ReservationStatus.EXPIRED);
            restoreStock(reservation);
            reservationRepository.save(reservation);
            paymentService.markCancelledPaymentFailed(reservation.getId());
            notificationService.notifyReservationStatusUpdate(reservation, "expired — the pickup window passed");
        }
        log.info("Auto-expired {} overdue reservation(s).", overdue.size());
    }

    // ------------------------------------------------------------------

    private void restoreStock(Reservation reservation) {
        inventoryRepository.findByPharmacyIdAndMedicineId(reservation.getPharmacy().getId(), reservation.getMedicine().getId())
                .ifPresent(inventory -> {
                    inventory.setQuantity(inventory.getQuantity() + reservation.getQuantity());
                    inventoryRepository.save(inventory);
                });
    }

    private String generateUniqueConfirmationCode() {
        for (int attempt = 0; attempt < 5; attempt++) {
            String candidate = CodeGenerator.reservationConfirmationCode();
            if (reservationRepository.findByConfirmationCode(candidate).isEmpty()) {
                return candidate;
            }
        }
        // Astronomically unlikely with a 32^6 code space, but fail loudly rather than
        // silently risk a duplicate if it ever did happen.
        throw new IllegalStateException("Could not generate a unique confirmation code — please try again.");
    }

    private ReservationQueueItem toQueueItem(Reservation reservation) {
        ReservationQueueItem item = new ReservationQueueItem();
        item.setReservationId(reservation.getId());
        item.setPatientName(reservation.getPatient().getFullName());
        item.setMaskedPhone(maskPhone(reservation.getPatient().getPhone()));
        item.setMedicineName(reservation.getMedicine().getName());
        item.setQuantity(reservation.getQuantity());
        item.setConfirmationCode(reservation.getConfirmationCode());
        item.setStatus(reservation.getStatus());
        item.setReservedAt(reservation.getReservedAt());
        item.setPickupDeadline(reservation.getPickupDeadline());
        return item;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() <= 4) {
            return "****";
        }
        int visible = 4;
        String tail = phone.substring(phone.length() - visible);
        return "*".repeat(phone.length() - visible) + tail;
    }
}