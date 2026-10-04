package com.library.model;

import lombok.Getter;

@Getter
public enum NotificationType {
    SYSTEM("Hệ thống", "bi-megaphone-fill", "primary"),
    OVERDUE_ALERT("Cảnh báo quá hạn", "bi-exclamation-triangle-fill", "danger"),
    BORROW_UPDATE("Mượn & Trả sách", "bi-journal-check", "info"),
    FINE_REMINDER("Nhắc phí phạt", "bi-cash-coin", "warning");

    private final String displayName;
    private final String icon;
    private final String badgeClass;

    NotificationType(String displayName, String icon, String badgeClass) {
        this.displayName = displayName;
        this.icon = icon;
        this.badgeClass = badgeClass;
    }
}
