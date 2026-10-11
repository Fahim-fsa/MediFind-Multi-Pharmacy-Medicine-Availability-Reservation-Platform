package com.medifind.service;

import com.medifind.entity.*;
import com.medifind.enums.NotificationType;
import com.medifind.enums.Role;
import com.medifind.repository.NotificationRepository;
import com.medifind.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * SLP: Notifications & 3rd-Party Integrations → "Build shared
 * Notification Service", "Reservation confirmation notification",
 * "Reservation status-change notifications", "Low-stock notification to
 * pharmacy staff"
 *
 * The one place every other service goes through to notify a user.
 * Every method here does two things for each recipient: save a
 * {@link Notification} row (so it shows up in their in-app notification
 * list) and call {@link EmailSimulationService#send} (so the same event
 * is also "emailed" — see that class for why this is simulated rather
 * than a real send).
 */
@Service
public class NotificationService {

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, h:mm a");

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EmailSimulationService emailSimulationService;
    private final SystemSettingService systemSettingService;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository,
                               EmailSimulationService emailSimulationService, SystemSettingService systemSettingService) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.emailSimulationService = emailSimulationService;
        this.systemSettingService = systemSettingService;
    }

    @Transactional
    public void notifyReservationConfirmation(Reservation reservation) {
        String template = systemSettingService.getString("notification.template.reservation.confirmation",
                "Hi {patientName}, your reservation for {quantity} x {medicineName} at {pharmacyName} is confirmed. " +
                        "Show code {confirmationCode} at the counter before {pickupDeadline}.");
        String message = template
                .replace("{patientName}", reservation.getPatient().getFullName())
                .replace("{quantity}", String.valueOf(reservation.getQuantity()))
                .replace("{medicineName}", reservation.getMedicine().getName())
                .replace("{pharmacyName}", reservation.getPharmacy().getPharmacyName())
                .replace("{confirmationCode}", reservation.getConfirmationCode())
                .replace("{pickupDeadline}", reservation.getPickupDeadline().format(DISPLAY_FORMAT));

        save(reservation.getPatient(), message, NotificationType.RESERVATION_CONFIRMATION);
        emailSimulationService.send(reservation.getPatient().getEmail(), "Your MediFind reservation is confirmed", message);
    }

    @Transactional
    public void notifyReservationStatusUpdate(Reservation reservation, String statusDescription) {
        String message = String.format("Your reservation %s for %s at %s is now %s.",
                reservation.getConfirmationCode(), reservation.getMedicine().getName(),
                reservation.getPharmacy().getPharmacyName(), statusDescription);
        save(reservation.getPatient(), message, NotificationType.RESERVATION_STATUS_UPDATE);
        emailSimulationService.send(reservation.getPatient().getEmail(), "Update on your MediFind reservation", message);
    }

    @Transactional
    public void notifyNewReservationToPharmacyStaff(Reservation reservation) {
        String message = String.format("New reservation %s: %d x %s, pickup by %s.",
                reservation.getConfirmationCode(), reservation.getQuantity(),
                reservation.getMedicine().getName(), reservation.getPickupDeadline().format(DISPLAY_FORMAT));
        for (User staff : staffOf(reservation.getPharmacy())) {
            save(staff, message, NotificationType.NEW_RESERVATION);
            emailSimulationService.send(staff.getEmail(), "New MediFind reservation", message);
        }
    }

    @Transactional
    public void notifyLowStock(Inventory inventory) {
        String message = String.format("Low stock: %s at %s has only %d unit(s) left.",
                inventory.getMedicine().getName(), inventory.getPharmacy().getPharmacyName(), inventory.getQuantity());
        for (User staff : staffOf(inventory.getPharmacy())) {
            save(staff, message, NotificationType.LOW_STOCK_WARNING);
            emailSimulationService.send(staff.getEmail(), "MediFind low-stock warning", message);
        }
    }

    @Transactional
    public void notifyAdminsOfPharmacyRegistration(Pharmacy pharmacy) {
        String message = String.format("New pharmacy awaiting verification: %s (license %s).",
                pharmacy.getPharmacyName(), pharmacy.getLicenseNumber());
        for (User admin : userRepository.findByRole(Role.ADMIN)) {
            save(admin, message, NotificationType.PHARMACY_REGISTRATION_REQUEST);
            emailSimulationService.send(admin.getEmail(), "MediFind: new pharmacy to verify", message);
        }
    }

    @Transactional
    public void notifyPharmacyDecision(Pharmacy pharmacy, boolean approved) {
        for (User staff : staffOf(pharmacy)) {
            String message = approved
                    ? String.format("Great news! %s has been verified and is now live on MediFind.", pharmacy.getPharmacyName())
                    : String.format("%s was not approved: %s", pharmacy.getPharmacyName(),
                    pharmacy.getRejectionReason() != null ? pharmacy.getRejectionReason() : "please contact support.");
            save(staff, message, approved ? NotificationType.PHARMACY_APPROVED : NotificationType.PHARMACY_REJECTED);
            emailSimulationService.send(staff.getEmail(), "MediFind pharmacy verification update", message);
        }
    }

    @Transactional
    public void sendPasswordResetEmail(User user, String resetLink) {
        String message = "We received a request to reset your MediFind password. Use this link (valid for 30 minutes): " + resetLink;
        save(user, "Password reset requested.", NotificationType.PASSWORD_RESET);
        emailSimulationService.send(user.getEmail(), "Reset your MediFind password", message);
    }

    @Transactional
    public void sendMfaCode(User admin, String code) {
        String message = "Your MediFind admin sign-in code is " + code + ". It expires in a few minutes.";
        save(admin, "Admin sign-in verification code sent.", NotificationType.MFA_CODE);
        emailSimulationService.send(admin.getEmail(), "Your MediFind admin verification code", message);
    }

    @Transactional(readOnly = true)
    public List<Notification> recentFor(Long userId) {
        return notificationRepository.findTop20ByUserIdOrderByCreatedDateDesc(userId);
    }

    @Transactional(readOnly = true)
    public long unreadCountFor(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }

    private List<User> staffOf(Pharmacy pharmacy) {
        return userRepository.findByRole(Role.PHARMACIST).stream()
                .filter(u -> u.getPharmacy() != null && u.getPharmacy().getId().equals(pharmacy.getId()))
                .toList();
    }

    private void save(User user, String message, NotificationType type) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setMessage(message);
        notification.setType(type);
        notificationRepository.save(notification);
    }
}

