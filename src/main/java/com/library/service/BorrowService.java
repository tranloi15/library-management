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

    BorrowRecord getById(Long id);

    BorrowRecord returnBook(Long id);

    BorrowRecord returnBook(Long id, Long fineAmount, String paymentMethod, String note);
 
    BorrowRecord renewBorrow(Long id, Long userId, boolean isAdmin);

    void updateOverdue();

    List<BorrowRecord> getUserHistory(Long userId);

    List<BorrowRecord> getUserBorrowing(Long userId);

    List<BorrowRecord> getUserReturned(Long userId);

    long calculateFine(Long id, long finePerDay);

    List<BorrowRecord> getOverdueRecords();

    List<Object[]> getTopBorrowedBooks();

    // Quy trình mượn tự phục vụ qua QR tại quầy
    BorrowRecord createPendingQrRequest(Long userId, Long bookId);

    BorrowRecord approvePendingRequest(Long recordId, String managerName);

    BorrowRecord rejectPendingRequest(Long recordId, String reason, String managerName);

    BorrowRecord cancelPendingRequest(Long recordId, Long userId);

    List<BorrowRecord> getPendingRequests();

    void cleanupExpiredPendingRequests();
}