package com.medifind.util;

import java.security.SecureRandom;

/**
 * SLP: Patient Reservation Management → "Generate & display reservation
 * confirmation code"
 * SLP: Admin Account & Verification Mgmt → "Multi-factor authentication
 * for admin accounts"
 */
public final class CodeGenerator {

    private static final String ALPHANUMERIC = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no 0/O/1/I to avoid confusion
    private static final SecureRandom RANDOM = new SecureRandom();

    private CodeGenerator() {
    }

    /** e.g. "MF-7K3QF2" — shown to the patient and read back at pickup. */
    public static String reservationConfirmationCode() {
        return "MF-" + randomAlphanumeric(6);
    }

    /** A 6-digit numeric one-time code for admin MFA / password resets. */
    public static String sixDigitOtp() {
        int value = 100000 + RANDOM.nextInt(900000);
        return String.valueOf(value);
    }

    /** A longer opaque token suitable for a password-reset email link. */
    public static String urlSafeToken() {
        return randomAlphanumeric(32);
    }

    private static String randomAlphanumeric(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }
}
