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
}
