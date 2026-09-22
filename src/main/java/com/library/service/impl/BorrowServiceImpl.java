package com.library.service.impl;

import com.library.model.BorrowRecord;
import com.library.model.BorrowStatus;
import com.library.model.Document;
import com.library.model.User;
import com.library.repository.BorrowRecordRepository;
import com.library.repository.DocumentRepository;
import com.library.repository.UserRepository;
import com.library.service.BorrowService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BorrowServiceImpl implements BorrowService {

    private final BorrowRecordRepository borrowRecordRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;

    @Override
    public List<BorrowRecord> getAll() {
        updateOverdue();
        return borrowRecordRepository.findAll();
    }

    @Override
    @Transactional
    public BorrowRecord create(
            Long userId,
            Long bookId,
            LocalDate borrowDate,
            LocalDate dueDate) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy độc giả"));

        Document document = documentRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy tài liệu"));

        if (!user.isActive()) {
            throw new IllegalStateException(
                    "Tài khoản độc giả đã bị khóa");
        }

        if (!document.isAvailable()) {
            throw new IllegalStateException(
                    "Tài liệu đã hết");
        }

        if (borrowDate == null) {
            borrowDate = LocalDate.now();
        }

        if (dueDate == null || !dueDate.isAfter(borrowDate)) {
            throw new IllegalArgumentException(
                    "Ngày trả phải sau ngày mượn");
        }

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

    @Override
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

    @Override
    @Transactional
    public void updateOverdue() {

        List<BorrowRecord> records = borrowRecordRepository.findByStatus(
                BorrowStatus.BORROWING);

        for (BorrowRecord record : records) {
            if (record.isOverdue()) {
                record.setStatus(BorrowStatus.OVERDUE);
            }
        }

        borrowRecordRepository.saveAll(records);
    }

    @Override
    public List<BorrowRecord> getUserHistory(Long userId) {

        updateOverdue();

        return borrowRecordRepository
                .findByUserIdOrderByBorrowDateDesc(userId);
    }

    @Override
    public List<BorrowRecord> getUserBorrowing(Long userId) {

        updateOverdue();

        return borrowRecordRepository
                .findByUserIdAndStatusOrderByBorrowDateDesc(
                        userId,
                        BorrowStatus.BORROWING);
    }

    @Override
    public List<BorrowRecord> getUserReturned(Long userId) {

        return borrowRecordRepository
                .findByUserIdAndStatusOrderByBorrowDateDesc(
                        userId,
                        BorrowStatus.RETURNED);
    }

    @Override
    public long calculateFine(Long id, long finePerDay) {

        BorrowRecord borrowRecord = borrowRecordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy phiếu mượn"));

        return borrowRecord.calculateLateFine(finePerDay);
    }

    @Override
    public List<BorrowRecord> getOverdueRecords() {

        updateOverdue();

        return borrowRecordRepository
                .findByStatus(BorrowStatus.OVERDUE);
    }

    @Override
    public List<Object[]> getTopBorrowedBooks() {

        return borrowRecordRepository.findTopBorrowedBooks();
    }
}