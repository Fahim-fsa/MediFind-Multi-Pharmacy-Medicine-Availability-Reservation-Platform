package com.medifind.controller;

import com.medifind.dto.InventoryItemRequest;
import com.medifind.dto.PharmacyProfileUpdateRequest;
import com.medifind.dto.ReservationQueueItem;
import com.medifind.entity.Inventory;
import com.medifind.entity.Pharmacy;
import com.medifind.entity.User;
import com.medifind.exception.BusinessRuleException;
import com.medifind.security.CustomUserDetails;
import com.medifind.service.InventoryService;
import com.medifind.service.MedicineService;
import com.medifind.service.NotificationService;
import com.medifind.service.PharmacyService;
import com.medifind.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * SLP: Pharmacy Onboarding & Verification / Pharmacy Inventory & Listing
 * Mgmt / Pharmacy Reservation Fulfilment
 *
 */
@Controller
@RequestMapping("/pharmacist")
public class PharmacistController {

    private final PharmacyService pharmacyService;
    private final InventoryService inventoryService;
    private final ReservationService reservationService;
    private final MedicineService medicineService;
    private final NotificationService notificationService;

    public PharmacistController(PharmacyService pharmacyService, InventoryService inventoryService,
                                 ReservationService reservationService, MedicineService medicineService,
                                 NotificationService notificationService) {
        this.pharmacyService = pharmacyService;
        this.inventoryService = inventoryService;
        this.reservationService = reservationService;
        this.medicineService = medicineService;
        this.notificationService = notificationService;
    }

    /** See PatientController's identical method for why this is a @ModelAttribute. */
    @ModelAttribute("unreadNotifications")
    public long unreadNotifications(@AuthenticationPrincipal CustomUserDetails principal) {
        return principal == null ? 0 : notificationService.unreadCountFor(principal.getUser().getId());
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        Pharmacy pharmacy = pharmacyOf(principal);
        List<ReservationQueueItem> queue = reservationService.incomingQueueForPharmacy(pharmacy.getId());
        List<Inventory> lowStock = inventoryService.lowStockForPharmacy(pharmacy.getId());

        model.addAttribute("pharmacy", pharmacy);
        model.addAttribute("queueSize", queue.size());
        model.addAttribute("recentQueue", queue.stream().limit(5).toList());
        model.addAttribute("lowStockItems", lowStock);
        model.addAttribute("totalListings", inventoryService.listForPharmacy(pharmacy.getId()).size());
        return "pharmacist/dashboard";
    }

    // =====================================================================
    // Inventory
    // SLP: Pharmacy Inventory & Listing Mgmt
    // =====================================================================

    @GetMapping("/inventory")
    public String inventory(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        Pharmacy pharmacy = pharmacyOf(principal);
        model.addAttribute("pharmacy", pharmacy);
        model.addAttribute("items", inventoryService.listForPharmacy(pharmacy.getId()));
        model.addAttribute("categories", medicineService.listCategories());
        if (!model.containsAttribute("inventoryItemRequest")) {
            model.addAttribute("inventoryItemRequest", new InventoryItemRequest());
        }
        return "pharmacist/inventory";
    }

    @PostMapping("/inventory/add")
    public String addInventory(@AuthenticationPrincipal CustomUserDetails principal,
                                @Valid @ModelAttribute InventoryItemRequest inventoryItemRequest,
                                BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        Pharmacy pharmacy = pharmacyOf(principal);
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Please check the highlighted fields.");
            return "redirect:/pharmacist/inventory";
        }
        try {
            inventoryService.addListing(pharmacy.getId(), inventoryItemRequest);
            redirectAttributes.addFlashAttribute("success", "Medicine added to your inventory.");
        } catch (BusinessRuleException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/pharmacist/inventory";
    }

    @PostMapping("/inventory/{id}/update")
    public String updateInventory(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long id,
                                   @Valid @ModelAttribute InventoryItemRequest inventoryItemRequest,
                                   BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        Pharmacy pharmacy = pharmacyOf(principal);
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Please check the highlighted fields.");
            return "redirect:/pharmacist/inventory";
        }
        try {
            inventoryService.updateListing(id, inventoryItemRequest, pharmacy.getId());
            redirectAttributes.addFlashAttribute("success", "Listing updated.");
        } catch (AccessDeniedException ex) {
            redirectAttributes.addFlashAttribute("error", "You can only edit your own pharmacy's listings.");
        }
        return "redirect:/pharmacist/inventory";
    }

    // =====================================================================
    // Reservations
    // SLP: Pharmacy Reservation Fulfilment
    // =====================================================================

    @GetMapping("/reservations")
    public String reservations(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        Pharmacy pharmacy = pharmacyOf(principal);
        model.addAttribute("pharmacy", pharmacy);
        model.addAttribute("queue", reservationService.incomingQueueForPharmacy(pharmacy.getId()));
        return "pharmacist/reservations";
    }

    @PostMapping("/reservations/{id}/collect")
    public String collect(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long id,
                           RedirectAttributes redirectAttributes) {
        Pharmacy pharmacy = pharmacyOf(principal);
        try {
            reservationService.markCollected(id, pharmacy.getId());
            redirectAttributes.addFlashAttribute("success", "Marked as collected.");
        } catch (BusinessRuleException | AccessDeniedException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/pharmacist/reservations";
    }

    @PostMapping("/reservations/{id}/reject")
    public String reject(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long id,
                          RedirectAttributes redirectAttributes) {
        Pharmacy pharmacy = pharmacyOf(principal);
        try {
            reservationService.pharmacistCancel(id, pharmacy.getId());
            redirectAttributes.addFlashAttribute("success", "Reservation rejected and stock restored.");
        } catch (BusinessRuleException | AccessDeniedException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/pharmacist/reservations";
    }

    // =====================================================================
    // Pharmacy profile
    // =====================================================================

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        model.addAttribute("pharmacy", pharmacyOf(principal));
        return "pharmacist/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@AuthenticationPrincipal CustomUserDetails principal,
                                 @Valid @ModelAttribute PharmacyProfileUpdateRequest request,
                                 BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        Pharmacy pharmacy = pharmacyOf(principal);
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Please check the highlighted fields.");
            return "redirect:/pharmacist/profile";
        }
        pharmacyService.updateProfile(pharmacy.getId(), request);
        redirectAttributes.addFlashAttribute("success", "Pharmacy profile updated.");
        return "redirect:/pharmacist/profile";
    }

    @GetMapping("/notifications")
    public String notifications(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        model.addAttribute("notifications", notificationService.recentFor(principal.getUser().getId()));
        return "pharmacist/notifications";
    }

    @PostMapping("/notifications/mark-read")
    public String markNotificationsRead(@AuthenticationPrincipal CustomUserDetails principal) {
        notificationService.markAllAsRead(principal.getUser().getId());
        return "redirect:/pharmacist/notifications";
    }

    private Pharmacy pharmacyOf(CustomUserDetails principal) {
        User user = principal.getUser();
        if (user.getPharmacy() == null) {
            throw new IllegalStateException("Pharmacist account " + user.getEmail() + " has no linked pharmacy.");
        }
        return user.getPharmacy();
    }
}
