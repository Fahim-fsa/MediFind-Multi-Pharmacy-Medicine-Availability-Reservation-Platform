package com.medifind.dto;

import com.medifind.enums.PaymentMethod;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * SLP: Patient Reservation Management → "Reserve medicine at a chosen
 * pharmacy"
 *
 */
public class ReservationRequest {

    @NotNull(message = "Please choose a pharmacy")
    private Long pharmacyId;

    @NotNull(message = "Please choose a medicine")
    private Long medicineId;

    @NotNull
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @NotNull(message = "Please choose a payment method")
    private PaymentMethod paymentMethod;

    public Long getPharmacyId() {
        return pharmacyId;
    }

    public void setPharmacyId(Long pharmacyId) {
        this.pharmacyId = pharmacyId;
    }

    public Long getMedicineId() {
        return medicineId;
    }

    public void setMedicineId(Long medicineId) {
        this.medicineId = medicineId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}
