-- =====================================================================
-- MediFind — reference database schema
--
-- SLP: Core Platform & Shared Engine → "Design MySQL database schema"
--
-- You do NOT need to run this file to use the app: with the default
-- "dev" profile, Hibernate reads the @Entity classes under
-- src/main/java/com/medifind/entity and creates/updates these same
-- tables automatically on startup (spring.jpa.hibernate.ddl-auto=update
-- in application-dev.properties).
--
-- This file exists as a reviewable, versioned reference — useful for
-- cross-checking against the project's ER diagram/SRS, for a
-- production setup (where application-prod.properties intentionally
-- uses ddl-auto=validate instead of auto-generating anything), or if
-- you later want to import it by hand:
--   mysql -u root -p medifind_db < database/schema.sql
-- =====================================================================

CREATE DATABASE IF NOT EXISTS medifind_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE medifind_db;

-- ---------------------------------------------------------------------
-- categories — SLP: Core Platform & Shared Engine (Medicine Catalogue)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS categories (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(250)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- pharmacies — SLP: Pharmacy Onboarding & Verification
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS pharmacies (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    pharmacy_name        VARCHAR(150) NOT NULL,
    license_number       VARCHAR(60)  NOT NULL UNIQUE,
    address              VARCHAR(250) NOT NULL,
    latitude             DOUBLE NOT NULL,
    longitude            DOUBLE NOT NULL,
    phone                VARCHAR(30) NOT NULL,
    opening_hours        VARCHAR(150),
    verification_status  VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    rejection_reason     VARCHAR(250),
    created_at           DATETIME NOT NULL,
    updated_at           DATETIME NOT NULL,
    INDEX idx_pharmacy_name (pharmacy_name),
    INDEX idx_pharmacy_verification (verification_status)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- users — Patient, Pharmacist and Admin accounts (SLP: Patient
-- Authentication & Profile / Pharmacy Onboarding & Verification /
-- Admin Account & Verification Mgmt — see the User entity's Javadoc)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name               VARCHAR(120) NOT NULL,
    email                    VARCHAR(150) NOT NULL UNIQUE,
    phone                    VARCHAR(30)  NOT NULL,
    password_hash            VARCHAR(100) NOT NULL,
    role                     VARCHAR(20)  NOT NULL,
    status                   VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    pharmacy_id              BIGINT NULL,
    failed_login_attempts    INT NOT NULL DEFAULT 0,
    locked_until             DATETIME NULL,
    reset_token              VARCHAR(100) NULL,
    reset_token_expiry       DATETIME NULL,
    mfa_code                 VARCHAR(10) NULL,
    mfa_code_expiry          DATETIME NULL,
    created_at               DATETIME NOT NULL,
    updated_at               DATETIME NOT NULL,
    CONSTRAINT fk_user_pharmacy FOREIGN KEY (pharmacy_id) REFERENCES pharmacies (id),
    INDEX idx_user_email (email),
    INDEX idx_user_role (role)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- medicines — shared catalogue (SLP: Medicine Search & Pharmacy Discovery)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS medicines (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(150) NOT NULL,
    generic_name  VARCHAR(150) NOT NULL,
    brand         VARCHAR(100),
    manufacturer  VARCHAR(150),
    image_url     VARCHAR(300),
    category_id   BIGINT NULL,
    search_count  BIGINT NOT NULL DEFAULT 0,
    created_at    DATETIME NOT NULL,
    CONSTRAINT fk_medicine_category FOREIGN KEY (category_id) REFERENCES categories (id),
    INDEX idx_medicine_name (name),
    INDEX idx_medicine_generic_name (generic_name),
    INDEX idx_medicine_brand (brand)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- inventory — per-pharmacy stock/price (SLP: Pharmacy Inventory & Listing Mgmt)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS inventory (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    pharmacy_id           BIGINT NOT NULL,
    medicine_id           BIGINT NOT NULL,
    quantity              INT NOT NULL,
    price                 DECIMAL(10,2) NOT NULL,
    availability_status   VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    last_updated          DATETIME NOT NULL,
    version               INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_inventory_pharmacy FOREIGN KEY (pharmacy_id) REFERENCES pharmacies (id),
    CONSTRAINT fk_inventory_medicine FOREIGN KEY (medicine_id) REFERENCES medicines (id),
    CONSTRAINT uq_pharmacy_medicine UNIQUE (pharmacy_id, medicine_id),
    INDEX idx_inventory_pharmacy (pharmacy_id),
    INDEX idx_inventory_medicine (medicine_id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- reservations — SLP: Core Platform & Shared Engine (Reservation Ledger)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS reservations (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id           BIGINT NOT NULL,
    pharmacy_id          BIGINT NOT NULL,
    medicine_id          BIGINT NOT NULL,
    quantity             INT NOT NULL,
    confirmation_code    VARCHAR(20) NOT NULL UNIQUE,
    status               VARCHAR(20) NOT NULL DEFAULT 'RESERVED',
    reserved_at          DATETIME NOT NULL,
    pickup_deadline      DATETIME NOT NULL,
    collected_at         DATETIME NULL,
    cancelled_at         DATETIME NULL,
    CONSTRAINT fk_reservation_patient  FOREIGN KEY (patient_id)  REFERENCES users (id),
    CONSTRAINT fk_reservation_pharmacy FOREIGN KEY (pharmacy_id) REFERENCES pharmacies (id),
    CONSTRAINT fk_reservation_medicine FOREIGN KEY (medicine_id) REFERENCES medicines (id),
    INDEX idx_reservation_patient (patient_id),
    INDEX idx_reservation_pharmacy (pharmacy_id),
    INDEX idx_reservation_status (status)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- payments — SLP: Core Platform & Shared Engine (Payment structure, MVP scope)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS payments (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    reservation_id   BIGINT NOT NULL UNIQUE,
    amount           DECIMAL(10,2) NOT NULL,
    payment_status   VARCHAR(20) NOT NULL,
    payment_method   VARCHAR(20) NOT NULL,
    created_at       DATETIME NOT NULL,
    CONSTRAINT fk_payment_reservation FOREIGN KEY (reservation_id) REFERENCES reservations (id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- notifications — SLP: Notifications & 3rd-Party Integrations
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notifications (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT NOT NULL,
    message       VARCHAR(500) NOT NULL,
    type          VARCHAR(40) NOT NULL,
    is_read       BOOLEAN NOT NULL DEFAULT FALSE,
    created_date  DATETIME NOT NULL,
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX idx_notification_user (user_id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- audit_logs — SLP: Platform Security & Compliance (audit logging)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS audit_logs (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    admin_id    BIGINT NULL,
    action      VARCHAR(150) NOT NULL,
    old_value   TEXT NULL,
    new_value   TEXT NULL,
    timestamp   DATETIME NOT NULL,
    CONSTRAINT fk_audit_admin FOREIGN KEY (admin_id) REFERENCES users (id),
    INDEX idx_audit_admin (admin_id),
    INDEX idx_audit_timestamp (timestamp)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- system_settings — SLP: Admin Reporting & Configuration
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS system_settings (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    setting_key    VARCHAR(100) NOT NULL UNIQUE,
    setting_value  VARCHAR(500) NOT NULL,
    description    VARCHAR(250)
) ENGINE=InnoDB;
