package com.medifind.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * SLP: Patient Authentication & Profile → "Forgot Password" flow via
 * email reset link
 * SLP: Pharmacy Onboarding & Verification → "Pharmacy 'Forgot Password' flow"
 *
 * Shared by every role's "forgot password" form — the flow itself
 * (generate a token, "email" a reset link, let the user pick a new
 * password) is identical for a patient, a pharmacist, or an admin.
 */
public class ForgotPasswordRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
