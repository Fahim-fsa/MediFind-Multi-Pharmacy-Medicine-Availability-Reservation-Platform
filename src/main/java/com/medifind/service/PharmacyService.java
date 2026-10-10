package com.medifind.service;

import com.medifind.dto.PharmacyAdminRequest;
import com.medifind.dto.PharmacyProfileUpdateRequest;
import com.medifind.entity.Pharmacy;
import com.medifind.entity.User;
import com.medifind.enums.VerificationStatus;
import com.medifind.exception.BusinessRuleException;
import com.medifind.exception.ResourceNotFoundException;
import com.medifind.repository.InventoryRepository;
import com.medifind.repository.NotificationRepository;
import com.medifind.repository.PharmacyRepository;
import com.medifind.repository.ReservationRepository;
import com.medifind.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * SLP: Pharmacy Onboarding & Verification
 */
@Service
public class PharmacyService {

    private final PharmacyRepository pharmacyRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final ReservationRepository reservationRepository;
    private final InventoryRepository inventoryRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    public PharmacyService(PharmacyRepository pharmacyRepository, NotificationService notificationService,
                           AuditLogService auditLogService, ReservationRepository reservationRepository,
                           InventoryRepository inventoryRepository, UserRepository userRepository,
                           NotificationRepository notificationRepository) {
        this.pharmacyRepository = pharmacyRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
        this.reservationRepository = reservationRepository;
        this.inventoryRepository = inventoryRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
    }

    public Pharmacy getById(Long id) {
        return pharmacyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pharmacy not found: " + id));
    }

    @Transactional
    public Pharmacy updateProfile(Long pharmacyId, PharmacyProfileUpdateRequest request) {
        Pharmacy pharmacy = getById(pharmacyId);
        pharmacy.setPharmacyName(request.getPharmacyName());
        pharmacy.setAddress(request.getAddress());
        pharmacy.setLatitude(request.getLatitude());
        pharmacy.setLongitude(request.getLongitude());
        pharmacy.setPhone(request.getPhone());
        pharmacy.setOpeningHours(request.getOpeningHours());
        return pharmacyRepository.save(pharmacy);
    }

    /** SLP: Pharmacy Onboarding & Verification → "Admin approval workflow before publishing listings" */
    public List<Pharmacy> findPendingVerification() {
        return pharmacyRepository.findByVerificationStatusOrderByCreatedAtAsc(VerificationStatus.PENDING);
    }

    /** SLP: Admin Oversight & Complaint Handling → "View all active listings with search and filter" */
    public Page<Pharmacy> search(VerificationStatus status, String search, Pageable pageable) {
        return pharmacyRepository.search(status, search, pageable);
    }

    /** SLP: Pharmacy Onboarding & Verification → "Issue verified-pharmacy badge on approval" */
    @Transactional
    public Pharmacy approve(Long pharmacyId, User admin) {
        Pharmacy pharmacy = getById(pharmacyId);
        String oldStatus = pharmacy.getVerificationStatus().name();
        pharmacy.setVerificationStatus(VerificationStatus.APPROVED);
        pharmacy.setRejectionReason(null);
        pharmacy = pharmacyRepository.save(pharmacy);

        auditLogService.record(admin, "APPROVE_PHARMACY: " + pharmacy.getPharmacyName(), oldStatus, "APPROVED");
        notificationService.notifyPharmacyDecision(pharmacy, true);
        return pharmacy;
    }

