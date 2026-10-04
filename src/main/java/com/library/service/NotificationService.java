package com.library.service;

import com.library.dto.NotificationCampaignDto;
import com.library.model.Notification;
import com.library.model.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NotificationService {

    Notification sendToUser(Long senderId, String senderName, Long recipientId, String title, String content, NotificationType type, String targetUrl);

    List<Notification> sendToUsers(Long senderId, String senderName, List<Long> recipientIds, String title, String content, NotificationType type, String targetUrl);

    int sendBroadcast(Long senderId, String senderName, String title, String content, NotificationType type, String targetUrl);

    void markAsRead(Long notificationId, Long userId);

    void markAllAsRead(Long userId);

    long countUnread(Long userId);

    List<Notification> getRecentNotifications(Long userId, int limit);

    Page<Notification> getUserNotifications(Long userId, Boolean isRead, NotificationType type, Pageable pageable);

    List<NotificationCampaignDto> getSentCampaignSummaries();

    Page<Notification> getAllSentNotifications(Pageable pageable);

    void deleteCampaign(String broadcastId);

    void deleteNotification(Long id);

    List<Long> findUserIdsWithOverdueBooks();

    List<Long> findUserIdsWithUnpaidFines();

    List<Long> findNewReaderUserIds(int days);
}
