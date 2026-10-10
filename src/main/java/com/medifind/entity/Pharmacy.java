package com.medifind.entity;

import com.medifind.enums.VerificationStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * SLP: Pharmacy Onboarding & Verification → "Pharmacy registration form"
 * SLP: Core Platform & Shared Engine → "Design MySQL database schema"

 */
@Entity
@Table(name = "pharmacies", indexes = {
        @Index(name = "idx_pharmacy_name", columnList = "pharmacy_name"),
        @Index(name = "idx_pharmacy_verification", columnList = "verification_status")
})
public class Pharmacy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pharmacy_name", nullable = false, length = 150)
    private String pharmacyName;

    @Column(name = "license_number", nullable = false, unique = true, length = 60)
    private String licenseNumber;

    @Column(nullable = false, length = 250)
    private String address;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false, length = 30)
    private String phone;

    /**
     * Kept as a simple free-text field (e.g. "9:00 AM - 10:00 PM, 7 days")
     * rather than a structured per-weekday schedule — a deliberate
     * simplification so the entity and its form stay easy to follow; a
     * later iteration could normalise this into its own table if the
     * project needs per-day hours.
     */
    @Column(name = "opening_hours", length = 150)
    private String openingHours;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    /** Admin note explaining a rejection, shown back to the pharmacy. */
    @Column(name = "rejection_reason", length = 250)
    private String rejectionReason;

    @OneToMany(mappedBy = "pharmacy", fetch = FetchType.LAZY)
    private List<User> staff = new ArrayList<>();

    @OneToMany(mappedBy = "pharmacy", fetch = FetchType.LAZY)
    private List<Inventory> inventoryItems = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Pharmacy() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPharmacyName() {
        return pharmacyName;
    }

    public void setPharmacyName(String pharmacyName) {
        this.pharmacyName = pharmacyName;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getOpeningHours() {
        return openingHours;
    }

    public void setOpeningHours(String openingHours) {
        this.openingHours = openingHours;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public List<User> getStaff() {
        return staff;
    }

    public void setStaff(List<User> staff) {
        this.staff = staff;
    }

    public List<Inventory> getInventoryItems() {
        return inventoryItems;
    }

    public void setInventoryItems(List<Inventory> inventoryItems) {
        this.inventoryItems = inventoryItems;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pharmacy)) return false;
        Pharmacy other = (Pharmacy) o;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