    @Transactional
    public Pharmacy reject(Long pharmacyId, String reason, User admin) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleException("Please give a reason for rejecting this pharmacy.");
        }
        Pharmacy pharmacy = getById(pharmacyId);
        String oldStatus = pharmacy.getVerificationStatus().name();
        pharmacy.setVerificationStatus(VerificationStatus.REJECTED);
        pharmacy.setRejectionReason(reason);
        pharmacy = pharmacyRepository.save(pharmacy);

        auditLogService.record(admin, "REJECT_PHARMACY: " + pharmacy.getPharmacyName(), oldStatus, "REJECTED (" + reason + ")");
        notificationService.notifyPharmacyDecision(pharmacy, false);
        return pharmacy;
    }

    /** SLP: Medicine Search & Pharmacy Discovery → "Integrate mapping/geolocation service" */
    public List<Pharmacy> findAllApproved() {
        return pharmacyRepository.search(VerificationStatus.APPROVED, null, Pageable.unpaged()).getContent();
    }

    public long countByStatus(VerificationStatus status) {
        return pharmacyRepository.countByVerificationStatus(status);
    }

    /**
     * SLP: Admin Oversight & Complaint Handling → "full CRUD" for pharmacy listings
     *
     * An admin adding a pharmacy directly is a trusted, deliberate act —
     * unlike a pharmacist's own self-registration, it doesn't need a
     * separate review step, so it's created straight into APPROVED.
     * Note this doesn't attach any staff login — pharmacist accounts are
     * still created via the registration flow (AuthService), which
     * always creates its own pharmacy alongside the account; there's no
     * "join this existing pharmacy" step for a pharmacist to attach to
     * one an admin created by hand.
     */
    @Transactional
    public Pharmacy create(PharmacyAdminRequest request, User admin) {
        if (pharmacyRepository.existsByLicenseNumber(request.getLicenseNumber())) {
            throw new BusinessRuleException(
                    "A pharmacy with license number \"" + request.getLicenseNumber() + "\" already exists.");
        }
        Pharmacy pharmacy = new Pharmacy();
        applyAdminRequest(pharmacy, request);
        pharmacy.setVerificationStatus(VerificationStatus.APPROVED);
        pharmacy = pharmacyRepository.save(pharmacy);
        auditLogService.record(admin, "CREATE_PHARMACY: " + pharmacy.getPharmacyName(), null, describe(pharmacy));
        return pharmacy;
    }

    /** SLP: Admin Oversight & Complaint Handling → "full CRUD" for pharmacy listings */
    @Transactional
    public Pharmacy updateByAdmin(Long pharmacyId, PharmacyAdminRequest request, User admin) {
        Pharmacy pharmacy = getById(pharmacyId);
        boolean licenseChanged = !pharmacy.getLicenseNumber().equals(request.getLicenseNumber());
        if (licenseChanged && pharmacyRepository.existsByLicenseNumber(request.getLicenseNumber())) {
            throw new BusinessRuleException(
                    "Another pharmacy already uses license number \"" + request.getLicenseNumber() + "\".");
        }
        String before = describe(pharmacy);
        applyAdminRequest(pharmacy, request);
        pharmacy = pharmacyRepository.save(pharmacy);
        auditLogService.record(admin, "UPDATE_PHARMACY: " + pharmacy.getPharmacyName(), before, describe(pharmacy));
        return pharmacy;
    }

    /**
     * SLP: Admin Oversight & Complaint Handling → "full CRUD" for pharmacy listings
     *
     * Reservations are the platform's transaction ledger, so a pharmacy
     * with any reservation history — active or long since collected —
     * is never deletable; {@link #reject} is the right tool for taking
     * one out of service instead. A pharmacy with no reservations has no
     * such history to protect, so it's removed along with the listings
     * and staff logins that only exist because of it (their
     * notifications first, since those are a non-nullable foreign key
     * to the user).
     */
    @Transactional
    public void delete(Long pharmacyId, User admin) {
        Pharmacy pharmacy = getById(pharmacyId);
        if (reservationRepository.existsByPharmacyId(pharmacyId)) {
            throw new BusinessRuleException("Cannot delete " + pharmacy.getPharmacyName() +
                    " — it has reservation history that needs to be kept for the record. " +
                    "Reject it instead if it should stop appearing to patients.");
        }
        String description = describe(pharmacy);
        List<User> staff = pharmacy.getStaff();
        if (!staff.isEmpty()) {
            notificationRepository.deleteByUserIdIn(staff.stream().map(User::getId).toList());
            userRepository.deleteAll(staff);
        }
        inventoryRepository.deleteByPharmacyId(pharmacyId);
        pharmacyRepository.delete(pharmacy);
        auditLogService.record(admin, "DELETE_PHARMACY: " + description, description, "DELETED");
    }

    private void applyAdminRequest(Pharmacy pharmacy, PharmacyAdminRequest request) {
        pharmacy.setPharmacyName(request.getPharmacyName());
        pharmacy.setLicenseNumber(request.getLicenseNumber());
        pharmacy.setAddress(request.getAddress());
        pharmacy.setLatitude(request.getLatitude());
        pharmacy.setLongitude(request.getLongitude());
        pharmacy.setPhone(request.getPhone());
        pharmacy.setOpeningHours(request.getOpeningHours());
    }

    private String describe(Pharmacy pharmacy) {
        return pharmacy.getPharmacyName() + " (" + pharmacy.getLicenseNumber() + "), " + pharmacy.getAddress();
    }
}
