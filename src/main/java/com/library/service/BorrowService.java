package com.library.service;

import com.library.model.BorrowRecord;

import java.util.List;

public interface BorrowService {

    List<BorrowRecord> getAll();

    BorrowRecord create(
            Long userId,
            Long bookId,
            java.time.LocalDate borrowDate,
            java.time.LocalDate dueDate);

    BorrowRecord returnBook(Long id);

    void updateOverdue();

    List<BorrowRecord> getUserHistory(Long userId);

    List<BorrowRecord> getUserBorrowing(Long userId);

    List<BorrowRecord> getUserReturned(Long userId);

    long calculateFine(Long id, long finePerDay);

    List<BorrowRecord> getOverdueRecords();

    List<Object[]> getTopBorrowedBooks();
}