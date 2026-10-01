package com.medifind.entity;

import com.medifind.enums.AvailabilityStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * SLP: Core Platform & Shared Engine → "Implement Multi-Pharmacy Inventory
 * Index service"
 * SLP: Pharmacy Inventory & Listing Mgmt → "Add medicine listing",
 * "Update stock quantity, price and availability", "Timestamp every stock
 * update"
 */
@Entity
@Table(name = "inventory",
        indexes = {
                @Index(name = "idx_inventory_pharmacy", columnList = "pharmacy_id"),
                @Index(name = "idx_inventory_medicine", columnList = "medicine_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_pharmacy_medicine", columnNames = {"pharmacy_id", "medicine_id"})
        })
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pharmacy_id", nullable = false)
    private Pharmacy pharmacy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicine_id", nullable = false)
    private Medicine medicine;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "availability_status", nullable = false, length = 20)
    private AvailabilityStatus availabilityStatus = AvailabilityStatus.AVAILABLE;

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    /** Optimistic-locking version — see class Javadoc above. */
    @Version
    @Column(nullable = false)
    private Integer version;

    @PrePersist
    @PreUpdate
    protected void touch() {
        this.lastUpdated = LocalDateTime.now();
    }

    public Inventory() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Pharmacy getPharmacy() {
        return pharmacy;
    }

    public void setPharmacy(Pharmacy pharmacy) {
        this.pharmacy = pharmacy;
    }

    public Medicine getMedicine() {
        return medicine;
    }

    public void setMedicine(Medicine medicine) {
        this.medicine = medicine;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public AvailabilityStatus getAvailabilityStatus() {
        return availabilityStatus;
    }

    public void setAvailabilityStatus(AvailabilityStatus availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public Integer getVersion() {
        return version;
    }

    /** True when this row should show up as purchasable to a patient. */
    @Transient
    public boolean isPurchasable() {
        return availabilityStatus == AvailabilityStatus.AVAILABLE && quantity > 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Inventory)) return false;
        Inventory other = (Inventory) o;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

