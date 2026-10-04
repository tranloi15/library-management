package com.library.controller;

import com.library.model.ActionType;
import com.library.model.ActivityLog;
import com.library.model.TargetType;
import com.library.service.ActivityLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/activity-logs")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class ActivityLogController {

    private final ActivityLogService activityLogService;

    @GetMapping
    public String viewLogs(
            @RequestParam(name = "actionType", required = false) ActionType actionType,
            @RequestParam(name = "targetType", required = false) TargetType targetType,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "dateFilter", defaultValue = "ALL") String dateFilter,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "15") int size,
            Model model) {

        Page<ActivityLog> logPage = activityLogService.getLogs(actionType, targetType, keyword, dateFilter, page, size);

        model.addAttribute("logs", logPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", logPage.getTotalPages());
        model.addAttribute("totalItems", logPage.getTotalElements());

        // Tiêu chí tìm kiếm giữ lại trên giao diện
        model.addAttribute("selectedActionType", actionType);
        model.addAttribute("selectedTargetType", targetType);
        model.addAttribute("keyword", keyword);
        model.addAttribute("dateFilter", dateFilter);

        // Danh mục lọc
        model.addAttribute("allActionTypes", ActionType.values());
        model.addAttribute("allTargetTypes", TargetType.values());

        // Thống kê nhanh trên đầu trang
        model.addAttribute("countToday", activityLogService.countToday());
        model.addAttribute("countUsersToday", activityLogService.countUsersToday());
        model.addAttribute("countBorrowToday", activityLogService.countBorrowToday());
        model.addAttribute("countDocumentToday", activityLogService.countDocumentToday());

        model.addAttribute("activeMenu", "activity_logs");

        return "admin/activity_logs";
    }
}
