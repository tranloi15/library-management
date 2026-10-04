package com.library.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "borrow_records")
public class BorrowRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "book_id", insertable = false, updatable = false)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    private LocalDate borrowDate;
    private LocalDate dueDate;
    private LocalDate returnDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BorrowStatus status;

    private Long fineAmount = 0L;

    private String paymentMethod; // "CASH", "QR_CODE", "NONE"

    @Column(length = 500)
    private String note;

    /**
     * Phương thức khởi tạo yêu cầu: "QR_SELF_SERVICE" hoặc "DESK_MANUAL"
     */
    private String requestType;

    /**
     * Thời điểm hết hạn giữ sách chờ duyệt tại quầy (TTL)
     */
    private java.time.LocalDateTime requestExpiresAt;

    private String rejectReason;

    private java.time.LocalDateTime approvedAt;

    private String approvedBy;

    public boolean isRequestExpired() {
        if (status != BorrowStatus.PENDING) {
            return false;
        }
        if (requestExpiresAt != null) {
            return java.time.LocalDateTime.now().isAfter(requestExpiresAt);
        }
        return false;
    }

    public void approveRequest(int borrowDays, String managerName) {
        this.status = BorrowStatus.BORROWING;
        this.borrowDate = LocalDate.now();
        this.dueDate = LocalDate.now().plusDays(borrowDays);
        this.approvedAt = java.time.LocalDateTime.now();
        this.approvedBy = managerName;
    }

    public void rejectRequest(String reason, String managerName) {
        this.status = BorrowStatus.REJECTED;
        this.rejectReason = reason;
        this.approvedBy = managerName;
    }

    public void cancel(String cancelNote) {
        this.status = BorrowStatus.CANCELLED;
        this.note = cancelNote;
    }

    public boolean markOverdueIfApplicable() {
        if (this.status == BorrowStatus.BORROWING && isOverdue()) {
            this.status = BorrowStatus.OVERDUE;
            return true;
        }
        return false;
    }

    public BorrowRecord(Long bookId, Long userId, LocalDate borrowDate, LocalDate dueDate, BorrowStatus status) {
        this.bookId = bookId;
        this.userId = userId;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.status = status;
    }

    public BorrowRecord(
            Long bookId,
            Long userId,
            LocalDate borrowDate,
            LocalDate dueDate,
            LocalDate returnDate,
            BorrowStatus status) {

        this(bookId, userId, borrowDate, dueDate, status);
        this.returnDate = returnDate;
    }

    public BorrowRecord(
            Document document,
            User user,
            LocalDate borrowDate,
            LocalDate dueDate,
            BorrowStatus status) {

        this.document = document;
        this.user = user;

        if (document != null) {
            this.bookId = document.getId();
        }

        if (user != null) {
            this.userId = user.getId();
        }

        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.status = status;
    }

    public BorrowRecord(
            Document document,
            User user,
            LocalDate borrowDate,
            LocalDate dueDate,
            LocalDate returnDate,
            BorrowStatus status) {

        this(document, user, borrowDate, dueDate, status);
        this.returnDate = returnDate;
    }

    public long getDaysRemaining() {

        if (dueDate == null) {
            return 0;
        }

        return ChronoUnit.DAYS.between(
                LocalDate.now(),
                dueDate);
    }

    public boolean isOverdue() {
        if ((status != BorrowStatus.BORROWING && status != BorrowStatus.OVERDUE) || dueDate == null) {
            return false;
        }

        return LocalDate.now().isAfter(dueDate);
    }

    public long getOverdueDays() {

        if (status == BorrowStatus.RETURNED || dueDate == null) {
            return 0;
        }

        if (!LocalDate.now().isAfter(dueDate)) {
            return 0;
        }

        return ChronoUnit.DAYS.between(
                dueDate,
                LocalDate.now());
    }

    public void returnDocument(Long fineAmount, String paymentMethod, String note) {
        this.returnDate = LocalDate.now();
        this.status = BorrowStatus.RETURNED;
        this.fineAmount = fineAmount != null ? Math.max(0, fineAmount) : 0L;
        this.paymentMethod = (paymentMethod != null && !paymentMethod.trim().isEmpty()) ? paymentMethod : "NONE";
        this.note = note;
    }

    public void returnDocument() {
        returnDocument(0L, "NONE", null);
    }

    public long calculateLateFine(long finePerDay) {

        long overdueDays = getOverdueDays();

        if (overdueDays <= 0 || finePerDay <= 0) {
            return 0;
        }

        return overdueDays * finePerDay;
    }
}