package com.medifind.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * SLP: Medicine Search & Pharmacy Discovery → "View nearby pharmacy list
 * with price, stock status, distance, hours", "Show verified-pharmacy
 * badge and last-updated timestamp"
 */
public class MedicineSearchResultRow {

    private Long medicineId;
    private String name;
    private String genericName;
    private String brand;
    private String manufacturer;
    private String categoryName;
    private String imageUrl;
    private List<PharmacyOffer> offers = new ArrayList<>();

    public Long getMedicineId() {
        return medicineId;
    }

    public void setMedicineId(Long medicineId) {
        this.medicineId = medicineId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGenericName() {
        return genericName;
    }

    public void setGenericName(String genericName) {
        this.genericName = genericName;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public List<PharmacyOffer> getOffers() {
        return offers;
    }

    public void setOffers(List<PharmacyOffer> offers) {
        this.offers = offers;
    }

    /**
     * One pharmacy's offer for the enclosing medicine: price, stock,
     * distance from the patient (when their location is known — see
     * DistanceUtil), opening hours, verified badge, and when the stock
     * figure was last updated.
     */
    public static class PharmacyOffer {
        private Long pharmacyId;
        private String pharmacyName;
        private String address;
        private boolean verified;
        private BigDecimal price;
        private int quantity;
        private boolean inStock;
        private String openingHours;
        private LocalDateTime lastUpdated;
        /** Kilometres from the patient's supplied location; null if unknown. */
        private Double distanceKm;

        public Long getPharmacyId() {
            return pharmacyId;
        }

        public void setPharmacyId(Long pharmacyId) {
            this.pharmacyId = pharmacyId;
        }

        public String getPharmacyName() {
            return pharmacyName;
        }

        public void setPharmacyName(String pharmacyName) {
            this.pharmacyName = pharmacyName;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public boolean isVerified() {
            return verified;
        }

        public void setVerified(boolean verified) {
            this.verified = verified;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public void setPrice(BigDecimal price) {
            this.price = price;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public boolean isInStock() {
            return inStock;
        }

        public void setInStock(boolean inStock) {
            this.inStock = inStock;
        }

        public String getOpeningHours() {
            return openingHours;
        }

        public void setOpeningHours(String openingHours) {
            this.openingHours = openingHours;
        }

        public LocalDateTime getLastUpdated() {
            return lastUpdated;
        }

        public void setLastUpdated(LocalDateTime lastUpdated) {
            this.lastUpdated = lastUpdated;
        }

        public Double getDistanceKm() {
            return distanceKm;
        }

        public void setDistanceKm(Double distanceKm) {
            this.distanceKm = distanceKm;
        }
    }
}
