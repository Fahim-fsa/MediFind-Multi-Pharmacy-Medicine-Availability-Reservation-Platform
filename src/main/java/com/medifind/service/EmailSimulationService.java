package com.medifind.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * SLP: Notifications & 3rd-Party Integrations → "Integrate SMS/email
 * provider infrastructure"
 *
 * The requirement brief explicitly scopes this as "email notification
 * simulation" for the MVP — MediFind does not hold real SMTP/SMS
 * provider credentials, so no email actually leaves the server. Instead,
 * every "send" is logged clearly (and shown on-screen for password-reset
 * / MFA codes in dev — see AuthController) so the whole flow can be
 * demonstrated and tested without a mail server.
 *
 * Swapping in a real provider later only means changing what happens
 * *inside* {@link #send}: everywhere else in the app already calls this
 * one method, so nothing else needs to change. A production version
 * would inject a {@code JavaMailSender} (SMTP) or an SDK client
 * (SendGrid/Twilio/etc.) here instead of logging.
 */
@Service
public class EmailSimulationService {

    private static final Logger log = LoggerFactory.getLogger(EmailSimulationService.class);

    public void send(String toAddress, String subject, String body) {
        log.info("---------------------------------------------------------------");
        log.info("[SIMULATED EMAIL] To: {}", toAddress);
        log.info("[SIMULATED EMAIL] Subject: {}", subject);
        log.info("[SIMULATED EMAIL] Body: {}", body);
        log.info("---------------------------------------------------------------");
    }
}