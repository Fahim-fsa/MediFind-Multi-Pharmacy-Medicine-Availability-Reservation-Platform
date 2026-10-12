package com.medifind.repository;

import com.medifind.entity.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * SLP: Admin Reporting & Configuration → "Configure reservation time
 * window & notification templates"
 */
public interface SystemSettingRepository extends JpaRepository<SystemSetting, Long> {

    Optional<SystemSetting> findBySettingKey(String settingKey);
}
