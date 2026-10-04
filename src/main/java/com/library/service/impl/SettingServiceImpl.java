package com.library.service.impl;

import com.library.model.ActionType;
import com.library.model.SystemSetting;
import com.library.model.TargetType;
import com.library.repository.SystemSettingRepository;
import com.library.service.ActivityLogService;
import com.library.service.SettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettingServiceImpl implements SettingService {

    private final SystemSettingRepository systemSettingRepository;
    private final ActivityLogService activityLogService;

    @Override
    @Transactional(readOnly = true)
    public String getValue(String key, String defaultValue) {
        return systemSettingRepository.findBySettingKey(key)
                .map(SystemSetting::getSettingValue)
                .filter(v -> v != null && !v.trim().isEmpty())
                .orElse(defaultValue);
    }

    @Override
    @Transactional(readOnly = true)
    public long getFinePerDay() {
        try {
            return Long.parseLong(getValue("fine_per_day", "5000"));
        } catch (NumberFormatException e) {
            return 5000L;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public int getMaxBorrowDays() {
        try {
            return Integer.parseInt(getValue("max_borrow_days", "14"));
        } catch (NumberFormatException e) {
            return 14;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public int getMaxBorrowLimit() {
        try {
            return Integer.parseInt(getValue("max_borrow_limit", "5"));
        } catch (NumberFormatException e) {
            return 5;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public int getQrTimeoutMinutes() {
        try {
            return Integer.parseInt(getValue("qr_request_timeout", "30"));
        } catch (NumberFormatException e) {
            return 30;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String getLibraryOpeningHours() {
        return getValue("library_opening_hours", "Thứ Hai - Thứ Bảy: 08:00 - 21:00 (Nghỉ Chủ Nhật & Lễ)");
    }

    @Override
    @Transactional(readOnly = true)
    public String getLibraryHotline() {
        return getValue("library_hotline", "024.3854.4444");
    }

    @Override
    @Transactional(readOnly = true)
    public String getLibraryEmail() {
        return getValue("library_email", "hotro@thuvienso.edu.vn");
    }

    @Override
    @Transactional(readOnly = true)
    public String getLibraryRegulations() {
        return getValue("library_regulations_text",
                "1. Mỗi độc giả được mượn tối đa 5 cuốn sách trong thời hạn 14 ngày.\n" +
                "2. Độc giả có trách nhiệm bảo quản nguyên vẹn tài liệu, không viết vẽ, làm rách hoặc làm mất sách.\n" +
                "3. Trả sách quá hạn sẽ chịu mức phạt 5.000 VNĐ/ngày/cuốn.\n" +
                "4. Độc giả có thể gia hạn trực tuyến thêm 7 ngày nếu sách chưa bị quá hạn và chưa có người khác đặt trước.");
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFeatureEnabled(String featureKey, boolean defaultValue) {
        String val = getValue(featureKey, String.valueOf(defaultValue));
        return "true".equalsIgnoreCase(val) || "1".equals(val);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFeatureQrBorrowEnabled() {
        return isFeatureEnabled("feature_qr_borrow", true);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> getAllSettingsMap() {
        List<SystemSetting> list = systemSettingRepository.findAll();
        Map<String, String> map = new HashMap<>();
        for (SystemSetting setting : list) {
            map.put(setting.getSettingKey(), setting.getSettingValue());
        }
        return map;
    }

    @Override
    @Transactional
    public void updateSettings(Map<String, String> newSettings) {
        if (newSettings == null || newSettings.isEmpty()) return;

        StringBuilder changeSummary = new StringBuilder();
        int changeCount = 0;

        for (Map.Entry<String, String> entry : newSettings.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            SystemSetting setting = systemSettingRepository.findBySettingKey(key).orElse(null);
            if (setting != null) {
                if (!String.valueOf(setting.getSettingValue()).equals(value)) {
                    changeSummary.append(key).append(": '").append(setting.getSettingValue()).append("' -> '").append(value).append("'; ");
                    setting.setSettingValue(value);
                    setting.setUpdatedAt(LocalDateTime.now());
                    systemSettingRepository.save(setting);
                    changeCount++;
                }
            } else {
                SystemSetting newEntity = SystemSetting.builder()
                        .settingKey(key)
                        .settingValue(value)
                        .settingGroup(guessGroup(key))
                        .description(key)
                        .updatedAt(LocalDateTime.now())
                        .build();
                systemSettingRepository.save(newEntity);
                changeSummary.append("Thêm mới: ").append(key).append("='").append(value).append("'; ");
                changeCount++;
            }
        }

        if (changeCount > 0) {
            activityLogService.log(
                    ActionType.SETTINGS_UPDATE,
                    TargetType.SETTING,
                    null,
                    "Cấu hình hệ thống",
                    "Cập nhật " + changeCount + " thông số quy định & tính năng",
                    changeSummary.toString()
            );
        }
    }

    private String guessGroup(String key) {
        if (key.startsWith("feature_")) return "TOGGLE";
        if (key.startsWith("library_")) return "INFO";
        return "POLICY";
    }
}
