package com.library.service;

import java.util.Map;

public interface SettingService {

    String getValue(String key, String defaultValue);

    long getFinePerDay();

    int getMaxBorrowDays();

    int getMaxBorrowLimit();

    int getQrTimeoutMinutes();

    String getLibraryOpeningHours();

    String getLibraryHotline();

    String getLibraryEmail();

    String getLibraryRegulations();

    boolean isFeatureEnabled(String featureKey, boolean defaultValue);

    boolean isFeatureQrBorrowEnabled();

    Map<String, String> getAllSettingsMap();

    void updateSettings(Map<String, String> newSettings);
}
