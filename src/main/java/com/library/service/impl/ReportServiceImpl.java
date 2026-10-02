package com.library.service.impl;

import com.library.dto.DashboardStatsDto;
import com.library.dto.TopBookDto;
import com.library.model.Book;
import com.library.model.BorrowRecord;
import com.library.model.BorrowStatus;
import com.library.repository.BorrowRecordRepository;
import com.library.repository.DocumentRepository;
import com.library.repository.UserRepository;
import com.library.service.BorrowService;
import com.library.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final BorrowService borrowService;

    @Override
    public DashboardStatsDto getDashboardStats() {

        borrowService.updateOverdue();

        List<BorrowRecord> borrowingRecords =
                borrowRecordRepository.findByStatus(BorrowStatus.BORROWING);

        List<BorrowRecord> overdueRecords =
                borrowRecordRepository.findByStatus(BorrowStatus.OVERDUE);

        return new DashboardStatsDto(
                documentRepository.count(),
                userRepository.count(),
                borrowingRecords.size(),
                overdueRecords.size()
        );
    }

    /** Top sách mượn nhiều nhất trên toàn bộ lịch sử. */
    @Override
    public List<TopBookDto> getTopBorrowedBooks(int limit) {

        if (limit <= 0) {
            return List.of();
        }

        return toTopBookDtos(borrowRecordRepository.findTopBorrowedBooks(), limit);
    }

    /** Top sách mượn nhiều nhất trong khoảng ngày [from, to]. */
    @Override
    public List<TopBookDto> getTopBorrowedBooks(LocalDate from, LocalDate to, int limit) {

        if (limit <= 0 || from == null || to == null || from.isAfter(to)) {
            return List.of();
        }

        return toTopBookDtos(borrowRecordRepository.findTopBorrowedBooksBetween(from, to), limit);
    }

    /** Tổng số lượt mượn trong khoảng ngày [from, to]. */
    @Override
    public long countBorrowsBetween(LocalDate from, LocalDate to) {

        if (from == null || to == null || from.isAfter(to)) {
            return 0;
        }

        return borrowRecordRepository.countBorrowsBetween(from, to);
    }

    @Override
    public List<BorrowRecord> getOverdueRecords() {

        borrowService.updateOverdue();

        return borrowRecordRepository
                .findByStatus(BorrowStatus.OVERDUE);
    }

    /**
     * Chuyển kết quả truy vấn [Document, số lượt mượn] thành danh sách TopBookDto.
     * Chỉ lấy Sách (bỏ Tạp chí), kèm ISBN và ảnh bìa để giao diện hiển thị.
     */
    private List<TopBookDto> toTopBookDtos(List<Object[]> rows, int limit) {

        return rows.stream()
                .filter(row -> row[0] instanceof Book)
                .limit(limit)
                .map(row -> {

                    Book book = (Book) row[0];

                    Long borrowCount = ((Number) row[1]).longValue();

                    return new TopBookDto(
                            book.getId(),
                            book.getTitle(),
                            book.getAuthor(),
                            borrowCount,
                            book.getIsbn(),
                            book.getImageUrl()
                    );
                })
                .toList();
    }
}