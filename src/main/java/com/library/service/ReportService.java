package com.library.service;

import com.library.dto.DashboardStatsDto;
import com.library.dto.TopBookDto;

import java.util.List;

public interface ReportService {

    DashboardStatsDto getDashboardStats();

    List<TopBookDto> getTopBorrowedBooks(int limit);
}