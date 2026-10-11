package com.medifind.service;

import com.medifind.entity.Medicine;
import com.medifind.entity.Pharmacy;
import com.medifind.entity.User;
import com.medifind.enums.Role;
import com.medifind.enums.UserStatus;
import com.medifind.enums.VerificationStatus;
import com.medifind.repository.PharmacyRepository;
import com.medifind.repository.ReservationRepository;
import com.medifind.repository.UserRepository;
import com.medifind.util.PdfReportBuilder;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * SLP: Admin Reporting & Configuration → "Generate platform-wide
 * reports", "Export reports as CSV/PDF"
 */
@Service
public class ReportService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final ReservationRepository reservationRepository;
    private final PharmacyRepository pharmacyRepository;
    private final UserRepository userRepository;
    private final MedicineService medicineService;

    public ReportService(ReservationRepository reservationRepository, PharmacyRepository pharmacyRepository,
                         UserRepository userRepository, MedicineService medicineService) {
        this.reservationRepository = reservationRepository;
        this.pharmacyRepository = pharmacyRepository;
        this.userRepository = userRepository;
        this.medicineService = medicineService;
    }

    /** A report ready to render: a title, its column headers, and its body rows. */
    public record ReportTable(String title, List<String> headers, List<List<String>> rows) {
    }

    public ReportTable dailyReservationsTable(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<Object[]> raw = reservationRepository.countDailyReservationsSince(since);
        List<List<String>> rows = new ArrayList<>();
        for (Object[] r : raw) {
            rows.add(List.of(String.valueOf(r[0]), String.valueOf(r[1])));
        }
        return new ReportTable("Daily Reservations — last " + days + " days", List.of("Date", "Reservations"), rows);
    }

    public ReportTable mostSearchedMedicinesTable() {
        List<Medicine> medicines = medicineService.mostSearched();
        List<List<String>> rows = medicines.stream()
                .map(m -> List.of(m.getName(), m.getGenericName(), String.valueOf(m.getSearchCount())))
                .toList();
        return new ReportTable("Most Searched Medicines", List.of("Medicine", "Generic Name", "Times Searched"), rows);
    }

    public ReportTable activePharmaciesTable() {
        List<Pharmacy> pharmacies = pharmacyRepository.search(VerificationStatus.APPROVED, null, Pageable.unpaged()).getContent();
        List<List<String>> rows = pharmacies.stream()
                .map(p -> List.of(p.getPharmacyName(), p.getAddress(), p.getVerificationStatus().name(),
                        p.getCreatedAt().format(DATE_FORMAT)))
                .toList();
        return new ReportTable("Active (Verified) Pharmacies", List.of("Pharmacy", "Address", "Status", "Registered On"), rows);
    }

    public ReportTable activeUsersTable() {
        List<User> patients = userRepository.search(Role.PATIENT, UserStatus.ACTIVE, null, Pageable.unpaged()).getContent();
        List<List<String>> rows = patients.stream()
                .map(u -> List.of(u.getFullName(), u.getEmail(), u.getStatus().name(), u.getCreatedAt().format(DATE_FORMAT)))
                .toList();
        return new ReportTable("Active Patients", List.of("Name", "Email", "Status", "Joined On"), rows);
    }

    // --- summary counters for the dashboard cards, not exported ----------
    public long activePharmacyCount() {
        return pharmacyRepository.countByVerificationStatus(VerificationStatus.APPROVED);
    }

    public long pendingPharmacyCount() {
        return pharmacyRepository.countByVerificationStatus(VerificationStatus.PENDING);
    }

    public long activePatientCount() {
        return userRepository.countByRoleAndStatus(Role.PATIENT, UserStatus.ACTIVE);
    }

    // --- export ------------------------------------------------------------

    public byte[] exportPdf(ReportTable table) {
        return PdfReportBuilder.buildTableReport(table.title(), table.headers(), table.rows());
    }

    public byte[] exportCsv(ReportTable table) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", table.headers().stream().map(this::csvEscape).toList())).append("\r\n");
        for (List<String> row : table.rows()) {
            sb.append(String.join(",", row.stream().map(this::csvEscape).toList())).append("\r\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String csvEscape(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuoting = value.contains(",") || value.contains("\"") || value.contains("\n");
        String escaped = value.replace("\"", "\"\"");
        return needsQuoting ? "\"" + escaped + "\"" : escaped;
    }
}