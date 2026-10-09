package com.medifind.controller;

import com.medifind.dto.MedicineSearchResultRow;
import com.medifind.dto.ReservationRequest;
import com.medifind.entity.Inventory;
import com.medifind.entity.Medicine;
import com.medifind.entity.Pharmacy;
import com.medifind.entity.Reservation;
import com.medifind.entity.User;
import com.medifind.enums.ReservationStatus;
import com.medifind.exception.BusinessRuleException;
import com.medifind.security.CustomUserDetails;
import com.medifind.service.InventoryService;
import com.medifind.service.MedicineService;
import com.medifind.service.NotificationService;
import com.medifind.service.PharmacyService;
import com.medifind.service.ReservationService;
import com.medifind.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * SLP: Patient Authentication & Profile / Medicine Search & Pharmacy
 * Discovery / Patient Reservation Management
 */
@Controller
@RequestMapping("/patient")
public class PatientController {

    private final MedicineService medicineService;
    private final ReservationService reservationService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final PharmacyService pharmacyService;
    private final InventoryService inventoryService;

    public PatientController(MedicineService medicineService, ReservationService reservationService,
                             UserService userService, NotificationService notificationService,
                             PharmacyService pharmacyService, InventoryService inventoryService) {
        this.medicineService = medicineService;
        this.reservationService = reservationService;
        this.userService = userService;
        this.notificationService = notificationService;
        this.pharmacyService = pharmacyService;
        this.inventoryService = inventoryService;
    }

    /**
     * Runs before every action in this controller and merges its
     */
    @ModelAttribute("unreadNotifications")
    public long unreadNotifications(@AuthenticationPrincipal CustomUserDetails principal) {
        return principal == null ? 0 : notificationService.unreadCountFor(principal.getUser().getId());
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        User patient = principal.getUser();
        Page<Reservation> recent = reservationService.historyForPatient(
                patient.getId(), null, null, null, PageRequest.of(0, 5));
        model.addAttribute("patient", patient);
        model.addAttribute("recentReservations", recent.getContent());
        return "patient/dashboard";
    }

    /**
     * SLP: Medicine Search & Pharmacy Discovery → "Search medicine by
     * name, generic name or brand", "Filter results by distance and
     * availability"
     */
    @GetMapping("/search")
    public String search(@RequestParam(required = false) String q,
                         @RequestParam(required = false) Double lat,
                         @RequestParam(required = false) Double lng,
                         @RequestParam(required = false) Double maxDistanceKm,
                         Model model) {
        List<MedicineSearchResultRow> results = (q == null || q.isBlank())
                ? List.of()
                : medicineService.search(q, lat, lng, maxDistanceKm);
        model.addAttribute("query", q);
        model.addAttribute("results", results);
        model.addAttribute("maxDistanceKm", maxDistanceKm);
        return "patient/search";
    }

    @GetMapping("/medicine/{id}")
    public String medicineDetails(@PathVariable Long id,
                                  @RequestParam(required = false) Double lat,
                                  @RequestParam(required = false) Double lng,
                                  Model model) {
        Medicine medicine = medicineService.getById(id);
        MedicineSearchResultRow result = medicineService.getOffersForMedicine(id, lat, lng);
        model.addAttribute("medicine", medicine);
        model.addAttribute("result", result);
        model.addAttribute("reservationRequest", new ReservationRequest());
        return "patient/medicine-details";
    }

    /** SLP: Medicine Search & Pharmacy Discovery → "Pharmacy details page" */
    @GetMapping("/pharmacy/{id}")
    public String pharmacyDetails(@PathVariable Long id, Model model) {
        Pharmacy pharmacy = pharmacyService.getById(id);
        if (pharmacy.getVerificationStatus() != com.medifind.enums.VerificationStatus.APPROVED) {
            throw new com.medifind.exception.ResourceNotFoundException("Pharmacy not found: " + id);
        }
        List<Inventory> stock = inventoryService.listForPharmacy(id).stream()
                .filter(Inventory::isPurchasable)
                .toList();
        model.addAttribute("pharmacy", pharmacy);
        model.addAttribute("stock", stock);
        return "patient/pharmacy-details";
    }

    /** SLP: Medicine Search & Pharmacy Discovery → "Integrate mapping/geolocation service" */
    @GetMapping("/pharmacy-map")
    public String pharmacyMap() {
        return "patient/pharmacy-map";
    }

    /** SLP: Patient Reservation Management → "Reserve medicine at a chosen pharmacy" */
    @PostMapping("/reserve")
    public String reserve(@Valid @ModelAttribute ReservationRequest reservationRequest, BindingResult bindingResult,
                          @AuthenticationPrincipal CustomUserDetails principal,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Please choose a valid pharmacy and quantity.");
            return "redirect:/patient/medicine/" + reservationRequest.getMedicineId();
        }
        try {
            Reservation reservation = reservationService.reserve(principal.getUser(), reservationRequest);
            redirectAttributes.addFlashAttribute("success",
                    "Reserved! Your confirmation code is " + reservation.getConfirmationCode() + ".");
            return "redirect:/patient/reservations";
        } catch (BusinessRuleException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/patient/medicine/" + reservationRequest.getMedicineId();
        }
    }

    /**
     * SLP: Patient Reservation Management → "Reservation history with
     * date/status filters", "Cancel reservation before pickup window expires"
     */
    @GetMapping("/reservations")
    public String reservations(@AuthenticationPrincipal CustomUserDetails principal,
                               @RequestParam(required = false) ReservationStatus status,
                               @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate from,
                               @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate to,
                               @RequestParam(defaultValue = "0") int page,
                               Model model) {
        LocalDateTime fromDateTime = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDateTime = to != null ? LocalDateTime.of(to, LocalTime.MAX) : null;

        Page<Reservation> reservations = reservationService.historyForPatient(
                principal.getUser().getId(), status, fromDateTime, toDateTime, PageRequest.of(page, 10));

        model.addAttribute("reservations", reservations);
        model.addAttribute("status", status);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        return "patient/reservations";
    }

    @PostMapping("/reservations/{id}/cancel")
    public String cancelReservation(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal,
                                    RedirectAttributes redirectAttributes) {
        try {
            reservationService.cancel(id, principal.getUser().getId());
            redirectAttributes.addFlashAttribute("success", "Reservation cancelled.");
        } catch (BusinessRuleException | AccessDeniedException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/patient/reservations";
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        model.addAttribute("patient", principal.getUser());
        return "patient/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@AuthenticationPrincipal CustomUserDetails principal,
                                @RequestParam String fullName, @RequestParam String phone,
                                RedirectAttributes redirectAttributes) {
        userService.updateProfile(principal.getUser().getId(), fullName, phone);
        redirectAttributes.addFlashAttribute("success", "Profile updated.");
        return "redirect:/patient/profile";
    }

    @GetMapping("/notifications")
    public String notifications(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        model.addAttribute("notifications", notificationService.recentFor(principal.getUser().getId()));
        return "patient/notifications";
    }

    @PostMapping("/notifications/mark-read")
    public String markNotificationsRead(@AuthenticationPrincipal CustomUserDetails principal,
                                        RedirectAttributes redirectAttributes) {
        notificationService.markAllAsRead(principal.getUser().getId());
        return "redirect:/patient/notifications";
    }
}
