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

    public BorrowRecord(
            Long bookId,
            Long userId,
            LocalDate borrowDate,
            LocalDate dueDate,
            BorrowStatus status) {

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

        if (status == BorrowStatus.RETURNED || dueDate == null) {
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

    public void returnDocument() {

        this.returnDate = LocalDate.now();
        this.status = BorrowStatus.RETURNED;
    }

    public long calculateLateFine(long finePerDay) {

        long overdueDays = getOverdueDays();

        if (overdueDays <= 0 || finePerDay <= 0) {
            return 0;
        }

        return overdueDays * finePerDay;
    }
}