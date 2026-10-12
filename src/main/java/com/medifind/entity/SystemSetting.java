package com.medifind.entity;

import jakarta.persistence.*;

/**
 * SLP: Admin Reporting & Configuration → "Configure reservation time
 * window & notification templates"
 *
 * A small key/value table so an admin can change a handful of platform
 * behaviours at runtime (e.g. how many hours a reservation stays valid)
 * without redeploying the app. This is deliberately simple — a plain
 * lookup table rather than a generic rules engine — which keeps it easy
 * to reason about for a course project while still being genuinely
 * runtime-configurable. See SystemSettingService for the typed
 * getInt/getString helpers and DataSeeder for the default rows.
 */
@Entity
@Table(name = "system_settings")
public class SystemSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "setting_key", nullable = false, unique = true, length = 100)
    private String settingKey;

    @Column(name = "setting_value", nullable = false, length = 500)
    private String settingValue;

    @Column(length = 250)
    private String description;

    public SystemSetting() {
    }

    public SystemSetting(String settingKey, String settingValue, String description) {
        this.settingKey = settingKey;
        this.settingValue = settingValue;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSettingKey() {
        return settingKey;
    }

    public void setSettingKey(String settingKey) {
        this.settingKey = settingKey;
    }

    public String getSettingValue() {
        return settingValue;
    }

    public void setSettingValue(String settingValue) {
        this.settingValue = settingValue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SystemSetting)) return false;
        SystemSetting other = (SystemSetting) o;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
