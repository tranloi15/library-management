package com.library.service.impl;

import com.library.model.ActionType;
import com.library.model.BorrowRecord;
import com.library.model.BorrowStatus;
import com.library.model.Document;
import com.library.model.TargetType;
import com.library.model.User;
import com.library.repository.BorrowRecordRepository;
import com.library.repository.DocumentRepository;
import com.library.repository.UserRepository;
import com.library.service.ActivityLogService;
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
    private final ActivityLogService activityLogService;
    private final com.library.service.SettingService settingService;
    private final com.library.service.NotificationService notificationService;

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

        if (!user.canBorrow()) {
            throw new IllegalStateException(
                    user.isActive() ? "Chỉ tài khoản độc giả mới có thể lập phiếu mượn!" : "Tài khoản độc giả đã bị khóa, không thể mượn sách");
        }

        int maxLimit = settingService != null ? settingService.getMaxBorrowLimit() : 5;
        long currentBorrowing = borrowRecordRepository.findByUserId(userId).stream()
                .filter(r -> r.getStatus() == BorrowStatus.BORROWING || r.getStatus() == BorrowStatus.OVERDUE)
                .count();
        if (currentBorrowing >= maxLimit) {
            throw new IllegalStateException("Độc giả đã đạt giới hạn mượn tối đa (" + maxLimit + " cuốn). Vui lòng hoàn trả sách trước khi mượn tiếp!");
        }

        if (document.getQuantity() <= 0 || !document.isAvailable()) {
            throw new IllegalStateException(
                    "Tài liệu '" + document.getTitle() + "' hiện đã hết số lượng trong kho");
        }

        if (borrowDate == null) {
            borrowDate = LocalDate.now();
        }

        if (dueDate == null || !dueDate.isAfter(borrowDate)) {
            throw new IllegalArgumentException(
                    "Ngày hẹn trả phải sau ngày mượn ít nhất 1 ngày");
        }

        BorrowRecord borrowRecord = new BorrowRecord(
                document,
                user,
                borrowDate,
                dueDate,
                BorrowStatus.BORROWING);

        document.adjustQuantity(-1);
        documentRepository.save(document);

        BorrowRecord saved = borrowRecordRepository.save(borrowRecord);
        activityLogService.log(
                ActionType.BORROW_CREATE,
                TargetType.BORROW_RECORD,
                saved.getId(),
                document.getTitle(),
                "Tạo phiếu mượn #" + saved.getId() + " - Tài liệu: " + document.getTitle() + " cho độc giả " + user.getFullName()
        );
        return saved;
    }

    @Override
    @Transactional
    public BorrowRecord returnBook(Long id) {
        return returnBook(id, 0L, "NONE", null);
    }

    @Override
    @Transactional
    public BorrowRecord returnBook(Long id, Long fineAmount, String paymentMethod, String note) {
        BorrowRecord borrowRecord = borrowRecordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy phiếu mượn với mã #" + id));

        if (borrowRecord.getStatus() == BorrowStatus.RETURNED) {
            throw new IllegalStateException(
                    "Phiếu mượn này đã được hoàn tất trả sách trước đó!");
        }

        Document document = borrowRecord.getDocument();
        if (document == null && borrowRecord.getBookId() != null) {
            document = documentRepository.findById(borrowRecord.getBookId()).orElse(null);
        }

        borrowRecord.returnDocument(fineAmount, paymentMethod, note);

        if (document != null) {
            document.adjustQuantity(1);
            documentRepository.save(document);
        }

        BorrowRecord saved = borrowRecordRepository.save(borrowRecord);
        String docTitle = document != null ? document.getTitle() : "Tài liệu #" + saved.getBookId();
        activityLogService.log(
                ActionType.BORROW_RETURN,
                TargetType.BORROW_RECORD,
                saved.getId(),
                docTitle,
                "Hoàn tất trả sách phiếu mượn #" + saved.getId() + " - " + docTitle
        );
        if (fineAmount != null && fineAmount > 0) {
            activityLogService.log(
                    ActionType.FINE_COLLECT,
                    TargetType.BORROW_RECORD,
                    saved.getId(),
                    docTitle,
                    "Thu tiền phạt trễ hạn " + String.format("%,d VNĐ", fineAmount) + " qua hình thức " + paymentMethod
            );
        }
        return saved;
    }

    @Override
    @Transactional
    public BorrowRecord renewBorrow(Long id, Long userId, boolean isAdmin) {
        BorrowRecord record = borrowRecordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu mượn với mã #" + id));

        // Kiểm tra quyền sở hữu phiếu mượn
        if (!isAdmin && (record.getUser() == null || !record.getUser().getId().equals(userId))) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền gia hạn phiếu mượn này!");
        }

        if (record.getStatus() == BorrowStatus.RETURNED) {
            throw new IllegalStateException("Không thể gia hạn sách đã hoàn tất trả!");
        }

        if (record.isOverdue() || record.getStatus() == BorrowStatus.OVERDUE) {
            throw new IllegalStateException("Sách đã quá hạn, không thể gia hạn. Vui lòng mang sách đến quầy để hoàn trả!");
        }

        // Gia hạn thêm 7 ngày vào ngày hẹn trả
        LocalDate currentDueDate = record.getDueDate() != null ? record.getDueDate() : LocalDate.now();
        record.setDueDate(currentDueDate.plusDays(7));
        BorrowRecord saved = borrowRecordRepository.save(record);
        String docTitle = record.getDocument() != null ? record.getDocument().getTitle() : "Phiếu mượn #" + record.getId();
        activityLogService.log(
                ActionType.BORROW_EXTEND,
                TargetType.BORROW_RECORD,
                saved.getId(),
                docTitle,
                "Gia hạn thêm 7 ngày cho phiếu mượn #" + saved.getId() + " (Hạn mới: " + saved.getDueDate() + ")"
        );
        return saved;
    }

    @Override
    @Transactional
    public void updateOverdue() {
        List<BorrowRecord> activeRecords = borrowRecordRepository.findByStatus(BorrowStatus.BORROWING);
        List<BorrowRecord> overdueRecords = activeRecords.stream()
                .filter(BorrowRecord::markOverdueIfApplicable)
                .toList();

        if (!overdueRecords.isEmpty()) {
            borrowRecordRepository.saveAll(overdueRecords);
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

    @Override
    @Transactional
    public BorrowRecord createPendingQrRequest(Long userId, Long bookId) {
        if (settingService != null && !settingService.isFeatureQrBorrowEnabled()) {
            throw new IllegalStateException("Tính năng mượn sách qua QR tự phục vụ hiện đang tạm ngưng bảo trì!");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin tài khoản độc giả"));

        if (!user.isActive()) {
            throw new IllegalStateException("Tài khoản độc giả đã bị khóa, không thể gửi yêu cầu mượn sách");
        }

        Document document = documentRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài liệu với mã #" + bookId));

        if (document.getQuantity() <= 0 || !document.isAvailable()) {
            throw new IllegalStateException("Tài liệu '" + document.getTitle() + "' hiện đã hết bản khả dụng trong kho");
        }

        // Kiểm tra độc giả có sách quá hạn không
        List<BorrowRecord> userRecords = borrowRecordRepository.findByUserId(userId);
        boolean hasOverdue = userRecords.stream().anyMatch(BorrowRecord::isOverdue);
        if (hasOverdue) {
            throw new IllegalStateException("Bạn đang có sách mượn quá hạn chưa hoàn trả. Vui lòng trả sách tại quầy trước khi mượn thêm!");
        }

        // Kiểm tra nợ phí phạt
        boolean hasUnpaidFine = userRecords.stream().anyMatch(r ->
                (r.getFineAmount() != null && r.getFineAmount() > 0 && "NONE".equalsIgnoreCase(r.getPaymentMethod())));
        if (hasUnpaidFine) {
            throw new IllegalStateException("Bạn có khoản phí phạt trễ hạn chưa quyết toán. Vui lòng thanh toán tại quầy trước khi gửi yêu cầu mượn!");
        }

        // Kiểm tra giới hạn mượn tối đa
        int maxLimit = settingService != null ? settingService.getMaxBorrowLimit() : 5;
        long activeBorrowCount = userRecords.stream()
                .filter(r -> r.getStatus() == BorrowStatus.BORROWING || r.getStatus() == BorrowStatus.OVERDUE || r.getStatus() == BorrowStatus.PENDING)
                .count();
        if (activeBorrowCount >= maxLimit) {
            throw new IllegalStateException("Bạn đã đạt giới hạn mượn tối đa (" + maxLimit + " cuốn bao gồm cả yêu cầu chờ duyệt)!");
        }

        // Kiểm tra xem độc giả đã gửi yêu cầu hoặc đang mượn chính cuốn sách này chưa
        boolean alreadyHasBook = userRecords.stream()
                .anyMatch(r -> r.getBookId().equals(bookId) && (r.getStatus() == BorrowStatus.BORROWING || r.getStatus() == BorrowStatus.PENDING));
        if (alreadyHasBook) {
            throw new IllegalStateException("Bạn đang có một yêu cầu chờ duyệt hoặc đang mượn cuốn sách này!");
        }

        document.adjustQuantity(-1);
        documentRepository.save(document);

        int timeoutMinutes = settingService != null ? settingService.getQrTimeoutMinutes() : 30;
        int borrowDays = settingService != null ? settingService.getMaxBorrowDays() : 14;

        BorrowRecord record = new BorrowRecord();
        record.setDocument(document);
        record.setBookId(document.getId());
        record.setUser(user);
        record.setUserId(user.getId());
        record.setBorrowDate(LocalDate.now());
        record.setDueDate(LocalDate.now().plusDays(borrowDays));
        record.setStatus(BorrowStatus.PENDING);
        record.setRequestType("QR_SELF_SERVICE");
        record.setRequestExpiresAt(java.time.LocalDateTime.now().plusMinutes(timeoutMinutes));
        record.setNote("Yêu cầu mượn tự phục vụ qua QR tại quầy");

        BorrowRecord saved = borrowRecordRepository.save(record);

        if (notificationService != null) {
            try {
                notificationService.sendToUser(
                        null,
                        "Hệ thống Thư viện",
                        userId,
                        "Yêu cầu mượn sách thành công: " + document.getTitle(),
                        "Yêu cầu mượn của bạn đã được ghi nhận. Vui lòng mang sách ra quầy trong vòng " + timeoutMinutes + " phút để quản lý hoàn tất thủ tục mượn.",
                        com.library.model.NotificationType.BORROW_UPDATE,
                        "/borrow/history"
                );
            } catch (Exception ignored) {
            }
        }

        // Ghi nhật ký
        if (activityLogService != null) {
            try {
                activityLogService.log(
                        ActionType.BORROW_REQUEST,
                        TargetType.BORROW_RECORD,
                        saved.getId(),
                        document.getTitle(),
                        "Độc giả " + user.getFullName() + " gửi yêu cầu mượn sách qua QR",
                        "Mã độc giả: " + user.getReaderCode() + "\nTài liệu: " + document.getTitle() + "\nThời hạn giữ: " + timeoutMinutes + " phút"
                );
            } catch (Exception ignored) {
            }
        }

        return saved;
    }

    @Override
    @Transactional
    public BorrowRecord approvePendingRequest(Long recordId, String managerName) {
        BorrowRecord record = getById(recordId);

        if (record.getStatus() != BorrowStatus.PENDING) {
            throw new IllegalStateException("Phiếu mượn này không ở trạng thái chờ duyệt (Hiện tại: " + record.getStatus().getDisplayName() + ")");
        }

        if (record.isRequestExpired()) {
            throw new IllegalStateException("Yêu cầu mượn này đã hết thời hạn giữ sách tại quầy!");
        }

        int borrowDays = settingService != null ? settingService.getMaxBorrowDays() : 14;
        record.approveRequest(borrowDays, managerName != null ? managerName : "Quản lý");
        BorrowRecord saved = borrowRecordRepository.save(record);

        // Gửi thông báo cho độc giả
        if (notificationService != null) {
            try {
                notificationService.sendToUser(
                        null,
                        "Quản lý Thư viện",
                        record.getUserId(),
                        "Đã duyệt phiếu mượn sách: " + (record.getDocument() != null ? record.getDocument().getTitle() : "Tài liệu"),
                        "Thủ tục mượn sách tại quầy đã hoàn tất thành công. Hạn trả sách của bạn là " + record.getDueDate() + ". Chúc bạn đọc sách vui vẻ!",
                        com.library.model.NotificationType.BORROW_UPDATE,
                        "/borrow/history"
                );
            } catch (Exception ignored) {
            }
        }

        // Ghi nhật ký
        if (activityLogService != null) {
            try {
                activityLogService.log(
                        ActionType.BORROW_APPROVE,
                        TargetType.BORROW_RECORD,
                        saved.getId(),
                        record.getDocument() != null ? record.getDocument().getTitle() : "Tài liệu #" + record.getBookId(),
                        "Quản lý " + managerName + " duyệt yêu cầu mượn sách tại quầy",
                        "Độc giả: " + (record.getUser() != null ? record.getUser().getFullName() : record.getUserId()) + "\nHạn trả: " + record.getDueDate()
                );
            } catch (Exception ignored) {
            }
        }

        return saved;
    }

    @Override
    @Transactional
    public BorrowRecord rejectPendingRequest(Long recordId, String reason, String managerName) {
        BorrowRecord record = getById(recordId);

        if (record.getStatus() != BorrowStatus.PENDING) {
            throw new IllegalStateException("Phiếu mượn này không ở trạng thái chờ duyệt!");
        }

        Document doc = record.getDocument();
        if (doc != null) {
            doc.adjustQuantity(1);
            documentRepository.save(doc);
        }

        String safeReason = (reason != null && !reason.isBlank()) ? reason.trim() : "Quản lý từ chối tại quầy";
        record.rejectRequest(safeReason, managerName != null ? managerName : "Quản lý");
        BorrowRecord saved = borrowRecordRepository.save(record);

        if (notificationService != null) {
            try {
                notificationService.sendToUser(
                        null,
                        "Quản lý Thư viện",
                        record.getUserId(),
                        "Yêu cầu mượn sách không được duyệt: " + (doc != null ? doc.getTitle() : "Tài liệu"),
                        "Rất tiếc yêu cầu mượn sách của bạn không được phê duyệt tại quầy. Lý do: " + safeReason + ". Vui lòng liên hệ quản lý để được hỗ trợ.",
                        com.library.model.NotificationType.BORROW_UPDATE,
                        "/borrow/history"
                );
            } catch (Exception ignored) {
            }
        }

        if (activityLogService != null) {
            try {
                activityLogService.log(
                        ActionType.BORROW_REJECT,
                        TargetType.BORROW_RECORD,
                        saved.getId(),
                        doc != null ? doc.getTitle() : "Tài liệu #" + record.getBookId(),
                        "Quản lý " + managerName + " từ chối yêu cầu mượn tại quầy",
                        "Lý do từ chối: " + safeReason
                );
            } catch (Exception ignored) {
            }
        }

        return saved;
    }

    @Override
    @Transactional
    public BorrowRecord cancelPendingRequest(Long recordId, Long userId) {
        BorrowRecord record = getById(recordId);

        if (record.getStatus() != BorrowStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể hủy yêu cầu mượn đang chờ duyệt!");
        }

        if (userId != null && !record.getUserId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền hủy yêu cầu này!");
        }

        Document doc = record.getDocument();
        if (doc != null) {
            doc.adjustQuantity(1);
            documentRepository.save(doc);
        }

        record.cancel("Độc giả tự hủy yêu cầu mượn sách");
        BorrowRecord saved = borrowRecordRepository.save(record);

        if (activityLogService != null) {
            try {
                activityLogService.log(
                        ActionType.BORROW_RETURN,
                        TargetType.BORROW_RECORD,
                        saved.getId(),
                        doc != null ? doc.getTitle() : "Tài liệu #" + record.getBookId(),
                        "Độc giả tự hủy yêu cầu mượn sách chờ duyệt",
                        null
                );
            } catch (Exception ignored) {
            }
        }

        return saved;
    }

    @Override
    @Transactional
    public List<BorrowRecord> getPendingRequests() {
        cleanupExpiredPendingRequests();
        return borrowRecordRepository.findByStatus(BorrowStatus.PENDING).stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
    }

    @Override
    @Transactional
    public void cleanupExpiredPendingRequests() {
        List<BorrowRecord> pending = borrowRecordRepository.findByStatus(BorrowStatus.PENDING);
        for (BorrowRecord r : pending) {
            if (r.isRequestExpired()) {
                r.cancel("Hệ thống tự động hủy do quá thời hạn giữ sách tại quầy");
                Document doc = r.getDocument();
                if (doc != null) {
                    doc.adjustQuantity(1);
                    documentRepository.save(doc);
                }
                borrowRecordRepository.save(r);

                if (notificationService != null) {
                    try {
                        notificationService.sendToUser(
                                null,
                                "Hệ thống Thư viện",
                                r.getUserId(),
                                "Yêu cầu mượn sách đã hết hạn",
                                "Yêu cầu mượn cuốn '" + (doc != null ? doc.getTitle() : "sách") + "' đã tự động hết hạn do quá thời gian giữ sách tại quầy.",
                                com.library.model.NotificationType.BORROW_UPDATE,
                                "/borrow/history"
                        );
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }
}