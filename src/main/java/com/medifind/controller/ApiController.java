package com.medifind.controller;

import com.medifind.dto.PharmacyMapPoint;
import com.medifind.dto.ReservationQueueItem;
import com.medifind.entity.Pharmacy;
import com.medifind.security.CustomUserDetails;
import com.medifind.service.NotificationService;
import com.medifind.service.PharmacyService;
import com.medifind.service.ReservationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * SLP: Medicine Search & Pharmacy Discovery → "Integrate mapping/
 * geolocation service"
 * SLP: Pharmacy Reservation Fulfilment → "View incoming reservations in
 * real time"
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    private final PharmacyService pharmacyService;
    private final ReservationService reservationService;
    private final NotificationService notificationService;

    public ApiController(PharmacyService pharmacyService, ReservationService reservationService,
                         NotificationService notificationService) {
        this.pharmacyService = pharmacyService;
        this.reservationService = reservationService;
        this.notificationService = notificationService;
    }

    /** Backs the Leaflet.js map on patient/pharmacy-map.html. */
    @GetMapping("/patient/pharmacies/map")
    public List<PharmacyMapPoint> pharmacyMapPoints() {
        List<Pharmacy> pharmacies = pharmacyService.findAllApproved();
        return pharmacies.stream()
                .map(p -> new PharmacyMapPoint(p.getId(), p.getPharmacyName(), p.getAddress(),
                        p.getLatitude(), p.getLongitude(), true, p.getOpeningHours()))
                .toList();
    }

    @GetMapping("/patient/notifications/unread-count")
    public Map<String, Long> unreadNotificationCount(@AuthenticationPrincipal CustomUserDetails principal) {
        return Map.of("count", notificationService.unreadCountFor(principal.getUser().getId()));
    }

    /**
     * Lightweight polling endpoint the pharmacist reservations page calls
     * every few seconds to refresh the queue without a full page reload
     */
    @GetMapping("/pharmacist/reservations/live")
    public List<ReservationQueueItem> liveQueue(@AuthenticationPrincipal CustomUserDetails principal) {
        Long pharmacyId = principal.getUser().getPharmacy().getId();
        return reservationService.incomingQueueForPharmacy(pharmacyId);
    }
}
