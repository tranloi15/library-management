package com.library.service.impl;

import com.library.dto.NotificationCampaignDto;
import com.library.model.*;
import com.library.repository.BorrowRecordRepository;
import com.library.repository.NotificationRepository;
import com.library.repository.UserRepository;
import com.library.service.ActivityLogService;
import com.library.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final ActivityLogService activityLogService;

    @Override
    @Transactional
    public Notification sendToUser(Long senderId, String senderName, Long recipientId, String title, String content, NotificationType type, String targetUrl) {
        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người nhận với ID: " + recipientId));

        Notification notification = Notification.builder()
                .senderId(senderId)
                .senderName(senderName)
                .recipientId(recipient.getId())
                .recipientName(recipient.getFullName())
                .recipientCode(recipient.getReaderCode())
                .broadcastId(UUID.randomUUID().toString())
                .isBroadcast(false)
                .title(title)
                .content(content)
                .type(type != null ? type : NotificationType.SYSTEM)
                .isRead(false)
                .targetUrl(targetUrl)
                .build();

        Notification saved = notificationRepository.save(notification);

        if (activityLogService != null) {
            try {
                activityLogService.log(
                        ActionType.NOTIFICATION_SEND,
                        TargetType.NOTIFICATION,
                        saved.getId(),
                        title,
                        "Gửi thông báo tới người dùng " + recipient.getFullName(),
                        "Người nhận: " + recipient.getUsername() + " (" + recipient.getEmail() + ")\nLoại: " + notification.getType().getDisplayName() + "\nTiêu đề: " + title
                );
            } catch (Exception e) {
                log.warn("Không thể ghi nhật ký gửi thông báo: {}", e.getMessage());
            }
        }

        return saved;
    }

    @Override
    @Transactional
    public List<Notification> sendToUsers(Long senderId, String senderName, List<Long> recipientIds, String title, String content, NotificationType type, String targetUrl) {
        if (recipientIds == null || recipientIds.isEmpty()) {
            return List.of();
        }

        List<User> recipients = userRepository.findAllById(recipientIds);
        if (recipients.isEmpty()) {
            return List.of();
        }

        String broadcastId = UUID.randomUUID().toString();
        NotificationType notifType = type != null ? type : NotificationType.SYSTEM;
        List<Notification> list = new ArrayList<>();

        for (User u : recipients) {
            list.add(Notification.builder()
                    .senderId(senderId)
                    .senderName(senderName)
                    .recipientId(u.getId())
                    .recipientName(u.getFullName())
                    .recipientCode(u.getReaderCode())
                    .broadcastId(broadcastId)
                    .isBroadcast(false)
                    .title(title)
                    .content(content)
                    .type(notifType)
                    .isRead(false)
                    .targetUrl(targetUrl)
                    .build());
        }

        List<Notification> savedList = notificationRepository.saveAll(list);

        if (activityLogService != null) {
            try {
                activityLogService.log(
                        ActionType.NOTIFICATION_SEND,
                        TargetType.NOTIFICATION,
                        null,
                        title,
                        "Gửi thông báo chọn lọc tới " + savedList.size() + " độc giả",
                        "Loại: " + notifType.getDisplayName() + "\nTiêu đề: " + title + "\nSố lượng người nhận: " + savedList.size()
                );
            } catch (Exception e) {
                log.warn("Không thể ghi nhật ký gửi thông báo nhóm: {}", e.getMessage());
            }
        }

        return savedList;
    }

    @Override
    @Transactional
    public int sendBroadcast(Long senderId, String senderName, String title, String content, NotificationType type, String targetUrl) {
        List<User> recipients = userRepository.findAll().stream()
                .filter(User::isActive)
                .toList();

        if (recipients.isEmpty()) {
            return 0;
        }

        String broadcastId = UUID.randomUUID().toString();
        NotificationType notifType = type != null ? type : NotificationType.SYSTEM;
        List<Notification> list = new ArrayList<>();

        for (User u : recipients) {
            list.add(Notification.builder()
                    .senderId(senderId)
                    .senderName(senderName)
                    .recipientId(u.getId())
                    .recipientName(u.getFullName())
                    .recipientCode(u.getReaderCode())
                    .broadcastId(broadcastId)
                    .isBroadcast(true)
                    .title(title)
                    .content(content)
                    .type(notifType)
                    .isRead(false)
                    .targetUrl(targetUrl)
                    .build());
        }

        notificationRepository.saveAll(list);

        if (activityLogService != null) {
            try {
                activityLogService.log(
                        ActionType.NOTIFICATION_SEND,
                        TargetType.NOTIFICATION,
                        null,
                        title,
                        "Phát thông báo toàn hệ thống tới " + list.size() + " người dùng",
                        "Loại: " + notifType.getDisplayName() + "\nTiêu đề: " + title + "\nNội dung: " + content
                );
            } catch (Exception e) {
                log.warn("Không thể ghi nhật ký phát thông báo toàn hệ thống: {}", e.getMessage());
            }
        }

        return list.size();
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông báo"));

        if (!notification.getRecipientId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền đánh dấu thông báo này");
        }

        if (!notification.isRead()) {
            notification.markAsRead();
            notificationRepository.save(notification);
        }
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByRecipientId(userId, LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(Long userId) {
        if (userId == null) {
            return 0;
        }
        return notificationRepository.countByRecipientIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getRecentNotifications(Long userId, int limit) {
        if (userId == null) {
            return List.of();
        }
        if (limit <= 5) {
            return notificationRepository.findTop5ByRecipientIdOrderByCreatedAtDesc(userId);
        }
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId, PageRequest.of(0, limit)).getContent();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Notification> getUserNotifications(Long userId, Boolean isRead, NotificationType type, Pageable pageable) {
        if (isRead != null) {
            return notificationRepository.findByRecipientIdAndIsReadOrderByCreatedAtDesc(userId, isRead, pageable);
        }
        if (type != null) {
            return notificationRepository.findByRecipientIdAndTypeOrderByCreatedAtDesc(userId, type, pageable);
        }
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationCampaignDto> getSentCampaignSummaries() {
        List<Object[]> rows = notificationRepository.findBroadcastCampaignSummaries();
        List<NotificationCampaignDto> dtos = new ArrayList<>();

        for (Object[] r : rows) {
            String broadcastId = (String) r[0];
            String title = (String) r[1];
            NotificationType type = (NotificationType) r[2];
            String senderName = (String) r[3];
            LocalDateTime sentAt = (LocalDateTime) r[4];
            long totalCount = ((Number) r[5]).longValue();
            long readCount = ((Number) r[6]).longValue();

            dtos.add(NotificationCampaignDto.builder()
                    .broadcastId(broadcastId)
                    .title(title)
                    .type(type)
                    .senderName(senderName)
                    .sentAt(sentAt)
                    .totalRecipients(totalCount)
                    .readCount(readCount)
                    .build());
        }

        return dtos;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Notification> getAllSentNotifications(Pageable pageable) {
        return notificationRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Override
    @Transactional
    public void deleteCampaign(String broadcastId) {
        notificationRepository.deleteByBroadcastId(broadcastId);
    }

    @Override
    @Transactional
    public void deleteNotification(Long id) {
        notificationRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findUserIdsWithOverdueBooks() {
        List<BorrowRecord> borrowing = borrowRecordRepository.findByStatusIn(
                List.of(BorrowStatus.BORROWING, BorrowStatus.OVERDUE)
        );

        return borrowing.stream()
                .filter(BorrowRecord::isOverdue)
                .map(BorrowRecord::getUserId)
                .distinct()
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findUserIdsWithUnpaidFines() {
        List<BorrowRecord> allRecords = borrowRecordRepository.findAll();

        return allRecords.stream()
                .filter(r -> (r.isOverdue() && r.getOverdueDays() > 0) ||
                        (r.getFineAmount() != null && r.getFineAmount() > 0 && "NONE".equalsIgnoreCase(r.getPaymentMethod())))
                .map(BorrowRecord::getUserId)
                .distinct()
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findNewReaderUserIds(int days) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(days);
        return userRepository.findByRole(RoleName.ROLE_READER).stream()
                .filter(User::isActive)
                .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isAfter(threshold))
                .map(User::getId)
                .distinct()
                .toList();
    }
}
