package com.library.repository;

import com.library.model.ActionType;
import com.library.model.ActivityLog;
import com.library.model.TargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    @Query("SELECT a FROM ActivityLog a WHERE " +
           "(:actionType IS NULL OR a.actionType = :actionType) AND " +
           "(:targetType IS NULL OR a.targetType = :targetType) AND " +
           "(:keyword IS NULL OR LOWER(a.username) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "    OR LOWER(a.userFullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "    OR LOWER(a.description) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "    OR LOWER(COALESCE(a.targetName, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:startDate IS NULL OR a.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR a.createdAt <= :endDate) " +
           "ORDER BY a.createdAt DESC")
    Page<ActivityLog> filterLogs(
            @Param("actionType") ActionType actionType,
            @Param("targetType") TargetType targetType,
            @Param("keyword") String keyword,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT COUNT(DISTINCT a.username) FROM ActivityLog a WHERE a.createdAt BETWEEN :start AND :end")
    long countDistinctUsersByCreatedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    long countByTargetTypeAndCreatedAtBetween(TargetType targetType, LocalDateTime start, LocalDateTime end);

    List<ActivityLog> findTop10ByOrderByCreatedAtDesc();
}
