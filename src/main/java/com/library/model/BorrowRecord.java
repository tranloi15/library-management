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
    private BorrowStatus status; // "BORROWING", "RETURNED", "OVERDUE"

    private Long fineAmount = 0L;

    @Enumerated(EnumType.STRING)
    @Column(name = "book_condition")
    private BookCondition bookCondition = BookCondition.GOOD;

    private Long damageFee = 0L;

    private String paymentMethod; // "CASH", "QR_CODE", "NONE"

    @Column(length = 500)
    private String note;

    public BorrowRecord(Long bookId, Long userId, LocalDate borrowDate, LocalDate dueDate, BorrowStatus status) {
        this.bookId = bookId;
        this.userId = userId;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.status = status;
    }

    public BorrowRecord(Long bookId, Long userId, LocalDate borrowDate, LocalDate dueDate, LocalDate returnDate,
            BorrowStatus status) {
        this(bookId, userId, borrowDate, dueDate, status);
        this.returnDate = returnDate;
    }

    public BorrowRecord(Document document, User user, LocalDate borrowDate, LocalDate dueDate, BorrowStatus status) {
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

    public BorrowRecord(Document document, User user, LocalDate borrowDate, LocalDate dueDate, LocalDate returnDate,
            BorrowStatus status) {
        this(document, user, borrowDate, dueDate, status);
        this.returnDate = returnDate;
    }

    public long getDaysRemaining() {
        if (dueDate == null)
            return 0;
        return ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
    }

    public boolean isOverdue() {
        if ((status != BorrowStatus.BORROWING && status != BorrowStatus.OVERDUE)
                || dueDate == null) {
            return false;
        }

        return LocalDate.now().isAfter(dueDate);
    }

    public long calculateOverdueDays() {
        if (dueDate == null) {
            return 0;
        }

        LocalDate endDate = returnDate != null
                ? returnDate
                : LocalDate.now();

        if (!endDate.isAfter(dueDate)) {
            return 0;
        }

        return ChronoUnit.DAYS.between(dueDate, endDate);
    }

    public long getOverdueDays() {
        return calculateOverdueDays();
    }

    public long calculateLateFine(long finePerDay) {
        long overdueDays = calculateOverdueDays();

        return overdueDays > 0
                ? overdueDays * finePerDay
                : 0;
    }

    public void returnDocument(
            Long fineAmount,
            String paymentMethod,
            String note,
            BookCondition bookCondition,
            Long damageFee) {

        this.returnDate = LocalDate.now();
        this.status = BorrowStatus.RETURNED;

        this.fineAmount = fineAmount != null ? Math.max(0, fineAmount) : 0L;

        this.paymentMethod = (paymentMethod != null && !paymentMethod.trim().isEmpty())
                ? paymentMethod
                : "NONE";

        this.bookCondition = bookCondition != null
                ? bookCondition
                : BookCondition.GOOD;

        this.damageFee = damageFee != null
                ? Math.max(0, damageFee)
                : 0L;

        this.note = note;
    }

    public void returnDocument(
            Long fineAmount,
            String paymentMethod,
            String note) {

        returnDocument(
                fineAmount,
                paymentMethod,
                note,
                BookCondition.GOOD,
                0L);
    }

    public void returnDocument() {
        returnDocument(0L, "NONE", null, BookCondition.GOOD, 0L);
    }

    public long getTotalFine() {
        return (fineAmount != null ? fineAmount : 0L)
                + (damageFee != null ? damageFee : 0L);
    }
}
