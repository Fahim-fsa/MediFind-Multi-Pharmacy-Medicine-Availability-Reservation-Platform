package com.medifind.service;

import com.medifind.dto.InventoryItemRequest;
import com.medifind.entity.Inventory;
import com.medifind.entity.Medicine;
import com.medifind.entity.Pharmacy;
import com.medifind.enums.AvailabilityStatus;
import com.medifind.exception.BusinessRuleException;
import com.medifind.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * SLP: Development Style → "Testing"
 * SLP: Pharmacy Inventory & Listing Mgmt
 */
@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock private InventoryRepository inventoryRepository;
    @Mock private MedicineService medicineService;
    @Mock private PharmacyService pharmacyService;
    @Mock private NotificationService notificationService;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private InventoryService inventoryService;

    private Pharmacy pharmacy;
    private Medicine medicine;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(inventoryService, "lowStockThreshold", 5);

        pharmacy = new Pharmacy();
        pharmacy.setId(10L);

        medicine = new Medicine();
        medicine.setId(20L);
        medicine.setName("Napa");
    }

    @Test
    void addListing_createsNewRowWhenNoneExists() {
        InventoryItemRequest request = new InventoryItemRequest();
        request.setName("Napa");
        request.setGenericName("Paracetamol");
        request.setQuantity(50);
        request.setPrice(new BigDecimal("3.50"));
        request.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);

        when(pharmacyService.getById(10L)).thenReturn(pharmacy);
        when(medicineService.createIfAbsent("Napa", "Paracetamol", null, null, null, null)).thenReturn(medicine);
        when(inventoryRepository.findByPharmacyIdAndMedicineId(10L, 20L)).thenReturn(Optional.empty());
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(inv -> inv.getArgument(0));

        Inventory result = inventoryService.addListing(10L, request);

        assertThat(result.getQuantity()).isEqualTo(50);
        assertThat(result.getPharmacy()).isEqualTo(pharmacy);
        assertThat(result.getMedicine()).isEqualTo(medicine);
    }

    @Test
    void addListing_rejectsWhenAlreadyListed() {
        InventoryItemRequest request = new InventoryItemRequest();
        request.setMedicineId(20L);
        request.setQuantity(10);
        request.setPrice(BigDecimal.TEN);
        request.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);

        Inventory existing = new Inventory();
        existing.setId(1L);

        when(pharmacyService.getById(10L)).thenReturn(pharmacy);
        when(medicineService.getById(20L)).thenReturn(medicine);
        when(inventoryRepository.findByPharmacyIdAndMedicineId(10L, 20L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> inventoryService.addListing(10L, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already listed");

        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void updateListing_rejectsWhenPharmacyDoesNotOwnListing() {
        Inventory inventory = new Inventory();
        inventory.setId(5L);
        inventory.setPharmacy(pharmacy); // pharmacy id = 10
        inventory.setMedicine(medicine);
        inventory.setQuantity(20);
        inventory.setPrice(BigDecimal.ONE);

        InventoryItemRequest request = new InventoryItemRequest();
        request.setQuantity(15);
        request.setPrice(BigDecimal.ONE);
        request.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);

        when(inventoryRepository.findById(5L)).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> inventoryService.updateListing(5L, request, 999L))
                .isInstanceOf(AccessDeniedException.class);

        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void updateListing_sendsLowStockNotificationWhenAtOrBelowThreshold() {
        Inventory inventory = new Inventory();
        inventory.setId(5L);
        inventory.setPharmacy(pharmacy);
        inventory.setMedicine(medicine);
        inventory.setQuantity(20);
        inventory.setPrice(BigDecimal.ONE);

        InventoryItemRequest request = new InventoryItemRequest();
        request.setQuantity(3); // <= lowStockThreshold (5) and > 0
        request.setPrice(new BigDecimal("4.00"));
        request.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);

        when(inventoryRepository.findById(5L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(inv -> inv.getArgument(0));

        Inventory result = inventoryService.updateListing(5L, request, 10L);

        assertThat(result.getQuantity()).isEqualTo(3);
        verify(notificationService).notifyLowStock(result);
    }
}