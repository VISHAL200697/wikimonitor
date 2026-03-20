package org.qrdlife.wikiconnect.wikimonitor.service;

import lombok.RequiredArgsConstructor;
import org.qrdlife.wikiconnect.wikimonitor.model.AppSettings;
import org.qrdlife.wikiconnect.wikimonitor.repository.AppSettingsRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class SettingsService {

    private static final String AUTO_APPROVE_KEY = "auto_approve_new_users";
    private static final String MAX_ACTIVE_FILTERS_KEY = "max_active_filters_per_user";
    private final AppSettingsRepository appSettingsRepository;

    public boolean isAutoApproveEnabled() {
        return appSettingsRepository.findBySettingKey(AUTO_APPROVE_KEY)
                .map(setting -> Boolean.parseBoolean(setting.getSettingValue()))
                .orElse(false);
    }

    public void setAutoApprove(boolean enabled) {
        log.info("Setting auto-approve for new users to: {}", enabled);
        AppSettings setting = appSettingsRepository.findBySettingKey(AUTO_APPROVE_KEY)
                .orElse(new AppSettings(AUTO_APPROVE_KEY, String.valueOf(enabled)));
        setting.setSettingValue(String.valueOf(enabled));
        appSettingsRepository.save(setting);
    }

    public int getMaxActiveFiltersPerUser() {
        return appSettingsRepository.findBySettingKey(MAX_ACTIVE_FILTERS_KEY)
                .map(setting -> Integer.parseInt(setting.getSettingValue()))
                .orElse(3); // Default value
    }

    public void setMaxActiveFiltersPerUser(int maxFilters) {
        log.info("Setting max active filters per user to: {}", maxFilters);
        AppSettings setting = appSettingsRepository.findBySettingKey(MAX_ACTIVE_FILTERS_KEY)
                .orElse(new AppSettings(MAX_ACTIVE_FILTERS_KEY, String.valueOf(maxFilters)));
        setting.setSettingValue(String.valueOf(maxFilters));
        appSettingsRepository.save(setting);
    }
}
