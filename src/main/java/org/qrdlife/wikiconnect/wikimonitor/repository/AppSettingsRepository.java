package org.qrdlife.wikiconnect.wikimonitor.repository;

import org.qrdlife.wikiconnect.wikimonitor.model.AppSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppSettingsRepository extends JpaRepository<AppSettings, Long> {
    Optional<AppSettings> findBySettingKey(String settingKey);
}
