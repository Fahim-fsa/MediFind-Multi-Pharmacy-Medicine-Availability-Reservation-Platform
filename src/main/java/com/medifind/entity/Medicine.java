package com.medifind.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * SLP: Core Platform & Shared Engine → "Implement shared Medicine
 * Catalogue & Category service"
 * SLP: Medicine Search & Pharmacy Discovery → "Search medicine by name,
 * generic name or brand"

 */
@Entity
@Table(name = "medicines", indexes = {
        @Index(name = "idx_medicine_name", columnList = "name"),
        @Index(name = "idx_medicine_generic_name", columnList = "generic_name"),
        @Index(name = "idx_medicine_brand", columnList = "brand")
})
public class Medicine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "generic_name", nullable = false, length = 150)
    private String genericName;

    @Column(length = 100)
    private String brand;

    @Column(length = 150)
    private String manufacturer;

    /**
     * SLP: Pharmacy Inventory & Listing Mgmt → "Upload a photo of the
     * medicine when adding it to the inventory"
     *
     * A public, web-servable path (e.g. {@code /uploads/medicines/<file>})
     * pointing at the image a pharmacist uploaded when first adding this
     * medicine to the shared catalogue — see FileStorageService. Null
     * when no photo has been uploaded yet; templates fall back to a
     * generic placeholder icon in that case.
     */
    @Column(name = "image_url", length = 300)
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    /**
     * SLP: Admin Reporting & Configuration → "Generate platform-wide
     * reports" ("Most searched medicines"). Incremented once per medicine
     * each time it appears in a patient search result — see
     * MedicineService#search. A simple running counter is enough for this
     * report and avoids adding a whole separate search-log table.
     */
    @Column(name = "search_count", nullable = false)
    private long searchCount = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Medicine() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public long getSearchCount() {
        return searchCount;
    }

    public void setSearchCount(long searchCount) {
        this.searchCount = searchCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Medicine)) return false;
        Medicine other = (Medicine) o;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
