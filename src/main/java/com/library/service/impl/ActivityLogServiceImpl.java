package com.library.service.impl;

import com.library.model.ActionType;
import com.library.model.ActivityLog;
import com.library.model.TargetType;
import com.library.model.User;
import com.library.repository.ActivityLogRepository;
import com.library.repository.UserRepository;
import com.library.service.ActivityLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityLogServiceImpl implements ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(ActionType actionType, TargetType targetType, Long targetId, String targetName, String description, String details) {
        try {
            Long userId = null;
            String username = "system";
            String userFullName = "Hệ thống tự động";
            String userRole = "SYSTEM";

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                String authUsername = auth.getName();
                username = authUsername;
                User user = userRepository.findByUsername(authUsername).orElse(null);
                if (user != null) {
                    userId = user.getId();
                    userFullName = user.getFullName() != null ? user.getFullName() : authUsername;
                    userRole = user.getRole() != null ? user.getRole().name() : "AUTHENTICATED";
                } else {
                    userFullName = authUsername;
                    userRole = "AUTHENTICATED";
                }
            }

            ActivityLog entry = ActivityLog.builder()
                    .userId(userId)
                    .username(username)
                    .userFullName(userFullName)
                    .userRole(userRole)
                    .actionType(actionType)
                    .targetType(targetType)
                    .targetId(targetId)
                    .targetName(targetName)
                    .description(description)
                    .details(details)
                    .ipAddress(getClientIp())
                    .createdAt(LocalDateTime.now())
                    .build();

            activityLogRepository.save(entry);
        } catch (Exception ex) {
            log.error("Lỗi khi ghi nhật ký hoạt động: {}", ex.getMessage(), ex);
        }
    }

    @Override
    public void log(ActionType actionType, TargetType targetType, Long targetId, String targetName, String description) {
        log(actionType, targetType, targetId, targetName, description, null);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSystem(ActionType actionType, TargetType targetType, Long targetId, String targetName, String description, String details) {
        try {
            ActivityLog entry = ActivityLog.builder()
                    .userId(null)
                    .username("system")
                    .userFullName("Hệ thống tự động")
                    .userRole("SYSTEM")
                    .actionType(actionType)
                    .targetType(targetType)
                    .targetId(targetId)
                    .targetName(targetName)
                    .description(description)
                    .details(details)
                    .ipAddress("127.0.0.1")
                    .createdAt(LocalDateTime.now())
                    .build();

            activityLogRepository.save(entry);
        } catch (Exception ex) {
            log.error("Lỗi khi ghi nhật ký hệ thống: {}", ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ActivityLog> getLogs(ActionType actionType, TargetType targetType, String keyword, String dateFilter, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));

        LocalDateTime startDate = null;
        LocalDateTime endDate = null;
        LocalDate today = LocalDate.now();

        if ("TODAY".equalsIgnoreCase(dateFilter)) {
            startDate = today.atStartOfDay();
            endDate = today.atTime(LocalTime.MAX);
        } else if ("WEEK".equalsIgnoreCase(dateFilter)) {
            startDate = today.minusDays(7).atStartOfDay();
            endDate = today.atTime(LocalTime.MAX);
        } else if ("MONTH".equalsIgnoreCase(dateFilter)) {
            startDate = today.withDayOfMonth(1).atStartOfDay();
            endDate = today.atTime(LocalTime.MAX);
        }

        String searchKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;

        return activityLogRepository.filterLogs(actionType, targetType, searchKeyword, startDate, endDate, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public long countToday() {
        LocalDate today = LocalDate.now();
        return activityLogRepository.countByCreatedAtBetween(today.atStartOfDay(), today.atTime(LocalTime.MAX));
    }

    @Override
    @Transactional(readOnly = true)
    public long countUsersToday() {
        LocalDate today = LocalDate.now();
        return activityLogRepository.countDistinctUsersByCreatedAtBetween(today.atStartOfDay(), today.atTime(LocalTime.MAX));
    }

    @Override
    @Transactional(readOnly = true)
    public long countBorrowToday() {
        LocalDate today = LocalDate.now();
        return activityLogRepository.countByTargetTypeAndCreatedAtBetween(TargetType.BORROW_RECORD, today.atStartOfDay(), today.atTime(LocalTime.MAX));
    }

    @Override
    @Transactional(readOnly = true)
    public long countDocumentToday() {
        LocalDate today = LocalDate.now();
        return activityLogRepository.countByTargetTypeAndCreatedAtBetween(TargetType.DOCUMENT, today.atStartOfDay(), today.atTime(LocalTime.MAX));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityLog> getRecentLogs() {
        return activityLogRepository.findTop10ByOrderByCreatedAtDesc();
    }

    private String getClientIp() {
        try {
            RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
            if (attributes instanceof ServletRequestAttributes servletAttributes) {
                HttpServletRequest request = servletAttributes.getRequest();
                String xfHeader = request.getHeader("X-Forwarded-For");
                if (xfHeader != null && !xfHeader.isEmpty() && !"unknown".equalsIgnoreCase(xfHeader)) {
                    return xfHeader.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception ignored) {
        }
        return "127.0.0.1";
    }
}
