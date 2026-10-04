package com.library.service;

import com.library.dto.NotificationCampaignDto;
import com.library.model.*;
import com.library.repository.BorrowRecordRepository;
import com.library.repository.NotificationRepository;
import com.library.repository.UserRepository;
import com.library.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Kiểm thử dịch vụ thông báo NotificationService")
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BorrowRecordRepository borrowRecordRepository;

    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Test
    @DisplayName("Gửi thông báo cá nhân cho một người dùng thành công")
    void testSendToUserSuccess() {
        User recipient = new User();
        recipient.setId(10L);
        recipient.setUsername("reader1");
        recipient.setFullName("Nguyễn Văn An");
        recipient.setEmail("an@library.com");

        when(userRepository.findById(10L)).thenReturn(Optional.of(recipient));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Notification result = notificationService.sendToUser(
                1L,
                "Quản lý Thư viện",
                10L,
                "Sách sắp đến hạn trả",
                "Vui lòng trả sách đúng hẹn.",
                NotificationType.BORROW_UPDATE,
                "/borrow/history"
        );

        assertNotNull(result);
        assertEquals(10L, result.getRecipientId());
        assertEquals("Nguyễn Văn An", result.getRecipientName());
        assertEquals("Sách sắp đến hạn trả", result.getTitle());
        assertEquals(NotificationType.BORROW_UPDATE, result.getType());
        assertFalse(result.isRead());
        assertFalse(result.isBroadcast());
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    @DisplayName("Phát thông báo toàn hệ thống (Broadcast) tới tất cả người dùng đang hoạt động")
    void testSendBroadcastSuccess() {
        User u1 = new User();
        u1.setId(1L);
        u1.setFullName("Quản lý");
        u1.setActive(true);

        User u2 = new User();
        u2.setId(2L);
        u2.setFullName("Độc giả 1");
        u2.setActive(true);

        User u3 = new User();
        u3.setId(3L);
        u3.setFullName("Độc giả 2");
        u3.setActive(false); // Tài khoản đã bị khóa không nhận broadcast

        when(userRepository.findAll()).thenReturn(List.of(u1, u2, u3));

        int sentCount = notificationService.sendBroadcast(
                1L,
                "Quản lý Thư viện",
                "Thông báo lịch nghỉ lễ",
                "Thư viện nghỉ lễ từ ngày mai.",
                NotificationType.SYSTEM,
                "/catalog"
        );

        assertEquals(2, sentCount);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository, times(1)).saveAll(captor.capture());
        List<Notification> saved = captor.getValue();
        assertEquals(2, saved.size());
        assertTrue(saved.get(0).isBroadcast());
        assertEquals("Thông báo lịch nghỉ lễ", saved.get(0).getTitle());
    }

    @Test
    @DisplayName("Đánh dấu thông báo đã đọc thành công")
    void testMarkAsReadSuccess() {
        Notification notification = Notification.builder()
                .id(100L)
                .recipientId(10L)
                .isRead(false)
                .title("Nhắc nhở")
                .content("Nội dung")
                .type(NotificationType.SYSTEM)
                .build();

        when(notificationRepository.findById(100L)).thenReturn(Optional.of(notification));

        notificationService.markAsRead(100L, 10L);

        assertTrue(notification.isRead());
        assertNotNull(notification.getReadAt());
        verify(notificationRepository, times(1)).save(notification);
    }

    @Test
    @DisplayName("Đếm số lượng thông báo chưa đọc")
    void testCountUnread() {
        when(notificationRepository.countByRecipientIdAndIsReadFalse(10L)).thenReturn(4L);

        long unread = notificationService.countUnread(10L);

        assertEquals(4L, unread);
        assertEquals(0L, notificationService.countUnread(null));
    }

    @Test
    @DisplayName("Tổng hợp dữ liệu các đợt phát thông báo cho giao diện Quản lý")
    void testGetSentCampaignSummaries() {
        LocalDateTime sentTime = LocalDateTime.now().minusHours(2);
        Object[] row1 = new Object[]{"uuid-1", "Thông báo 1", NotificationType.SYSTEM, "Admin", sentTime, 20L, 15L};
        List<Object[]> rows = new java.util.ArrayList<>();
        rows.add(row1);
        when(notificationRepository.findBroadcastCampaignSummaries()).thenReturn(rows);

        List<NotificationCampaignDto> summaries = notificationService.getSentCampaignSummaries();

        assertEquals(1, summaries.size());
        NotificationCampaignDto dto = summaries.get(0);
        assertEquals("uuid-1", dto.getBroadcastId());
        assertEquals("Thông báo 1", dto.getTitle());
        assertEquals(20L, dto.getTotalRecipients());
        assertEquals(15L, dto.getReadCount());
        assertEquals(75, dto.getReadPercentage());
        assertEquals(5L, dto.getUnreadCount());
    }
}
