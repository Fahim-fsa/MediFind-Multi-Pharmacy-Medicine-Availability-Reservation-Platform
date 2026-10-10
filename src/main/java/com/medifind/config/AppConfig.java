package com.medifind.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * SLP: Patient Reservation Management → "Auto-expire reservations past
 * the pickup window"
 *
 * {@code @EnableScheduling} turns on Spring's background task scheduler,
 * which is what lets {@code @Scheduled} methods (see
 * ReservationService#expireOverdueReservations) run automatically on a
 * timer without a separate cron job or external scheduler process.
 */
@Configuration
@EnableScheduling
public class AppConfig {
}
