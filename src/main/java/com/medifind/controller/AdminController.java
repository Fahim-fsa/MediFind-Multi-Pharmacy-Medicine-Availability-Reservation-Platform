package com.medifind.controller;

import com.medifind.dto.PharmacyAdminRequest;
import com.medifind.entity.Pharmacy;
import com.medifind.entity.SystemSetting;
import com.medifind.enums.ReservationStatus;
import com.medifind.enums.UserStatus;
import com.medifind.enums.VerificationStatus;
import com.medifind.exception.BusinessRuleException;
import com.medifind.security.CustomUserDetails;
import com.medifind.service.*;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * SLP: Pharmacy Onboarding & Verification / Admin Account & Verification

 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final PharmacyService pharmacyService;
    private final UserService userService;
    private final InventoryService inventoryService;
    private final ReservationService reservationService;
    private final ReportService reportService;
    private final AuditLogService auditLogService;
    private final SystemSettingService systemSettingService;

    public AdminController(PharmacyService pharmacyService, UserService userService, InventoryService inventoryService,
                           ReservationService reservationService, ReportService reportService,
                           AuditLogService auditLogService, SystemSettingService systemSettingService) {
        this.pharmacyService = pharmacyService;
        this.userService = userService;
        this.inventoryService = inventoryService;
        this.reservationService = reservationService;
        this.reportService = reportService;
        this.auditLogService = auditLogService;
        this.systemSettingService = systemSettingService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("activePharmacies", reportService.activePharmacyCount());
        model.addAttribute("pendingPharmacies", reportService.pendingPharmacyCount());
        model.addAttribute("activePatients", reportService.activePatientCount());
        model.addAttribute("pendingVerificationList", pharmacyService.findPendingVerification());
        return "admin/dashboard";
    }

    // =====================================================================
    // Pharmacy verification
    // SLP: Pharmacy Onboarding & Verification
    // =====================================================================

    @GetMapping("/verification")
    public String verification(Model model) {
        model.addAttribute("pending", pharmacyService.findPendingVerification());
        return "admin/verification";
    }

    @PostMapping("/verification/{id}/approve")
    public String approve(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal,
                          RedirectAttributes redirectAttributes) {
        Pharmacy pharmacy = pharmacyService.approve(id, principal.getUser());
        redirectAttributes.addFlashAttribute("success", pharmacy.getPharmacyName() + " approved and now live.");
        return "redirect:/admin/verification";
    }

    @PostMapping("/verification/{id}/reject")
    public String reject(@PathVariable Long id, @RequestParam String reason,
                         @AuthenticationPrincipal CustomUserDetails principal, RedirectAttributes redirectAttributes) {
        try {
            Pharmacy pharmacy = pharmacyService.reject(id, reason, principal.getUser());
            redirectAttributes.addFlashAttribute("success", pharmacy.getPharmacyName() + " rejected.");
        } catch (BusinessRuleException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/verification";
    }

    // =====================================================================
    // User management
    // SLP: Admin Account & Verification Mgmt
    // =====================================================================

    @GetMapping("/users")
    public String users(@RequestParam(required = false) UserStatus status,
                        @RequestParam(required = false) String search,
                        @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("users", userService.searchPatients(status, search, PageRequest.of(page, 15)));
        model.addAttribute("status", status);
        model.addAttribute("search", search);
        return "admin/users";
    }

    @PostMapping("/users/{id}/suspend")
    public String suspend(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal,
                          RedirectAttributes redirectAttributes) {
        var user = userService.setStatus(id, UserStatus.SUSPENDED);
        auditLogService.record(principal.getUser(), "SUSPEND_USER: " + user.getEmail(), "ACTIVE", "SUSPENDED");
        redirectAttributes.addFlashAttribute("success", user.getFullName() + " has been suspended.");
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/activate")
    public String activate(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal,
                           RedirectAttributes redirectAttributes) {
        var user = userService.setStatus(id, UserStatus.ACTIVE);
        auditLogService.record(principal.getUser(), "REACTIVATE_USER: " + user.getEmail(), "SUSPENDED", "ACTIVE");
        redirectAttributes.addFlashAttribute("success", user.getFullName() + " has been reactivated.");
        return "redirect:/admin/users";
    }

    // =====================================================================
    // Listings oversight
    // SLP: Admin Oversight & Complaint Handling
    // =====================================================================

    @GetMapping("/listings")
    public String listings(@RequestParam(required = false) VerificationStatus status,
                           @RequestParam(required = false) String search,
                           @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("pharmacies", pharmacyService.search(status, search, PageRequest.of(page, 15)));
        model.addAttribute("status", status);
        model.addAttribute("search", search);
        return "admin/listings";
    }

    @GetMapping("/listings/{pharmacyId}/inventory")
    public String pharmacyInventory(@PathVariable Long pharmacyId, Model model) {
        model.addAttribute("pharmacy", pharmacyService.getById(pharmacyId));
        model.addAttribute("items", inventoryService.listForPharmacy(pharmacyId));
        return "admin/pharmacy-inventory";
    }

    @PostMapping("/listings/inventory/{inventoryId}/remove")
    public String removeListing(@PathVariable Long inventoryId, @AuthenticationPrincipal CustomUserDetails principal,
                                @RequestParam Long pharmacyId, RedirectAttributes redirectAttributes) {
        inventoryService.removeListing(inventoryId, principal.getUser());
        redirectAttributes.addFlashAttribute("success", "Listing removed.");
        return "redirect:/admin/listings/" + pharmacyId + "/inventory";
    }

    @GetMapping("/listings/add")
    public String newPharmacyForm(Model model) {
        model.addAttribute("pharmacyAdminRequest", new PharmacyAdminRequest());
        model.addAttribute("formTitle", "Add a pharmacy");
        model.addAttribute("formAction", "/admin/listings/add");
        model.addAttribute("submitLabel", "Add pharmacy");
        return "admin/pharmacy-form";
    }

    @GetMapping("/listings/{id}/edit")
    public String editPharmacyForm(@PathVariable Long id, Model model) {
        Pharmacy pharmacy = pharmacyService.getById(id);
        PharmacyAdminRequest request = new PharmacyAdminRequest();
        request.setPharmacyName(pharmacy.getPharmacyName());
        request.setLicenseNumber(pharmacy.getLicenseNumber());
        request.setAddress(pharmacy.getAddress());
        request.setLatitude(pharmacy.getLatitude());
        request.setLongitude(pharmacy.getLongitude());
        request.setPhone(pharmacy.getPhone());
        request.setOpeningHours(pharmacy.getOpeningHours());
        model.addAttribute("pharmacyAdminRequest", request);
        model.addAttribute("formTitle", "Edit " + pharmacy.getPharmacyName());
        model.addAttribute("formAction", "/admin/listings/" + id + "/update");
        model.addAttribute("submitLabel", "Save changes");
        return "admin/pharmacy-form";
    }

    @PostMapping("/listings/add")
    public String addPharmacy(@Valid @ModelAttribute PharmacyAdminRequest pharmacyAdminRequest,
                              BindingResult bindingResult, @AuthenticationPrincipal CustomUserDetails principal,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Please check the highlighted fields.");
            return "redirect:/admin/listings";
        }
        try {
            Pharmacy pharmacy = pharmacyService.create(pharmacyAdminRequest, principal.getUser());
            redirectAttributes.addFlashAttribute("success", pharmacy.getPharmacyName() + " added.");
        } catch (BusinessRuleException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/listings";
    }

    @PostMapping("/listings/{id}/update")
    public String updatePharmacy(@PathVariable Long id, @Valid @ModelAttribute PharmacyAdminRequest pharmacyAdminRequest,
                                 BindingResult bindingResult, @AuthenticationPrincipal CustomUserDetails principal,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Please check the highlighted fields.");
            return "redirect:/admin/listings";
        }
        try {
            Pharmacy pharmacy = pharmacyService.updateByAdmin(id, pharmacyAdminRequest, principal.getUser());
            redirectAttributes.addFlashAttribute("success", pharmacy.getPharmacyName() + " updated.");
        } catch (BusinessRuleException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/listings";
    }

    @PostMapping("/listings/{id}/delete")
    public String deletePharmacy(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal,
                                 RedirectAttributes redirectAttributes) {
        try {
            pharmacyService.delete(id, principal.getUser());
            redirectAttributes.addFlashAttribute("success", "Pharmacy deleted.");
        } catch (BusinessRuleException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/listings";
    }

    // =====================================================================
    // Reservation oversight
    // SLP: Admin Oversight & Complaint Handling → "View and cancel a
    // disputed reservation"
    // =====================================================================

    @GetMapping("/reservations")
    public String reservations(@RequestParam(required = false) ReservationStatus status,
                               @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("reservations", reservationService.adminSearch(status, PageRequest.of(page, 15)));
        model.addAttribute("status", status);
        return "admin/reservations";
    }

    @PostMapping("/reservations/{id}/cancel")
    public String cancelReservation(@PathVariable Long id, @RequestParam String reason,
                                    @AuthenticationPrincipal CustomUserDetails principal,
                                    RedirectAttributes redirectAttributes) {
        try {
            reservationService.adminCancel(id, principal.getUser(), reason);
            redirectAttributes.addFlashAttribute("success", "Reservation cancelled.");
        } catch (BusinessRuleException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/reservations";
    }

    // =====================================================================
    // Reports
    // SLP: Admin Reporting & Configuration → "Generate platform-wide
    // reports", "Export reports as CSV/PDF"
    // =====================================================================

    @GetMapping("/reports")
    public String reports(@RequestParam(defaultValue = "14") int days, Model model) {
        model.addAttribute("days", days);
        model.addAttribute("dailyReservations", reportService.dailyReservationsTable(days));
        model.addAttribute("mostSearched", reportService.mostSearchedMedicinesTable());
        model.addAttribute("activePharmacies", reportService.activePharmaciesTable());
        model.addAttribute("activeUsers", reportService.activeUsersTable());
        return "admin/reports";
    }

    @GetMapping("/reports/export")
    public ResponseEntity<ByteArrayResource> exportReport(@RequestParam String type, @RequestParam String format,
                                                          @RequestParam(defaultValue = "14") int days) {
        ReportService.ReportTable table = switch (type) {
            case "daily-reservations" -> reportService.dailyReservationsTable(days);
            case "most-searched" -> reportService.mostSearchedMedicinesTable();
            case "active-pharmacies" -> reportService.activePharmaciesTable();
            case "active-users" -> reportService.activeUsersTable();
            default -> throw new BusinessRuleException("Unknown report type: " + type);
        };

        byte[] content;
        MediaType mediaType;
        String extension;
        if ("pdf".equalsIgnoreCase(format)) {
            content = reportService.exportPdf(table);
            mediaType = MediaType.APPLICATION_PDF;
            extension = "pdf";
        } else {
            content = reportService.exportCsv(table);
            mediaType = MediaType.parseMediaType("text/csv");
            extension = "csv";
        }

        String filename = type + "." + extension;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(new ByteArrayResource(content));
    }

    // =====================================================================
    // Audit log
    // SLP: Platform Security & Compliance → "Platform-wide audit logging"
    // =====================================================================

    @GetMapping("/audit-log")
    public String auditLog(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("entries", auditLogService.findAll(PageRequest.of(page, 20)));
        return "admin/audit-log";
    }

    // =====================================================================
    // Platform settings
    // SLP: Admin Reporting & Configuration → "Configure reservation time
    // window & notification templates"
    // =====================================================================

    @GetMapping("/settings")
    public String settings(Model model) {
        model.addAttribute("settings", systemSettingService.findAll());
        return "admin/settings";
    }

    @PostMapping("/settings/{id}/update")
    public String updateSetting(@PathVariable Long id, @RequestParam String value,
                                @AuthenticationPrincipal CustomUserDetails principal,
                                RedirectAttributes redirectAttributes) {
        SystemSetting before = systemSettingService.findAll().stream()
                .filter(s -> s.getId().equals(id)).findFirst().orElse(null);
        String oldValue = before != null ? before.getSettingValue() : null;

        SystemSetting updated = systemSettingService.update(id, value);
        auditLogService.record(principal.getUser(), "UPDATE_SETTING: " + updated.getSettingKey(), oldValue, value);

        redirectAttributes.addFlashAttribute("success", "Setting updated.");
        return "redirect:/admin/settings";
    }
}
