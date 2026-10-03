package com.library.service;

import com.library.dto.DashboardStatsDto;
import com.library.dto.TopBookDto;
import com.library.model.BorrowRecord;

import java.time.LocalDate;
import java.util.List;

public interface ReportService {

    /** Số liệu tổng quan cho Bảng điều khiển. */
    DashboardStatsDto getDashboardStats();

    /** Top sách mượn nhiều nhất trên toàn bộ lịch sử. */
    List<TopBookDto> getTopBorrowedBooks(int limit);

    /** Top sách mượn nhiều nhất trong khoảng ngày [from, to]. */
    List<TopBookDto> getTopBorrowedBooks(LocalDate from, LocalDate to, int limit);

    /** Tổng số lượt mượn trong khoảng ngày [from, to]. */
    long countBorrowsBetween(LocalDate from, LocalDate to);

    /** Danh sách phiếu mượn đang quá hạn. */
    List<BorrowRecord> getOverdueRecords();
}