package com.medifind.dto;

import com.medifind.enums.AvailabilityStatus;
import jakarta.validation.constraints.*;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;

/**
 * SLP: Pharmacy Inventory & Listing Mgmt → "Add medicine listing",
 * "Update stock quantity, price and availability"
 */
public class InventoryItemRequest {

    private Long medicineId;

    // --- required only when medicineId is null (new catalogue entry) ---
    private String name;
    private String genericName;
    private String brand;
    private String manufacturer;
    private Long categoryId;

    /**
     * SLP: Pharmacy Inventory & Listing Mgmt → "Upload a photo of the
     * medicine when adding it to the inventory"
     */
    private MultipartFile image;

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity cannot be negative")
    private Integer quantity;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price cannot be negative")
    private BigDecimal price;

    @NotNull
    private AvailabilityStatus availabilityStatus = AvailabilityStatus.AVAILABLE;

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

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public MultipartFile getImage() {
        return image;
    }

    public void setImage(MultipartFile image) {
        this.image = image;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
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
}
