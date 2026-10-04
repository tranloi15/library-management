package com.library.model;

import lombok.Getter;

@Getter
public enum TargetType {
    DOCUMENT("Tài liệu"),
    BORROW_RECORD("Phiếu mượn"),
    USER("Người dùng"),
    SETTING("Cấu hình"),
    NOTIFICATION("Thông báo"),
    SYSTEM("Hệ thống");

    private final String displayName;

    TargetType(String displayName) {
        this.displayName = displayName;
    }
}
