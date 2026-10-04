package com.library.dto;

import com.library.model.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationCampaignDto {

    private String broadcastId;
    private String title;
    private NotificationType type;
    private String senderName;
    private LocalDateTime sentAt;
    private long totalRecipients;
    private long readCount;

    public int getReadPercentage() {
        if (totalRecipients <= 0) {
            return 0;
        }
        return (int) Math.round(((double) readCount / totalRecipients) * 100);
    }

    public long getUnreadCount() {
        return Math.max(0, totalRecipients - readCount);
    }
}
