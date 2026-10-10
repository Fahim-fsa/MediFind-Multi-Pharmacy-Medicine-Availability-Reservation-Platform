package com.medifind.enums;

/**
 * SLP: Pharmacy Onboarding & Verification
 * Story: "Admin approval workflow before publishing listings",
 *        "Issue verified-pharmacy badge on approval"
 *
 * A pharmacy starts PENDING when it registers, and cannot appear in
 * patient-facing search results or accept reservations until an admin
 * moves it to APPROVED (see AdminController → verification endpoints).
 */
public enum VerificationStatus {
    PENDING,
    APPROVED,
    REJECTED
}
