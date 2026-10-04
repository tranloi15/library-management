package com.library.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "activity_logs", indexes = {
    @Index(name = "idx_act_created_at", columnList = "created_at"),
    @Index(name = "idx_act_action_type", columnList = "action_type"),
    @Index(name = "idx_act_target_type", columnList = "target_type"),
    @Index(name = "idx_act_username", columnList = "username")
})
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(name = "user_full_name", nullable = false, length = 150)
    private String userFullName;

    @Column(name = "user_role", nullable = false, length = 50)
    private String userRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    private ActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 50)
    private TargetType targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "target_name", length = 255)
    private String targetName;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public String getActionBadgeClass() {
        if (actionType == null) return "bg-secondary-subtle text-secondary";
        return switch (actionType) {
            case BORROW_CREATE, BORROW_APPROVE, BOOK_CREATE, USER_CREATE, USER_UNLOCK ->
                "bg-success-subtle text-success border border-success-subtle";
            case BORROW_RETURN, BOOK_UPDATE, USER_UPDATE, BORROW_EXTEND, PASSWORD_CHANGE ->
                "bg-primary-subtle text-primary border border-primary-subtle";
            case FINE_COLLECT, STOCK_ADJUST, SETTINGS_UPDATE, NOTIFICATION_SEND ->
                "bg-warning-subtle text-warning-emphasis border border-warning-subtle";
            case BORROW_REJECT, BOOK_DELETE, USER_LOCK, ROLE_CHANGE ->
                "bg-danger-subtle text-danger border border-danger-subtle";
            default -> "bg-secondary-subtle text-secondary border border-secondary-subtle";
        };
    }

    public String getActionIcon() {
        if (actionType == null) return "bi-activity";
        return switch (actionType) {
            case BORROW_CREATE, BORROW_REQUEST -> "bi-journal-arrow-down";
            case BORROW_APPROVE -> "bi-check2-circle";
            case BORROW_REJECT -> "bi-x-circle";
            case BORROW_RETURN -> "bi-journal-arrow-up";
            case BORROW_EXTEND -> "bi-arrow-clockwise";
            case FINE_COLLECT -> "bi-cash-coin";
            case BOOK_CREATE -> "bi-journal-plus";
            case BOOK_UPDATE -> "bi-pencil-square";
            case BOOK_DELETE -> "bi-trash3";
            case STOCK_ADJUST -> "bi-boxes";
            case FILE_IMPORT -> "bi-file-earmark-spreadsheet";
            case USER_CREATE -> "bi-person-plus";
            case USER_UPDATE -> "bi-person-gear";
            case ROLE_CHANGE -> "bi-shield-check";
            case USER_LOCK -> "bi-lock";
            case USER_UNLOCK -> "bi-unlock";
            case PASSWORD_CHANGE -> "bi-key";
            case SETTINGS_UPDATE -> "bi-sliders";
            case NOTIFICATION_SEND -> "bi-bell";
            case SYSTEM_INIT -> "bi-cpu";
        };
    }
}
