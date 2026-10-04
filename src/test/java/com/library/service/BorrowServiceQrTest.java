package com.library.service;

import com.library.model.*;
import com.library.repository.BorrowRecordRepository;
import com.library.repository.DocumentRepository;
import com.library.repository.UserRepository;
import com.library.service.impl.BorrowServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Kiểm thử quy trình mượn tự phục vụ qua mã QR tại quầy")
class BorrowServiceQrTest {

    @Mock
    private BorrowRecordRepository borrowRecordRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ActivityLogService activityLogService;

    @Mock
    private SettingService settingService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private BorrowServiceImpl borrowService;

    private User sampleUser;
    private Book sampleBook;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(10L);
        sampleUser.setUsername("reader_test");
        sampleUser.setFullName("Nguyễn Văn Độc Giả");
        sampleUser.setRole(RoleName.ROLE_READER);
        sampleUser.setActive(true);

        sampleBook = new Book();
        sampleBook.setId(50L);
        sampleBook.setTitle("Lập trình Hướng đối tượng");
        sampleBook.setQuantity(3);

        lenient().when(settingService.isFeatureQrBorrowEnabled()).thenReturn(true);
        lenient().when(settingService.getQrTimeoutMinutes()).thenReturn(30);
        lenient().when(settingService.getMaxBorrowDays()).thenReturn(14);
        lenient().when(settingService.getMaxBorrowLimit()).thenReturn(5);
    }

    @Test
    @DisplayName("Tạo yêu cầu mượn QR thành công: Trừ 1 sách kho, đặt trạng thái PENDING và hẹn giờ 30 phút")
    void testCreatePendingQrRequestSuccess() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(sampleUser));
        when(documentRepository.findById(50L)).thenReturn(Optional.of(sampleBook));
        when(borrowRecordRepository.findByUserId(10L)).thenReturn(Collections.emptyList());
        when(borrowRecordRepository.save(any(BorrowRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BorrowRecord record = borrowService.createPendingQrRequest(10L, 50L);

        assertNotNull(record);
        assertEquals(BorrowStatus.PENDING, record.getStatus());
        assertEquals("QR_SELF_SERVICE", record.getRequestType());
        assertNotNull(record.getRequestExpiresAt());
        assertTrue(record.getRequestExpiresAt().isAfter(LocalDateTime.now()));

        // Sách trong kho phải giảm đi 1
        assertEquals(2, sampleBook.getQuantity());
        verify(documentRepository).save(sampleBook);
        verify(borrowRecordRepository).save(any(BorrowRecord.class));
    }

    @Test
    @DisplayName("Chặn tạo yêu cầu khi tính năng mượn QR đang bị tắt trong cấu hình")
    void testCreatePendingQrRequestFeatureDisabled() {
        when(settingService.isFeatureQrBorrowEnabled()).thenReturn(false);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                borrowService.createPendingQrRequest(10L, 50L)
        );
        assertTrue(ex.getMessage().contains("tạm ngưng"));
    }

    @Test
    @DisplayName("Chặn tạo yêu cầu khi sách trong kho đã hết bản khả dụng")
    void testCreatePendingQrRequestOutOfStock() {
        sampleBook.setQuantity(0);
        when(userRepository.findById(10L)).thenReturn(Optional.of(sampleUser));
        when(documentRepository.findById(50L)).thenReturn(Optional.of(sampleBook));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                borrowService.createPendingQrRequest(10L, 50L)
        );
        assertTrue(ex.getMessage().contains("hết bản khả dụng"));
    }

    @Test
    @DisplayName("Chặn tạo yêu cầu khi độc giả đang có sách quá hạn chưa trả")
    void testCreatePendingQrRequestHasOverdue() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(sampleUser));
        when(documentRepository.findById(50L)).thenReturn(Optional.of(sampleBook));

        BorrowRecord overdueRecord = new BorrowRecord();
        overdueRecord.setId(99L);
        overdueRecord.setStatus(BorrowStatus.OVERDUE);
        overdueRecord.setDueDate(LocalDate.now().minusDays(2));

        when(borrowRecordRepository.findByUserId(10L)).thenReturn(List.of(overdueRecord));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                borrowService.createPendingQrRequest(10L, 50L)
        );
        assertTrue(ex.getMessage().contains("quá hạn"));
    }

    @Test
    @DisplayName("Chặn tạo yêu cầu khi độc giả đã đạt giới hạn số sách mượn tối đa")
    void testCreatePendingQrRequestExceedLimit() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(sampleUser));
        when(documentRepository.findById(50L)).thenReturn(Optional.of(sampleBook));

        BorrowRecord r1 = new BorrowRecord(); r1.setStatus(BorrowStatus.BORROWING);
        BorrowRecord r2 = new BorrowRecord(); r2.setStatus(BorrowStatus.BORROWING);
        BorrowRecord r3 = new BorrowRecord(); r3.setStatus(BorrowStatus.BORROWING);
        BorrowRecord r4 = new BorrowRecord(); r4.setStatus(BorrowStatus.BORROWING);
        BorrowRecord r5 = new BorrowRecord(); r5.setStatus(BorrowStatus.PENDING);

        when(borrowRecordRepository.findByUserId(10L)).thenReturn(List.of(r1, r2, r3, r4, r5));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                borrowService.createPendingQrRequest(10L, 50L)
        );
        assertTrue(ex.getMessage().contains("giới hạn mượn tối đa"));
    }

    @Test
    @DisplayName("Quản lý duyệt yêu cầu tại quầy thành công: Đổi trạng thái sang BORROWING và ghi nhận người duyệt")
    void testApprovePendingRequestSuccess() {
        BorrowRecord pendingRecord = new BorrowRecord();
        pendingRecord.setId(101L);
        pendingRecord.setDocument(sampleBook);
        pendingRecord.setUser(sampleUser);
        pendingRecord.setUserId(sampleUser.getId());
        pendingRecord.setStatus(BorrowStatus.PENDING);
        pendingRecord.setRequestExpiresAt(LocalDateTime.now().plusMinutes(20));

        when(borrowRecordRepository.findById(101L)).thenReturn(Optional.of(pendingRecord));
        when(borrowRecordRepository.save(any(BorrowRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BorrowRecord approved = borrowService.approvePendingRequest(101L, "Quản lý Nguyễn Văn A");

        assertEquals(BorrowStatus.BORROWING, approved.getStatus());
        assertEquals("Quản lý Nguyễn Văn A", approved.getApprovedBy());
        assertNotNull(approved.getApprovedAt());
        assertNotNull(approved.getDueDate());
        verify(borrowRecordRepository).save(pendingRecord);
    }

    @Test
    @DisplayName("Từ chối duyệt khi yêu cầu mượn sách đã quá hạn 30 phút")
    void testApprovePendingRequestExpired() {
        BorrowRecord expiredRecord = new BorrowRecord();
        expiredRecord.setId(102L);
        expiredRecord.setStatus(BorrowStatus.PENDING);
        expiredRecord.setRequestExpiresAt(LocalDateTime.now().minusMinutes(5));

        when(borrowRecordRepository.findById(102L)).thenReturn(Optional.of(expiredRecord));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                borrowService.approvePendingRequest(102L, "Quản lý")
        );
        assertTrue(ex.getMessage().contains("hết thời hạn giữ sách"));
    }

    @Test
    @DisplayName("Quản lý từ chối yêu cầu: Hoàn trả số lượng sách và lưu lý do từ chối")
    void testRejectPendingRequestSuccess() {
        sampleBook.setQuantity(2);

        BorrowRecord pendingRecord = new BorrowRecord();
        pendingRecord.setId(103L);
        pendingRecord.setDocument(sampleBook);
        pendingRecord.setUser(sampleUser);
        pendingRecord.setUserId(sampleUser.getId());
        pendingRecord.setStatus(BorrowStatus.PENDING);

        when(borrowRecordRepository.findById(103L)).thenReturn(Optional.of(pendingRecord));
        when(borrowRecordRepository.save(any(BorrowRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BorrowRecord rejected = borrowService.rejectPendingRequest(103L, "Độc giả chưa xuất trình thẻ sinh viên", "Quản lý B");

        assertEquals(BorrowStatus.REJECTED, rejected.getStatus());
        assertEquals("Độc giả chưa xuất trình thẻ sinh viên", rejected.getRejectReason());
        // Số lượng trong kho được hoàn trả
        assertEquals(3, sampleBook.getQuantity());
        verify(documentRepository).save(sampleBook);
        verify(borrowRecordRepository).save(pendingRecord);
    }

    @Test
    @DisplayName("Độc giả tự hủy yêu cầu mượn: Hoàn trả sách và chuyển trạng thái CANCELLED")
    void testCancelPendingRequestSuccess() {
        sampleBook.setQuantity(1);

        BorrowRecord pendingRecord = new BorrowRecord();
        pendingRecord.setId(104L);
        pendingRecord.setDocument(sampleBook);
        pendingRecord.setUserId(sampleUser.getId());
        pendingRecord.setStatus(BorrowStatus.PENDING);

        when(borrowRecordRepository.findById(104L)).thenReturn(Optional.of(pendingRecord));
        when(borrowRecordRepository.save(any(BorrowRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BorrowRecord cancelled = borrowService.cancelPendingRequest(104L, sampleUser.getId());

        assertEquals(BorrowStatus.CANCELLED, cancelled.getStatus());
        assertEquals(2, sampleBook.getQuantity());
        verify(documentRepository).save(sampleBook);
        verify(borrowRecordRepository).save(pendingRecord);
    }

    @Test
    @DisplayName("Tự động quét dọn yêu cầu quá hạn: Hủy phiếu và hoàn trả sách vào kho")
    void testCleanupExpiredPendingRequests() {
        sampleBook.setQuantity(4);

        BorrowRecord expiredRecord = new BorrowRecord();
        expiredRecord.setId(105L);
        expiredRecord.setDocument(sampleBook);
        expiredRecord.setUserId(sampleUser.getId());
        expiredRecord.setStatus(BorrowStatus.PENDING);
        expiredRecord.setRequestExpiresAt(LocalDateTime.now().minusMinutes(10));

        BorrowRecord validRecord = new BorrowRecord();
        validRecord.setId(106L);
        validRecord.setDocument(sampleBook);
        validRecord.setUserId(sampleUser.getId());
        validRecord.setStatus(BorrowStatus.PENDING);
        validRecord.setRequestExpiresAt(LocalDateTime.now().plusMinutes(15));

        when(borrowRecordRepository.findByStatus(BorrowStatus.PENDING)).thenReturn(List.of(expiredRecord, validRecord));

        borrowService.cleanupExpiredPendingRequests();

        assertEquals(BorrowStatus.CANCELLED, expiredRecord.getStatus());
        assertEquals(BorrowStatus.PENDING, validRecord.getStatus());
        assertEquals(5, sampleBook.getQuantity());
        verify(documentRepository).save(sampleBook);
    }
}
