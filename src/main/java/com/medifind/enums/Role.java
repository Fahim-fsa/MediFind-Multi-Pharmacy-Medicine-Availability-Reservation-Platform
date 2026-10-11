package com.medifind.enums;

/**
 * SLP: Platform Security & Compliance
 * Story: "Role-based access control (RBAC) framework"
 * where Spring Security needs it — see {@link com.medifind.security.CustomUserDetails}.
 */
public enum Role {
    PATIENT,
    PHARMACIST,
    ADMIN
}

