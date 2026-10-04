package com.library.config;

import com.library.model.Notification;
import com.library.service.NotificationService;
import com.library.service.SettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributes {

    private final SettingService settingService;
    private final NotificationService notificationService;

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails.getId();
        }
        return null;
    }

    @ModelAttribute("unreadNotificationCount")
    public long getUnreadNotificationCount() {
        Long userId = getCurrentUserId();
        return userId != null ? notificationService.countUnread(userId) : 0L;
    }

    @ModelAttribute("recentNotifications")
    public List<Notification> getRecentNotifications() {
        Long userId = getCurrentUserId();
        return userId != null ? notificationService.getRecentNotifications(userId, 5) : Collections.emptyList();
    }

    @ModelAttribute("systemSettings")
    public Map<String, String> getSystemSettings() {
        return settingService.getAllSettingsMap();
    }

    @ModelAttribute("libraryHotline")
    public String getLibraryHotline() {
        return settingService.getLibraryHotline();
    }

    @ModelAttribute("libraryEmail")
    public String getLibraryEmail() {
        return settingService.getLibraryEmail();
    }

    @ModelAttribute("libraryOpeningHours")
    public String getLibraryOpeningHours() {
        return settingService.getLibraryOpeningHours();
    }

    @ModelAttribute("libraryRegulations")
    public String getLibraryRegulations() {
        return settingService.getLibraryRegulations();
    }

    @ModelAttribute("finePerDay")
    public long getFinePerDay() {
        return settingService.getFinePerDay();
    }

    @ModelAttribute("maxBorrowDays")
    public int getMaxBorrowDays() {
        return settingService.getMaxBorrowDays();
    }

    @ModelAttribute("maxBorrowLimit")
    public int getMaxBorrowLimit() {
        return settingService.getMaxBorrowLimit();
    }
}
