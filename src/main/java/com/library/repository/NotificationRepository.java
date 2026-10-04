package com.library.repository;

import com.library.model.Notification;
import com.library.model.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    Page<Notification> findByRecipientIdAndIsReadOrderByCreatedAtDesc(Long recipientId, boolean isRead, Pageable pageable);

    Page<Notification> findByRecipientIdAndTypeOrderByCreatedAtDesc(Long recipientId, NotificationType type, Pageable pageable);

    List<Notification> findTop5ByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    long countByRecipientIdAndIsReadFalse(Long recipientId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt WHERE n.recipientId = :recipientId AND n.isRead = false")
    int markAllAsReadByRecipientId(@Param("recipientId") Long recipientId, @Param("readAt") LocalDateTime readAt);

    Page<Notification> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<Notification> findByBroadcastId(String broadcastId);

    void deleteByBroadcastId(String broadcastId);

    /**
     * Thống kê các đợt phát tin broadcast/nhóm đã gửi đi
     * Trả về mảng Object:
     * [0] broadcastId, [1] title, [2] type, [3] senderName, [4] createdAt (min), [5] totalCount, [6] readCount
     */
    @Query("""
            SELECT n.broadcastId, n.title, n.type, n.senderName, MIN(n.createdAt), COUNT(n.id),
                   SUM(CASE WHEN n.isRead = true THEN 1L ELSE 0L END)
            FROM Notification n
            WHERE n.broadcastId IS NOT NULL
            GROUP BY n.broadcastId, n.title, n.type, n.senderName
            ORDER BY MIN(n.createdAt) DESC
            """)
    List<Object[]> findBroadcastCampaignSummaries();
}
