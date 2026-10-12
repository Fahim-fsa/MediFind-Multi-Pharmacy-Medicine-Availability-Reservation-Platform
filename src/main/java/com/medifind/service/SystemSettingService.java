package com.medifind.service;

import com.medifind.entity.SystemSetting;
import com.medifind.repository.SystemSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * SLP: Admin Reporting & Configuration → "Configure reservation time
 * window & notification templates"
 *
 * Typed read/write access to the {@link SystemSetting} key-value table,
 * with a safe fallback when a key has not been seeded yet (defensive
 * programming — the app still works even against a database where
 * DataSeeder never ran).
 */
@Service
public class SystemSettingService {

    private final SystemSettingRepository systemSettingRepository;

    public SystemSettingService(SystemSettingRepository systemSettingRepository) {
        this.systemSettingRepository = systemSettingRepository;
    }

    public int getInt(String key, int defaultValue) {
        return systemSettingRepository.findBySettingKey(key)
                .map(s -> {
                    try {
                        return Integer.parseInt(s.getSettingValue().trim());
                    } catch (NumberFormatException e) {
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }

    public String getString(String key, String defaultValue) {
        return systemSettingRepository.findBySettingKey(key)
                .map(SystemSetting::getSettingValue)
                .orElse(defaultValue);
    }

    public List<SystemSetting> findAll() {
        return systemSettingRepository.findAll();
    }

    /**
     * SLP: Admin Reporting & Configuration → "Log configuration changes
     * with before/after values" — the caller (AdminController) passes
     * the old/new pair on to AuditLogService itself so both concerns
     * (changing the value, and recording that it changed) stay separate.
     */
    @Transactional
    public SystemSetting update(Long id, String newValue) {
        SystemSetting setting = systemSettingRepository.findById(id)
                .orElseThrow(() -> new com.medifind.exception.ResourceNotFoundException("Setting not found: " + id));
        setting.setSettingValue(newValue);
        return systemSettingRepository.save(setting);
    }
}
