package com.library.service;

import com.library.model.BorrowRecord;
import com.library.model.BorrowStatus;
import com.library.model.Document;
import com.library.model.User;
import com.library.repository.BorrowRecordRepository;
import com.library.repository.DocumentRepository;
import com.library.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BorrowRecordService {

    private final BorrowRecordRepository borrowRecordRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;

    // Lấy tất cả phiếu mượn
    public List<BorrowRecord> getAll() {
        updateOverdue();
        return borrowRecordRepository.findAll();
    }

    // Tạo phiếu mượn
    @Transactional
    public BorrowRecord create(
            Long userId,
            Long bookId,
            LocalDate borrowDate,
            LocalDate dueDate) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy độc giả"));

        Document document = documentRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài liệu"));

        if (!user.isActive()) {
            throw new IllegalStateException("Tài khoản độc giả đã bị khóa");
        }

        if (!document.isAvailable()) {
            throw new IllegalStateException("Tài liệu đã hết");
        }

        if (borrowDate == null) {
            borrowDate = LocalDate.now();
        }

        if (dueDate == null || !dueDate.isAfter(borrowDate)) {
            throw new IllegalArgumentException(
                    "Ngày trả phải sau ngày mượn");
        }

        // boolean borrowing = borrowRecordRepository
        // .existsByBookIdAndStatus(bookId, BorrowStatus.BORROWING);

        // boolean overdue = borrowRecordRepository
        // .existsByBookIdAndStatus(bookId, BorrowStatus.OVERDUE);

        // if (borrowing || overdue) {
        // throw new IllegalStateException(
        // "Tài liệu này đang được mượn");
        // }

        BorrowRecord borrowRecord = new BorrowRecord(
                document,
                user,
                borrowDate,
                dueDate,
                BorrowStatus.BORROWING);

        document.adjustQuantity(-1);

        documentRepository.save(document);

        return borrowRecordRepository.save(borrowRecord);
    }

    // Trả sách
    @Transactional
    public BorrowRecord returnBook(Long id) {

        BorrowRecord borrowRecord = borrowRecordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy phiếu mượn"));

        if (borrowRecord.getStatus() == BorrowStatus.RETURNED) {
            throw new IllegalStateException(
                    "Sách này đã được trả");
        }

        Document document = borrowRecord.getDocument();

        borrowRecord.returnDocument();

        if (document != null) {
            document.adjustQuantity(1);
            documentRepository.save(document);
        }

        return borrowRecordRepository.save(borrowRecord);
    }

    // Cập nhật trạng thái quá hạn
    @Transactional
    public void updateOverdue() {

        List<BorrowRecord> records = borrowRecordRepository.findByStatus(BorrowStatus.BORROWING);

        for (BorrowRecord record : records) {
            if (record.isOverdue()) {
                record.setStatus(BorrowStatus.OVERDUE);
            }
        }

        borrowRecordRepository.saveAll(records);
    }

    // Lịch sử mượn của một độc giả
    public List<BorrowRecord> getUserHistory(Long userId) {
        updateOverdue();

        return borrowRecordRepository
                .findByUserIdOrderByBorrowDateDesc(userId);
    }

    // Các sách đang mượn
    public List<BorrowRecord> getUserBorrowing(Long userId) {
        updateOverdue();

        return borrowRecordRepository
                .findByUserIdAndStatusOrderByBorrowDateDesc(
                        userId,
                        BorrowStatus.BORROWING);
    }

    // Các sách đã trả
    public List<BorrowRecord> getUserReturned(Long userId) {
        return borrowRecordRepository
                .findByUserIdAndStatusOrderByBorrowDateDesc(
                        userId,
                        BorrowStatus.RETURNED);
    }

    // Tính tiền phạt
    public long calculateFine(Long id, long finePerDay) {

        BorrowRecord borrowRecord = borrowRecordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy phiếu mượn"));

        return borrowRecord.calculateLateFine(finePerDay);
    }

    public List<BorrowRecord> getOverdueRecords() {
        updateOverdue();

        return borrowRecordRepository
                .findByStatus(BorrowStatus.OVERDUE);
    }

    public List<Object[]> getTopBorrowedBooks() {
        return borrowRecordRepository.findTopBorrowedBooks();
    }
}
