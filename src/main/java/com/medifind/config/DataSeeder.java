package com.medifind.config;

import com.medifind.entity.*;
import com.medifind.enums.AvailabilityStatus;
import com.medifind.enums.Role;
import com.medifind.enums.UserStatus;
import com.medifind.enums.VerificationStatus;
import com.medifind.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SLP: Core Platform & Shared Engine → "Set up 3-tier project skeleton"
 *
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final PharmacyRepository pharmacyRepository;
    private final CategoryRepository categoryRepository;
    private final MedicineRepository medicineRepository;
    private final InventoryRepository inventoryRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, PharmacyRepository pharmacyRepository,
                       CategoryRepository categoryRepository, MedicineRepository medicineRepository,
                       InventoryRepository inventoryRepository, SystemSettingRepository systemSettingRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.pharmacyRepository = pharmacyRepository;
        this.categoryRepository = categoryRepository;
        this.medicineRepository = medicineRepository;
        this.inventoryRepository = inventoryRepository;
        this.systemSettingRepository = systemSettingRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedSystemSettings();
        seedAdmin();
        Map<String, Category> categories = seedCategories();
        Map<String, Medicine> medicines = seedMedicines(categories);
        seedPharmaciesAndInventory(medicines);
        printDemoCredentials();
    }

    private void seedSystemSettings() {
        if (systemSettingRepository.count() > 0) {
            return;
        }
        systemSettingRepository.save(new SystemSetting(
                "reservation.pickup.window.hours", "24",
                "How many hours a RESERVED reservation stays valid before it auto-expires."));
        systemSettingRepository.save(new SystemSetting(
                "inventory.low.stock.threshold", "5",
                "Stock level at/below which a pharmacist receives a low-stock notification."));
        systemSettingRepository.save(new SystemSetting(
                "notification.template.reservation.confirmation",
                "Hi {patientName}, your reservation for {quantity} x {medicineName} at {pharmacyName} is confirmed. " +
                        "Show code {confirmationCode} at the counter before {pickupDeadline}.",
                "Message template used for the reservation-confirmation notification/email."));
        log.info("Seeded default system settings.");
    }

    private void seedAdmin() {
        if (userRepository.existsByEmail("admin@medifind.com")) {
            return;
        }
        User admin = new User();
        admin.setFullName("System Administrator");
        admin.setEmail("admin@medifind.com");
        admin.setPhone("+8801000000000");
        admin.setPasswordHash(passwordEncoder.encode("Admin@12345"));
        admin.setRole(Role.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        userRepository.save(admin);
        log.info("Seeded default admin account.");
    }

    private Map<String, Category> seedCategories() {
        String[][] defs = {
                {"Analgesic", "Pain and fever relief"},
                {"Antibiotic", "Bacterial infection treatment"},
                {"Antacid", "Stomach acid / gastric relief"},
                {"Antihistamine", "Allergy relief"},
                {"Antidiabetic", "Blood sugar management"},
                {"Cardiovascular", "Heart and blood pressure medicines"},
                {"Respiratory", "Asthma and airway medicines"},
                {"Vitamin & Supplement", "Vitamins and dietary supplements"}
        };
        Map<String, Category> result = new HashMap<>();
        for (String[] def : defs) {
            Category category = categoryRepository.findByNameIgnoreCase(def[0])
                    .orElseGet(() -> categoryRepository.save(new Category(def[0], def[1])));
            result.put(def[0], category);
        }
        return result;
    }

    private Map<String, Medicine> seedMedicines(Map<String, Category> categories) {
        if (medicineRepository.count() > 0) {
            Map<String, Medicine> existing = new HashMap<>();
            medicineRepository.findAll().forEach(m -> existing.put(m.getBrand(), m));
            return existing;
        }
        // {name/brand, genericName, manufacturer, category}
        String[][] defs = {
                {"Napa", "Paracetamol", "Beximco Pharmaceuticals Ltd.", "Analgesic"},
                {"Ace", "Paracetamol", "Square Pharmaceuticals Ltd.", "Analgesic"},
                {"Seclo", "Omeprazole", "Square Pharmaceuticals Ltd.", "Antacid"},
                {"Amodis", "Metronidazole", "Square Pharmaceuticals Ltd.", "Antibiotic"},
                {"Fexo", "Fexofenadine", "Square Pharmaceuticals Ltd.", "Antihistamine"},
                {"Histacin", "Chlorpheniramine Maleate", "Opsonin Pharma Ltd.", "Antihistamine"},
                {"Monas", "Montelukast", "Beximco Pharmaceuticals Ltd.", "Respiratory"},
                {"Comet", "Metformin", "ACME Laboratories Ltd.", "Antidiabetic"},
                {"Angilock", "Atenolol", "Square Pharmaceuticals Ltd.", "Cardiovascular"},
                {"Neuro-B", "Vitamin B1 + B6 + B12", "Square Pharmaceuticals Ltd.", "Vitamin & Supplement"}
        };
        Map<String, Medicine> result = new HashMap<>();
        for (String[] def : defs) {
            Medicine medicine = new Medicine();
            medicine.setName(def[0]);
            medicine.setGenericName(def[1]);
            medicine.setBrand(def[0]);
            medicine.setManufacturer(def[2]);
            medicine.setCategory(categories.get(def[3]));
            medicineRepository.save(medicine);
            result.put(def[0], medicine);
        }
        log.info("Seeded {} demo medicines.", defs.length);
        return result;
    }

    private void seedPharmaciesAndInventory(Map<String, Medicine> medicines) {
        if (pharmacyRepository.count() > 0) {
            return;
        }

        Pharmacy dhanmondi = newPharmacy("Lazz Pharma - Dhanmondi",
                "LIC-DHK-10234", "House 12, Road 27, Dhanmondi, Dhaka 1209",
                23.7461, 90.3742, "+8802-9611234", "9:00 AM - 11:00 PM (7 days)",
                VerificationStatus.APPROVED);
        Pharmacy gulshan = newPharmacy("Well Care Pharmacy - Gulshan",
                "LIC-DHK-10567", "Road 11, Gulshan 1, Dhaka 1212",
                23.7808, 90.4144, "+8802-9887654", "8:00 AM - 12:00 AM (7 days)",
                VerificationStatus.APPROVED);
        Pharmacy mirpur = newPharmacy("Popular Pharmacy - Mirpur 10",
                "LIC-DHK-11890", "Mirpur 10 Circle, Dhaka 1216",
                23.8069, 90.3687, "+8802-9012345", "24 hours",
                VerificationStatus.PENDING);

        addStaff("pharmacist1@medifind.com", "Rafiq Hasan", "+8801711000001", dhanmondi);
        addStaff("pharmacist2@medifind.com", "Nabila Karim", "+8801711000002", gulshan);
        addStaff("pharmacist3@medifind.com", "Shafiul Islam", "+8801711000003", mirpur);

        stock(dhanmondi, medicines.get("Napa"), 120, "3.50");
        stock(dhanmondi, medicines.get("Ace"), 80, "3.20");
        stock(dhanmondi, medicines.get("Seclo"), 40, "6.00");
        stock(dhanmondi, medicines.get("Fexo"), 25, "8.50");
        stock(dhanmondi, medicines.get("Comet"), 3, "2.80"); // deliberately low — demonstrates the low-stock flow
        stock(dhanmondi, medicines.get("Neuro-B"), 60, "4.10");

        stock(gulshan, medicines.get("Napa"), 200, "3.00");
        stock(gulshan, medicines.get("Seclo"), 55, "5.80");
        stock(gulshan, medicines.get("Amodis"), 30, "7.20");
        stock(gulshan, medicines.get("Monas"), 20, "35.00");
        stock(gulshan, medicines.get("Angilock"), 45, "2.50");
        stock(gulshan, medicines.get("Histacin"), 90, "1.50");

        log.info("Seeded 3 demo pharmacies (2 approved, 1 pending) with inventory.");
    }

    private Pharmacy newPharmacy(String name, String license, String address, double lat, double lng,
                                  String phone, String hours, VerificationStatus status) {
        Pharmacy pharmacy = new Pharmacy();
        pharmacy.setPharmacyName(name);
        pharmacy.setLicenseNumber(license);
        pharmacy.setAddress(address);
        pharmacy.setLatitude(lat);
        pharmacy.setLongitude(lng);
        pharmacy.setPhone(phone);
        pharmacy.setOpeningHours(hours);
        pharmacy.setVerificationStatus(status);
        return pharmacyRepository.save(pharmacy);
    }

    private void addStaff(String email, String fullName, String phone, Pharmacy pharmacy) {
        User staff = new User();
        staff.setFullName(fullName);
        staff.setEmail(email);
        staff.setPhone(phone);
        staff.setPasswordHash(passwordEncoder.encode("Pharma@12345"));
        staff.setRole(Role.PHARMACIST);
        staff.setStatus(UserStatus.ACTIVE);
        staff.setPharmacy(pharmacy);
        userRepository.save(staff);
    }

    private void stock(Pharmacy pharmacy, Medicine medicine, int quantity, String price) {
        Inventory inventory = new Inventory();
        inventory.setPharmacy(pharmacy);
        inventory.setMedicine(medicine);
        inventory.setQuantity(quantity);
        inventory.setPrice(new BigDecimal(price));
        inventory.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        inventoryRepository.save(inventory);
    }

    private void printDemoCredentials() {
        log.info("=================================================================");
        log.info(" MediFind demo accounts (see README.md for the full list):");
        log.info("   Admin:      admin@medifind.com          / Admin@12345");
        log.info("   Pharmacist: pharmacist1@medifind.com     / Pharma@12345  (Lazz Pharma, approved)");
        log.info("   Pharmacist: pharmacist3@medifind.com     / Pharma@12345  (Popular Pharmacy, pending)");
        log.info("   Patients: register your own via /auth/register");
        log.info("=================================================================");
    }
}
