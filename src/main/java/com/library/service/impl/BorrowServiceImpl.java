package com.library.service.impl;

import com.library.model.BookCondition;
import com.library.model.BorrowRecord;
import com.library.model.BorrowStatus;
import com.library.model.Document;
import com.library.model.RoleName;
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
    public BorrowRecord getById(Long id) {
        return borrowRecordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu mượn với mã #" + id));
    }

    @Transactional
    @Override
    public BorrowRecord create(
            Long userId,
            Long bookId,
            LocalDate borrowDate,
            LocalDate dueDate) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy độc giả"));

        if (!user.isActive()) {
            throw new IllegalStateException("Tài khoản độc giả đã bị khóa");
        }

        if (user.getRole() != RoleName.ROLE_READER) {
            throw new IllegalStateException("Chỉ độc giả mới được mượn sách");
        }

        updateOverdue();

        List<BorrowStatus> activeStatuses = List.of(
                BorrowStatus.BORROWING,
                BorrowStatus.OVERDUE);

        long borrowedCount = borrowRecordRepository.countByUserIdAndStatusIn(
                userId,
                activeStatuses);

        if (borrowedCount >= 5) {
            throw new IllegalStateException(
                    "Độc giả đã mượn tối đa 5 cuốn sách");
        }

        boolean hasOverdue = !borrowRecordRepository
                .findByUserIdAndStatusInOrderByBorrowDateDesc(
                        userId,
                        List.of(BorrowStatus.OVERDUE))
                .isEmpty();

        if (hasOverdue) {
            throw new IllegalStateException(
                    "Độc giả đang có sách quá hạn, không thể mượn thêm");
        }

        Document document = documentRepository
                .findByIdForUpdate(bookId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy tài liệu"));

        if (document.getQuantity() <= 0
                || !document.isAvailable()) {
            throw new IllegalStateException(
                    "Sách đã hết");
        }

        if (borrowDate == null) {
            borrowDate = LocalDate.now();
        }

        if (dueDate == null) {
            dueDate = borrowDate.plusDays(14);
        }

        if (!dueDate.isAfter(borrowDate)) {
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
        return returnBook(
                id,
                0L,
                0L,
                "NONE",
                null,
                BookCondition.GOOD);
    }

    @Override
    @Transactional
    public BorrowRecord returnBook(Long id, Long fineAmount, String paymentMethod, String note) {
        return returnBook(
                id,
                fineAmount,
                0L,
                paymentMethod,
                note,
                BookCondition.GOOD);
    }

    @Transactional
    @Override
    public BorrowRecord returnBook(
            Long id,
            Long fineAmount,
            Long damageFee,
            String paymentMethod,
            String note,
            BookCondition bookCondition) {

        BorrowRecord borrowRecord = borrowRecordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy phiếu mượn"));

        if (borrowRecord.getStatus() == BorrowStatus.RETURNED) {
            throw new IllegalStateException(
                    "Sách này đã được trả");
        }

        Document document = borrowRecord.getDocument();

        if (document == null) {
            document = documentRepository
                    .findById(borrowRecord.getBookId())
                    .orElse(null);
        }

        BookCondition condition = bookCondition != null
                ? bookCondition
                : BookCondition.GOOD;

        long actualDamageFee = condition.getFee();

        borrowRecord.returnDocument(
                fineAmount,
                paymentMethod,
                note,
                condition,
                actualDamageFee);

        /*
         * Sách bị mất thì KHÔNG cộng lại kho.
         */
        if (document != null
                && condition != BookCondition.LOST) {

            document.adjustQuantity(1);
            documentRepository.save(document);
        }

        return borrowRecordRepository.save(borrowRecord);
    }

    @Override
    @Transactional
    public BorrowRecord renewBorrow(Long id, Long userId, boolean isAdmin) {
        BorrowRecord record = borrowRecordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu mượn với mã #" + id));

        // Kiểm tra quyền sở hữu phiếu mượn
        if (!isAdmin && (record.getUser() == null || !record.getUser().getId().equals(userId))) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Bạn không có quyền gia hạn phiếu mượn này!");
        }

        if (record.getStatus() == BorrowStatus.RETURNED) {
            throw new IllegalStateException("Không thể gia hạn sách đã hoàn tất trả!");
        }

        if (record.isOverdue() || record.getStatus() == BorrowStatus.OVERDUE) {
            throw new IllegalStateException(
                    "Sách đã quá hạn, không thể gia hạn. Vui lòng mang sách đến quầy để hoàn trả!");
        }

        // Gia hạn thêm 7 ngày vào ngày hẹn trả
        LocalDate currentDueDate = record.getDueDate() != null ? record.getDueDate() : LocalDate.now();
        record.setDueDate(currentDueDate.plusDays(7));
        return borrowRecordRepository.save(record);
    }

    @Override
    @Transactional
    public void updateOverdue() {

        List<BorrowRecord> records = borrowRecordRepository.findByStatus(
                BorrowStatus.BORROWING);

        boolean changed = false;

        for (BorrowRecord record : records) {

            if (record.isOverdue()) {
                record.setStatus(BorrowStatus.OVERDUE);
                changed = true;
            }
        }

        if (changed) {
            borrowRecordRepository.saveAll(records);
        }
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
                .findByUserIdAndStatusInOrderByBorrowDateDesc(
                        userId,
                        List.of(BorrowStatus.BORROWING, BorrowStatus.OVERDUE));
    }

    @Override
    public List<BorrowRecord> getUserReturned(Long userId) {

        return borrowRecordRepository
                .findByUserIdAndStatusOrderByBorrowDateDesc(
                        userId,
                        BorrowStatus.RETURNED);
    }

    @Override
    @Transactional(readOnly = true)
    public long calculateFine(Long id, long finePerDay) {

        BorrowRecord record = borrowRecordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy phiếu mượn"));

        return record.calculateLateFine(finePerDay);
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