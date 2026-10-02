package com.medifind.service;

import com.medifind.dto.MedicineSearchResultRow;
import com.medifind.entity.Category;
import com.medifind.entity.Inventory;
import com.medifind.entity.Medicine;
import com.medifind.exception.ResourceNotFoundException;
import com.medifind.repository.CategoryRepository;
import com.medifind.repository.InventoryRepository;
import com.medifind.repository.MedicineRepository;
import com.medifind.util.DistanceUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * SLP: Core Platform & Shared Engine → "Implement shared Medicine
 * Catalogue & Category service"
 * SLP: Medicine Search & Pharmacy Discovery
 *
 * Owns the shared medicine catalogue (name/generic/brand/manufacturer/
 * category — the same data every module reads) and the patient-facing
 * search that turns "a medicine name" into "which pharmacies have it, at
 * what price, how far away". Per-pharmacy stock management itself lives
 * in {@link InventoryService}; this class only reads that data to build
 * search results.
 */
@Service
public class MedicineService {

    private final MedicineRepository medicineRepository;
    private final InventoryRepository inventoryRepository;
    private final CategoryRepository categoryRepository;
    private final FileStorageService fileStorageService;

    public MedicineService(MedicineRepository medicineRepository, InventoryRepository inventoryRepository,
                           CategoryRepository categoryRepository, FileStorageService fileStorageService) {
        this.medicineRepository = medicineRepository;
        this.inventoryRepository = inventoryRepository;
        this.categoryRepository = categoryRepository;
        this.fileStorageService = fileStorageService;
    }

    public Medicine getById(Long id) {
        return medicineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medicine not found: " + id));
    }

    public List<Category> listCategories() {
        return categoryRepository.findAllByOrderByNameAsc();
    }

    public Page<Medicine> listAll(Pageable pageable) {
        return medicineRepository.findAllByOrderByNameAsc(pageable);
    }

    /**
     * SLP: Medicine Search & Pharmacy Discovery → "Search medicine by
     * name, generic name or brand", "View nearby pharmacy list with
     * price, stock status, distance, hours", "Filter results by distance
     * and availability"
     *
     * @param query        free-text search term
     * @param patientLat   patient's latitude, or null if unknown/not shared
     * @param patientLng   patient's longitude, or null if unknown/not shared
     * @param maxDistanceKm optional distance filter; null means "no limit"
     */
    @Transactional
    public List<MedicineSearchResultRow> search(String query, Double patientLat, Double patientLng, Double maxDistanceKm) {
        List<Medicine> matches = medicineRepository.searchByNameOrGenericOrBrand(query.trim());

        List<MedicineSearchResultRow> results = new ArrayList<>();
        for (Medicine medicine : matches) {
            // SLP: Admin Reporting & Configuration → "Generate platform-wide reports" (Most searched medicines)
            medicine.setSearchCount(medicine.getSearchCount() + 1);
            medicineRepository.save(medicine);

            buildRow(medicine, patientLat, patientLng, maxDistanceKm).ifPresent(results::add);
        }
        return results;
    }

    /**
     * Same offer-building logic as {@link #search}, for one already-known
     * medicine (the medicine details page) instead of a text query — and,
     * unlike {@link #search}, does not bump {@code searchCount}, since
     * opening a details page is a click-through rather than a new search.
     */
    @Transactional(readOnly = true)
    public MedicineSearchResultRow getOffersForMedicine(Long medicineId, Double patientLat, Double patientLng) {
        Medicine medicine = getById(medicineId);
        return buildRow(medicine, patientLat, patientLng, null).orElse(null);
    }

    private java.util.Optional<MedicineSearchResultRow> buildRow(Medicine medicine, Double patientLat,
                                                                 Double patientLng, Double maxDistanceKm) {
        List<Inventory> offerRows = inventoryRepository.findAvailabilityForMedicine(medicine.getId());
        if (offerRows.isEmpty()) {
            return java.util.Optional.empty(); // no pharmacy currently carries it — don't show an empty row
        }

        MedicineSearchResultRow row = new MedicineSearchResultRow();
        row.setMedicineId(medicine.getId());
        row.setName(medicine.getName());
        row.setGenericName(medicine.getGenericName());
        row.setBrand(medicine.getBrand());
        row.setManufacturer(medicine.getManufacturer());
        row.setCategoryName(medicine.getCategory() != null ? medicine.getCategory().getName() : null);
        row.setImageUrl(medicine.getImageUrl());

        List<MedicineSearchResultRow.PharmacyOffer> offers = new ArrayList<>();
        for (Inventory inv : offerRows) {
            MedicineSearchResultRow.PharmacyOffer offer = new MedicineSearchResultRow.PharmacyOffer();
            offer.setPharmacyId(inv.getPharmacy().getId());
            offer.setPharmacyName(inv.getPharmacy().getPharmacyName());
            offer.setAddress(inv.getPharmacy().getAddress());
            offer.setVerified(true); // findAvailabilityForMedicine already filters to APPROVED pharmacies
            offer.setPrice(inv.getPrice());
            offer.setQuantity(inv.getQuantity());
            offer.setInStock(inv.isPurchasable());
            offer.setOpeningHours(inv.getPharmacy().getOpeningHours());
            offer.setLastUpdated(inv.getLastUpdated());

            if (patientLat != null && patientLng != null) {
                double distance = DistanceUtil.haversineKm(patientLat, patientLng,
                        inv.getPharmacy().getLatitude(), inv.getPharmacy().getLongitude());
                offer.setDistanceKm(distance);
            }
            offers.add(offer);
        }

        if (maxDistanceKm != null) {
            offers.removeIf(o -> o.getDistanceKm() == null || o.getDistanceKm() > maxDistanceKm);
            if (offers.isEmpty()) {
                return java.util.Optional.empty();
            }
        }

        offers.sort(Comparator.comparing(
                o -> o.getDistanceKm() != null ? o.getDistanceKm() : Double.MAX_VALUE));
        row.setOffers(offers);
        return java.util.Optional.of(row);
    }

    /**
     * SLP: Pharmacy Inventory & Listing Mgmt → "Add medicine listing",
     * "Upload a photo of the medicine when adding it to the inventory"
     * (new catalogue entry case)

     */
    @Transactional
    public Medicine createIfAbsent(String name, String genericName, String brand, String manufacturer,
                                   Long categoryId, MultipartFile image) {
        if (brand != null && medicineRepository.existsByNameIgnoreCaseAndBrandIgnoreCase(name, brand)) {
            throw new com.medifind.exception.BusinessRuleException(
                    "A medicine named \"" + name + "\" with brand \"" + brand + "\" already exists in the catalogue. " +
                            "Search for it and add your stock to that entry instead.");
        }
        Medicine medicine = new Medicine();
        medicine.setName(name);
        medicine.setGenericName(genericName);
        medicine.setBrand(brand);
        medicine.setManufacturer(manufacturer);
        medicine.setImageUrl(fileStorageService.storeMedicineImage(image));
        if (categoryId != null) {
            categoryRepository.findById(categoryId).ifPresent(medicine::setCategory);
        }
        return medicineRepository.save(medicine);
    }

    public List<Medicine> mostSearched() {
        return medicineRepository.findTop10ByOrderBySearchCountDesc();
    }
}
