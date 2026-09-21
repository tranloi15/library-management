package com.library.service;

import com.library.dto.TopBookDto;
import java.util.List;

public interface ReportService {

    /**
     * Lấy danh sách top sách được mượn nhiều nhất.
     */
    List<TopBookDto> getTopBorrowedBooks();

    // Các phương thức thống kê/báo cáo khác nếu có...
}