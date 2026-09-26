package com.library.service;

import com.library.dto.DashboardStatsDto;
import com.library.dto.TopBookDto;
import com.library.model.BorrowRecord;

import java.util.List;

public interface ReportService {

    DashboardStatsDto getDashboardStats();

    List<TopBookDto> getTopBorrowedBooks(int limit);

    List<BorrowRecord> getOverdueRecords();
}