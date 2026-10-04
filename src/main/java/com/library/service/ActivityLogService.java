package com.library.service;

import com.library.model.ActionType;
import com.library.model.ActivityLog;
import com.library.model.TargetType;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ActivityLogService {

    void log(ActionType actionType, TargetType targetType, Long targetId, String targetName, String description, String details);

    void log(ActionType actionType, TargetType targetType, Long targetId, String targetName, String description);

    void logSystem(ActionType actionType, TargetType targetType, Long targetId, String targetName, String description, String details);

    Page<ActivityLog> getLogs(ActionType actionType, TargetType targetType, String keyword, String dateFilter, int page, int size);

    long countToday();

    long countUsersToday();

    long countBorrowToday();

    long countDocumentToday();

    List<ActivityLog> getRecentLogs();
}
