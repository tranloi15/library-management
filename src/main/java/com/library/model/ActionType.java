package com.library.model;

import lombok.Getter;

@Getter
public enum ActionType {
    // Nhóm lưu hành mượn trả
    BORROW_CREATE("Mượn sách mới", "BORROW"),
    BORROW_REQUEST("Yêu cầu mượn QR", "BORROW"),
    BORROW_APPROVE("Duyệt mượn sách", "BORROW"),
    BORROW_REJECT("Từ chối mượn sách", "BORROW"),
    BORROW_RETURN("Trả sách", "BORROW"),
    BORROW_EXTEND("Gia hạn mượn", "BORROW"),
    FINE_COLLECT("Thu tiền phạt", "BORROW"),

    // Nhóm quản lý kho tài liệu
    BOOK_CREATE("Thêm tài liệu mới", "DOCUMENT"),
    BOOK_UPDATE("Cập nhật tài liệu", "DOCUMENT"),
    BOOK_DELETE("Xóa tài liệu", "DOCUMENT"),
    STOCK_ADJUST("Điều chỉnh tồn kho", "DOCUMENT"),
    FILE_IMPORT("Nhập tài liệu từ file", "DOCUMENT"),

    // Nhóm tài khoản & bảo mật
    USER_CREATE("Tạo tài khoản", "USER"),
    USER_UPDATE("Cập nhật tài khoản", "USER"),
    ROLE_CHANGE("Thay đổi quyền hạn", "USER"),
    USER_LOCK("Khóa tài khoản", "USER"),
    USER_UNLOCK("Mở khóa tài khoản", "USER"),
    PASSWORD_CHANGE("Đổi mật khẩu", "USER"),

    // Nhóm quản lý hệ thống & thông báo
    SETTINGS_UPDATE("Cập nhật quy định", "SYSTEM"),
    NOTIFICATION_SEND("Gửi thông báo", "SYSTEM"),
    SYSTEM_INIT("Khởi tạo hệ thống", "SYSTEM");

    private final String description;
    private final String group;

    ActionType(String description, String group) {
        this.description = description;
        this.group = group;
    }
}
