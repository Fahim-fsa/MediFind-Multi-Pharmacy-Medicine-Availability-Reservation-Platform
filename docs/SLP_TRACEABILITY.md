# SLP Traceability — Jira Backlog → Code

This maps every story in `MediFind_Jira__Stories.csv` (78 stories across 12 Epics) to the file(s) that implement it. The uploaded sheet had no column literally named "SLP", so this document — and the matching `// SLP: <Epic> → "<Story>"` comments throughout the code — treat "SLP" as shorthand for that sheet's **Epic + Story**, since that's the closest thing to a backlog reference the uploaded data actually defines. If your course materials use "SLP" for something more specific, every tag below already names the exact story it corresponds to, so relabelling is a find-and-replace, not a re-read of the code.

Status legend: ✅ implemented · ⚠️ implemented with a caveat (see note) · ❌ not implemented in this pass

---

## Core Platform & Shared Engine

| Story | Status | Where |
|---|---|---|
| Set up 3-tier project skeleton (Spring Boot, MVC, REST) | ✅ | `pom.xml`, `MedifindApplication.java`, the `controller/service/repository/entity` package split |
| Provision hosting/deployment infrastructure | ⚠️ | `application-prod.properties` + README §3/§9 provide the *configuration*; actually provisioning a server/cloud account is a deployment-time step for your team, not something generated code can do |
| Design MySQL database schema | ✅ | `entity/*.java` (JPA mapping) and `database/schema.sql` (plain-SQL reference copy) |
| Configure JPA/Hibernate data access layer | ✅ | `repository/*.java`, `application-dev.properties` (`spring.jpa.*`) |
| Set up Git repository & branching strategy | ❌ | Process/workflow task for your team — not something that lives in the codebase itself |
| Implement shared Medicine Catalogue & Category service | ✅ | `MedicineService.java`, `CategoryRepository.java` |
| Implement Multi-Pharmacy Inventory Index service | ✅ | `InventoryService.java`, `InventoryRepository.java` |
| Implement Reservation Ledger core service | ✅ | `ReservationService.java` (see its class Javadoc for the stock-holding design decision) |
| Set up CI pipeline and environment configs | ✅ | `.github/workflows/ci.yml` (runs `mvn verify` on push/PR) + `application-dev/prod.properties` (per-environment config) |
| Configure responsive front-end framework | ✅ | `static/css/style.css` — a hand-written responsive design system rather than a third-party framework (see the file's own header comment for the design rationale) |

## Medicine Search & Pharmacy Discovery

| Story | Status | Where |
|---|---|---|
| Search medicine by name, generic name or brand | ✅ | `MedicineRepository.searchByNameOrGenericOrBrand`, `MedicineService.search`, `PatientController.search`, `templates/patient/search.html` |
| Display pharmacies reporting availability | ✅ | `InventoryRepository.findAvailabilityForMedicine`, `MedicineService.buildRow` |
| Pharmacy details page | ✅ | `PatientController.pharmacyDetails`, `templates/patient/pharmacy-details.html` |
| Integrate mapping/geolocation service | ✅ | `ApiController.pharmacyMapPoints`, `static/js/map.js` (Leaflet + OpenStreetMap), `static/js/search.js` (browser Geolocation API) |
| Show verified-pharmacy badge and last-updated timestamp | ✅ | `MedicineSearchResultRow.PharmacyOffer` (`verified`, `lastUpdated`), rendered in `search.html`/`medicine-details.html` |
| Filter results by distance and availability | ✅ | `MedicineService.search(query, lat, lng, maxDistanceKm)`, `DistanceUtil` (Haversine) |
| View nearby pharmacy list with price, stock status, distance, hours | ✅ | `MedicineSearchResultRow`, `templates/patient/search.html` |
| Optimise search & feed response time | ✅ | `@Index` annotations on `Medicine`/`Pharmacy`/`Inventory` (see each entity's `@Table(indexes = ...)`) |

## Patient Authentication & Profile

| Story | Status | Where |
|---|---|---|
| Patient registration with email or phone | ⚠️ | `AuthService.registerPatient` — simplified to email as the single login identifier, phone stored as an additional contact field (see `RegisterPatientRequest`'s Javadoc-style comment) |
| Secure patient login | ✅ | `AuthService.login`, `AuthController` (`/auth/login`) |
| "Forgot Password" flow via email reset link | ✅ | `AuthService.forgotPassword`/`resetPassword`, `templates/auth/forgot-password.html` + `reset-password.html` |
| Patient profile view & edit | ✅ | `PatientController.profile`/`updateProfile`, `templates/patient/profile.html` |
| Rate limiting on patient login attempts | ✅ | `User.failedLoginAttempts`/`lockedUntil`, `AuthService.recordFailedAttempt`, `CustomUserDetails.isAccountNonLocked` |
| Session/token management for patient login | ✅ | `AuthService.establishSecurityContext` (session-based — see `SecurityConfig`'s class comment for why session over JWT was chosen for a server-rendered app) |

## Patient Reservation Management

| Story | Status | Where |
|---|---|---|
| Generate & display reservation confirmation code | ✅ | `CodeGenerator.reservationConfirmationCode`, `ReservationService.generateUniqueConfirmationCode` |
| Reserve medicine at a chosen pharmacy | ✅ | `ReservationService.reserve`, `PatientController.reserve` |
| Real-time listing status update to Reserved | ✅ | Stock is decremented synchronously inside `ReservationService.reserve` (immediate, not eventually-consistent) |
| Cancel reservation before pickup window expires | ✅ | `ReservationService.cancel`, `PatientController.cancelReservation` |
| Reservation history with date/status filters | ✅ | `ReservationRepository.findHistoryForPatient`, `templates/patient/reservations.html` |
| View pickup details for an active reservation | ✅ | `templates/patient/reservations.html` and the confirmation flash message after reserving (confirmation code + pickup deadline) |
| Auto-expire reservations past the pickup window | ✅ | `ReservationService.expireOverdueReservations` (`@Scheduled`), enabled via `AppConfig`'s `@EnableScheduling` |

## Pharmacy Onboarding & Verification

| Story | Status | Where |
|---|---|---|
| Admin approval workflow before publishing listings | ✅ | `PharmacyService.approve`/`reject`, `AdminController` (`/admin/verification/**`) |
| Pharmacy registration form | ✅ | `AuthService.registerPharmacist`, `templates/auth/register-pharmacist.html` |
| Issue verified-pharmacy badge on approval | ✅ | `VerificationStatus.APPROVED` gates the `badge-verified` UI element everywhere a pharmacy is shown to a patient |
| Pharmacy "Forgot Password" flow | ✅ | Shared with patients — `AuthService.forgotPassword` works for every role |
| Add multiple staff logins under one branch account | ✅ | `RegisterPharmacistRequest.joinExisting`/`existingLicenseNumber`, `AuthService.registerPharmacist` |
| Secure pharmacy staff login | ✅ | Same `AuthService.login` path as patients, gated to `ROLE_PHARMACIST` pages by `SecurityConfig` |

## Pharmacy Inventory & Listing Mgmt

| Story | Status | Where |
|---|---|---|
| Timestamp every stock update | ✅ | `Inventory.touch()` (`@PrePersist`/`@PreUpdate`) |
| Update stock quantity, price and availability | ✅ | `InventoryService.updateListing`, `templates/pharmacist/inventory.html` |
| Mark medicine available/unavailable without deleting | ✅ | `AvailabilityStatus` enum + `Inventory.isPurchasable()` |
| Per-pharmacy inventory dashboard | ✅ | `InventoryService.listForPharmacy`, `templates/pharmacist/inventory.html` |
| Role-based access for branch data | ✅ | `InventoryService.assertOwnership`, `PharmacistController.pharmacyOf` (every action scoped to the logged-in staff member's own `Pharmacy`) |
| Add medicine listing | ✅ | `InventoryService.addListing` |

## Pharmacy Reservation Fulfilment

| Story | Status | Where |
|---|---|---|
| Notify staff of stock nearing zero | ✅ | `InventoryService.maybeWarnLowStock`, `NotificationService.notifyLowStock` |
| Notify staff of new reservations | ✅ | `NotificationService.notifyNewReservationToPharmacyStaff` |
| Mark reservation as Collected | ✅ | `ReservationService.markCollected`, `PharmacistController.collect` |
| View incoming reservations in real time | ⚠️ | `ReservationService.incomingQueueForPharmacy` (server-rendered) + `ApiController.liveQueue`/`static/js/reservation-poll.js` polls every 20s and updates a live count badge — a lighter-weight "real time" than WebSockets, a deliberate simplicity trade-off (see `ApiController`'s Javadoc) |
| Reservation queue in chronological order | ✅ | `ReservationRepository.findByPharmacyIdAndStatusOrderByReservedAtAsc` |

## Admin Account & Verification Mgmt

| Story | Status | Where |
|---|---|---|
| Secure admin login with role-based access | ✅ | `AuthService.adminLoginStep1`/`completeAdminMfa`, `templates/auth/admin-login.html` |
| Review and approve/reject pharmacy accounts | ✅ | `AdminController` (`/admin/verification/**`), `templates/admin/verification.html` |
| View and suspend patient accounts | ✅ | `UserService.setStatus`, `AdminController.suspend`/`activate` |
| Admin session timeout after inactivity | ✅ | `server.servlet.session.timeout` in `application.properties` |
| Multi-factor authentication for admin accounts | ✅ | `AuthService.adminLoginStep1`/`completeAdminMfa` (emailed 6-digit code — simulated per §6 of the README), `templates/auth/admin-mfa.html` |

## Admin Oversight & Complaint Handling

| Story | Status | Where |
|---|---|---|
| View all active listings with search and filter *(listed twice in the sheet, same story)* | ✅ | `PharmacyService.search`, `AdminController.listings`, `templates/admin/listings.html` |
| View and cancel a disputed reservation | ✅ | `ReservationService.adminCancel`, `AdminController.cancelReservation` |
| Review and resolve complaints | ❌ | No dedicated `Complaint` entity/ticketing workflow exists. The closest existing tools are disputed-reservation cancellation and listing removal (below), which cover the most common concrete complaint scenarios — a full complaint-ticket system (its own entity, status, resolution notes) would be a good next feature to add |
| Confirmation & audit logging for critical admin actions | ✅ | `AuditLogService.record`, called from every sensitive admin action (approve/reject pharmacy, suspend/reactivate user, cancel reservation, remove listing, change a setting) |

*(Also: "Remove inappropriate listings" from the requirement brief's Admin Dashboard prompt, not its own CSV row, is implemented at `InventoryService.removeListing` / `templates/admin/pharmacy-inventory.html`.)*

## Admin Reporting & Configuration

| Story | Status | Where |
|---|---|---|
| Generate platform-wide reports | ✅ | `ReportService` (`dailyReservationsTable`, `mostSearchedMedicinesTable`, `activePharmaciesTable`, `activeUsersTable`) |
| Export reports as CSV/PDF | ✅ | `ReportService.exportCsv`/`exportPdf`, `PdfReportBuilder` (OpenPDF), `AdminController.exportReport` |
| Configure reservation time window & notification templates | ✅ | `SystemSetting` entity, `SystemSettingService`, `templates/admin/settings.html` |
| Log configuration changes with before/after values | ✅ | `AdminController.updateSetting` records old/new values via `AuditLogService` |
| Age-flagged pharmacy verification queue | ✅ | `templates/admin/verification.html` shows "Waiting since \<date\>" per pharmacy (sorted oldest-first by `findByVerificationStatusOrderByCreatedAtAsc`) |
| Sortable/filterable/exportable admin data tables | ✅ | Search/filter query params across `admin/users`, `admin/listings`, `admin/reservations`; export covered above |

## Notifications & 3rd-Party Integrations

| Story | Status | Where |
|---|---|---|
| Reservation status-change notifications | ✅ | `NotificationService.notifyReservationStatusUpdate` |
| Reservation confirmation notification | ✅ | `NotificationService.notifyReservationConfirmation` |
| Build shared Notification Service | ✅ | `NotificationService.java` |
| Low-stock notification to pharmacy staff | ✅ | `NotificationService.notifyLowStock` |
| Integrate mapping/geolocation provider (shared) *(same feature as the Search epic's mapping story)* | ✅ | `static/js/map.js` |
| Integrate SMS/email provider infrastructure | ⚠️ | `EmailSimulationService` — deliberately simulated per the requirement brief's own "email notification simulation" instruction (see README §6); swapping in a real provider is a one-file change |

## Platform Security & Compliance

| Story | Status | Where |
|---|---|---|
| System monitoring & threat protection | ⚠️ | Spring Boot Actuator `/actuator/health` + `/actuator/info` (`application.properties`) — a course-project-appropriate baseline, not a full APM/WAF |
| Limit patient data exposed during reservation | ✅ | `ReservationQueueItem` DTO (masked phone, no email/id exposed), `ReservationService.toQueueItem`/`maskPhone` |
| Automated database backup & recovery | ⚠️ | `scripts/backup.sh`/`restore.sh` (mysqldump wrappers) — "automated" in the sense of being one command t#o run from cron/Task Scheduler, not a managed cloud backup service |
| Platform-wide audit logging | ✅ | `AuditLog` entity, `AuditLogService`, `templates/admin/audit-log.html` |
| Role-based access control (RBAC) framework | ✅ | `Role` enum, `CustomUserDetails.getAuthorities`, `SecurityConfig.authorizeHttpRequests` |
| Enforce HTTPS/TLS across the platform | ⚠️ | Commented-out `server.ssl.*` block in `application-prod.properties`, ready to enable with a real certificate — not turned on by default since that needs a cert this project can't generate for you (see README §7) |
| Prevent duplicate/incorrect stock updates | ✅ | `Inventory.version` (`@Version` — JPA optimistic locking) |
| Password hashing for all user types | ✅ | `SecurityConfig.passwordEncoder` (BCrypt), used identically for Patient/Pharmacist/Admin in `AuthService` |
