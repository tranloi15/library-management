package com.library.controller;

import com.library.config.CustomUserDetails;
import com.library.model.Notification;
import com.library.model.NotificationType;
import com.library.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/notifications")
    public String showNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(name = "filter", defaultValue = "ALL") String filter,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            Model model) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        Long userId = userDetails.getId();
        Boolean isRead = null;
        NotificationType type = null;

        if ("UNREAD".equalsIgnoreCase(filter)) {
            isRead = false;
        } else if ("READ".equalsIgnoreCase(filter)) {
            isRead = true;
        } else if (!"ALL".equalsIgnoreCase(filter)) {
            try {
                type = NotificationType.valueOf(filter);
            } catch (Exception ignored) {
            }
        }

        Page<Notification> notifPage = notificationService.getUserNotifications(
                userId, isRead, type, PageRequest.of(page, size));

        model.addAttribute("notifications", notifPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", notifPage.getTotalPages());
        model.addAttribute("totalElements", notifPage.getTotalElements());
        model.addAttribute("currentFilter", filter);
        model.addAttribute("types", NotificationType.values());

        return "notifications/index";
    }

    @PostMapping("/notifications/{id}/read")
    public String markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestHeader(value = "Referer", required = false) String referer) {

        if (userDetails != null) {
            notificationService.markAsRead(id, userDetails.getId());
        }
        return "redirect:" + (referer != null ? referer : "/notifications");
    }

    @PostMapping("/notifications/read-all")
    public String markAllAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestHeader(value = "Referer", required = false) String referer) {

        if (userDetails != null) {
            notificationService.markAllAsRead(userDetails.getId());
        }
        return "redirect:" + (referer != null ? referer : "/notifications");
    }

    @PostMapping("/notifications/{id}/delete")
    public String deleteNotification(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestHeader(value = "Referer", required = false) String referer) {

        if (userDetails != null) {
            notificationService.deleteNotification(id);
        }
        return "redirect:" + (referer != null ? referer : "/notifications");
    }

    @ResponseBody
    @GetMapping("/api/notifications/recent")
    public ResponseEntity<List<Notification>> getRecentNotifications(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(notificationService.getRecentNotifications(userDetails.getId(), 5));
    }

    @ResponseBody
    @GetMapping("/api/notifications/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCount(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Map<String, Object> res = new HashMap<>();
        long count = userDetails != null ? notificationService.countUnread(userDetails.getId()) : 0L;
        res.put("unreadCount", count);
        return ResponseEntity.ok(res);
    }

    @ResponseBody
    @PostMapping("/api/notifications/read-all")
    public ResponseEntity<Map<String, Object>> apiMarkAllAsRead(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Map<String, Object> res = new HashMap<>();
        if (userDetails != null) {
            notificationService.markAllAsRead(userDetails.getId());
            res.put("success", true);
        } else {
            res.put("success", false);
        }
        return ResponseEntity.ok(res);
    }

    @ResponseBody
    @PostMapping("/api/notifications/{id}/read")
    public ResponseEntity<Map<String, Object>> apiMarkSingleAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Map<String, Object> res = new HashMap<>();
        if (userDetails != null) {
            try {
                notificationService.markAsRead(id, userDetails.getId());
                res.put("success", true);
            } catch (Exception e) {
                res.put("success", false);
                res.put("message", e.getMessage());
            }
        } else {
            res.put("success", false);
        }
        return ResponseEntity.ok(res);
    }
}
