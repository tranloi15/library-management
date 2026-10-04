package com.library.model;

import lombok.Getter;

@Getter
public enum BorrowStatus {
    PENDING("Chờ duyệt tại quầy", "warning", "bi-hourglass-split"),
    BORROWING("Đang mượn", "primary", "bi-journal-arrow-up"),
    RETURNED("Đã trả", "success", "bi-check2-circle"),
    OVERDUE("Quá hạn", "danger", "bi-exclamation-triangle-fill"),
    REJECTED("Bị từ chối", "secondary", "bi-x-circle"),
    CANCELLED("Đã hủy", "secondary", "bi-slash-circle");

    private final String displayName;
    private final String badgeClass;
    private final String icon;

    BorrowStatus(String displayName, String badgeClass, String icon) {
        this.displayName = displayName;
        this.badgeClass = badgeClass;
        this.icon = icon;
    }
}