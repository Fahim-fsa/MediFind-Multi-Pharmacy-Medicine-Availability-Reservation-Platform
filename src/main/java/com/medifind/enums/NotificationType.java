package com.medifind.enums;

/**
 * SLP: Notifications & 3rd-Party Integrations
 * Story: "Build shared Notification Service"
 *
 * Categorises each Notification row so the UI can show a different icon
 * per type and so NotificationService knows which message template to use.
 * This is a small addition beyond the minimum "id/userId/message/createdDate"
 * fields listed in the MD requirement brief — it costs one column but makes
 * a "mark all as read" / filterable notification centre possible later.
 */
public enum NotificationType {
    RESERVATION_CONFIRMATION,
    RESERVATION_STATUS_UPDATE,
    NEW_RESERVATION,
    LOW_STOCK_WARNING,
    PHARMACY_REGISTRATION_REQUEST,
    PHARMACY_APPROVED,
    PHARMACY_REJECTED,
    PASSWORD_RESET,
    MFA_CODE,
    GENERAL
}
