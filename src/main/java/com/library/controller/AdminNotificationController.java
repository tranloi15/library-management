package com.library.controller;

import com.library.config.CustomUserDetails;
import com.library.dto.NotificationCampaignDto;
import com.library.model.NotificationType;
import com.library.model.RoleName;
import com.library.model.User;
import com.library.repository.UserRepository;
import com.library.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminNotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @GetMapping
    public String index(Model model) {
        List<User> readers = userRepository.findByRole(RoleName.ROLE_READER).stream()
                .filter(User::isActive)
                .toList();

        List<NotificationCampaignDto> campaigns = notificationService.getSentCampaignSummaries();

        List<Long> overdueUserIds = notificationService.findUserIdsWithOverdueBooks();
        List<Long> unpaidFineUserIds = notificationService.findUserIdsWithUnpaidFines();
        List<Long> newReaderUserIds = notificationService.findNewReaderUserIds(7);

        model.addAttribute("readers", readers);
        model.addAttribute("campaigns", campaigns);
        model.addAttribute("types", NotificationType.values());
        model.addAttribute("overdueCount", overdueUserIds.size());
        model.addAttribute("finesCount", unpaidFineUserIds.size());
        model.addAttribute("newReadersCount", newReaderUserIds.size());

        return "admin/notifications";
    }

    @PostMapping("/send")
    public String sendNotification(
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam NotificationType type,
            @RequestParam(defaultValue = "ALL") String targetScope,
            @RequestParam(name = "recipientIds", required = false) List<Long> recipientIds,
            @RequestParam(name = "targetUrl", required = false) String targetUrl,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        Long senderId = userDetails != null ? userDetails.getId() : null;
        String senderName = userDetails != null ? userDetails.getFullName() : "Ban Quản lý";

        if (title.isBlank() || content.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng nhập đầy đủ tiêu đề và nội dung thông báo!");
            return "redirect:/admin/notifications";
        }

        if ("ALL".equalsIgnoreCase(targetScope)) {
            int sentCount = notificationService.sendBroadcast(senderId, senderName, title.trim(), content.trim(), type, targetUrl);
            redirectAttributes.addFlashAttribute("success", "Đã phát thông báo toàn hệ thống tới " + sentCount + " độc giả!");
        } else {
            if (recipientIds == null || recipientIds.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Vui lòng chọn ít nhất một độc giả nhận thông báo!");
                return "redirect:/admin/notifications";
            }
            notificationService.sendToUsers(senderId, senderName, recipientIds, title.trim(), content.trim(), type, targetUrl);
            redirectAttributes.addFlashAttribute("success", "Đã gửi thông báo thành công tới " + recipientIds.size() + " độc giả được chọn!");
        }

        return "redirect:/admin/notifications";
    }

    @PostMapping("/campaigns/{broadcastId}/delete")
    public String deleteCampaign(
            @PathVariable String broadcastId,
            RedirectAttributes redirectAttributes) {
        notificationService.deleteCampaign(broadcastId);
        redirectAttributes.addFlashAttribute("success", "Đã xóa đợt phát thông báo!");
        return "redirect:/admin/notifications";
    }

    @ResponseBody
    @GetMapping("/smart-recipients")
    public ResponseEntity<Map<String, List<Long>>> getSmartRecipients() {
        Map<String, List<Long>> res = new HashMap<>();
        res.put("overdue", notificationService.findUserIdsWithOverdueBooks());
        res.put("fines", notificationService.findUserIdsWithUnpaidFines());
        res.put("newReaders", notificationService.findNewReaderUserIds(7));
        return ResponseEntity.ok(res);
    }
}
