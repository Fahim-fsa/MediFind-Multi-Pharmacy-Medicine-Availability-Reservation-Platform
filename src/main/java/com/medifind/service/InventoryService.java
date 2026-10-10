package com.medifind.service;

import com.medifind.dto.InventoryItemRequest;
import com.medifind.entity.Inventory;
import com.medifind.entity.Medicine;
import com.medifind.entity.Pharmacy;
import com.medifind.exception.BusinessRuleException;
import com.medifind.exception.ResourceNotFoundException;
import com.medifind.repository.InventoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * SLP: Pharmacy Inventory & Listing Mgmt
 * SLP: Core Platform & Shared Engine → "Implement Multi-Pharmacy
 * Inventory Index service"
 */
@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final MedicineService medicineService;
    private final PharmacyService pharmacyService;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    @Value("${medifind.inventory.low-stock-threshold:5}")
    private int lowStockThreshold;

    public InventoryService(InventoryRepository inventoryRepository, MedicineService medicineService,
                            PharmacyService pharmacyService, NotificationService notificationService,
                            AuditLogService auditLogService) {
        this.inventoryRepository = inventoryRepository;
        this.medicineService = medicineService;
        this.pharmacyService = pharmacyService;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    public Inventory getById(Long id) {
        return inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found: " + id));
    }

    /** SLP: Pharmacy Inventory & Listing Mgmt → "Per-pharmacy inventory dashboard" */
    public List<Inventory> listForPharmacy(Long pharmacyId) {
        return inventoryRepository.findByPharmacyIdOrderByLastUpdatedDesc(pharmacyId);
    }

    public List<Inventory> lowStockForPharmacy(Long pharmacyId) {
        return inventoryRepository.findByPharmacyIdAndQuantityLessThanEqual(pharmacyId, lowStockThreshold);
    }

    /** SLP: Pharmacy Inventory & Listing Mgmt → "Add medicine listing" */
    @Transactional
    public Inventory addListing(Long pharmacyId, InventoryItemRequest request) {
        Pharmacy pharmacy = pharmacyService.getById(pharmacyId);

        Medicine medicine = request.getMedicineId() != null
                ? medicineService.getById(request.getMedicineId())
                : medicineService.createIfAbsent(request.getName(), request.getGenericName(),
                request.getBrand(), request.getManufacturer(), request.getCategoryId(), request.getImage());

        if (inventoryRepository.findByPharmacyIdAndMedicineId(pharmacyId, medicine.getId()).isPresent()) {
            throw new BusinessRuleException(
                    "\"" + medicine.getName() + "\" is already listed for your pharmacy. Edit that listing instead of adding it again.");
        }

        Inventory inventory = new Inventory();
        inventory.setPharmacy(pharmacy);
        inventory.setMedicine(medicine);
        inventory.setQuantity(request.getQuantity());
        inventory.setPrice(request.getPrice());
        inventory.setAvailabilityStatus(request.getAvailabilityStatus());
        inventory = inventoryRepository.save(inventory);

        maybeWarnLowStock(inventory);
        return inventory;
    }

    /**
     * SLP: Pharmacy Inventory & Listing Mgmt → "Update stock quantity,
     * price and availability", "Timestamp every stock update", "Mark
     * medicine available/unavailable without deleting"
     * SLP: Platform Security & Compliance → "Role-based access for
     * branch data"
     */
    @Transactional
    public Inventory updateListing(Long inventoryId, InventoryItemRequest request, Long requestingPharmacyId) {
        Inventory inventory = getById(inventoryId);
        assertOwnership(inventory, requestingPharmacyId);

        inventory.setQuantity(request.getQuantity());
        inventory.setPrice(request.getPrice());
        inventory.setAvailabilityStatus(request.getAvailabilityStatus());
        inventory = inventoryRepository.save(inventory); // @PreUpdate stamps lastUpdated; @Version guards concurrent edits

        maybeWarnLowStock(inventory);
        return inventory;
    }

    /**
     * SLP: Admin Oversight & Complaint Handling → "Remove inappropriate
     * listings"
     */
    @Transactional
    public void removeListing(Long inventoryId, com.medifind.entity.User admin) {
        Inventory inventory = getById(inventoryId);
        String description = inventory.getMedicine().getName() + " @ " + inventory.getPharmacy().getPharmacyName();
        inventoryRepository.delete(inventory);
        auditLogService.record(admin, "REMOVE_LISTING: " + description, description, "REMOVED");
    }

    private void assertOwnership(Inventory inventory, Long requestingPharmacyId) {
        if (!inventory.getPharmacy().getId().equals(requestingPharmacyId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "This listing belongs to a different pharmacy.");
        }
    }

    private void maybeWarnLowStock(Inventory inventory) {
        if (inventory.getQuantity() <= lowStockThreshold && inventory.getQuantity() > 0) {
            notificationService.notifyLowStock(inventory);
        }
    }
}
