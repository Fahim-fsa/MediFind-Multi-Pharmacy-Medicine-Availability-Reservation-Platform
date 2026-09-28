package com.medifind.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * SLP: Pharmacy Onboarding & Verification → "Pharmacy registration form",
 * "Secure pharmacy staff login", "Add multiple staff logins under one
 * branch account"
 *
 */
public class RegisterPharmacistRequest {

    // --- staff account fields (always required) --------------------
    @NotBlank(message = "Full name is required")
    @Size(max = 120)
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Size(max = 30)
    private String phone;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "Please confirm your password")
    private String confirmPassword;

    // --- flow switch --------------------------------------------------
    private boolean joinExisting = false;

    // --- required only when joinExisting = true ------------------------
    private String existingLicenseNumber;

    // --- required only when joinExisting = false (new pharmacy) --------
    private String pharmacyName;
    private String licenseNumber;
    private String address;
    private Double latitude;
    private Double longitude;
    private String pharmacyPhone;
    private String openingHours;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public boolean isJoinExisting() {
        return joinExisting;
    }

    public void setJoinExisting(boolean joinExisting) {
        this.joinExisting = joinExisting;
    }

    public String getExistingLicenseNumber() {
        return existingLicenseNumber;
    }

    public void setExistingLicenseNumber(String existingLicenseNumber) {
        this.existingLicenseNumber = existingLicenseNumber;
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

    public String getPharmacyPhone() {
        return pharmacyPhone;
    }

    public void setPharmacyPhone(String pharmacyPhone) {
        this.pharmacyPhone = pharmacyPhone;
    }

    public String getOpeningHours() {
        return openingHours;
    }

    public void setOpeningHours(String openingHours) {
        this.openingHours = openingHours;
    }
}
