package com.library.service.impl;

import com.library.dto.DashboardStatsDto;
import com.library.dto.TopBookDto;
import com.library.model.Book;
import com.library.model.BorrowRecord;
import com.library.model.BorrowStatus;
import com.library.model.Document;
import com.library.repository.BorrowRecordRepository;
import com.library.repository.DocumentRepository;
import com.library.repository.UserRepository;
import com.library.service.BorrowService;
import com.library.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

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

    @Override
    public List<TopBookDto> getTopBorrowedBooks(int limit) {

        if (limit <= 0) {
            return List.of();
        }

        return borrowRecordRepository.findTopBorrowedBooks()
                .stream()
                .filter(row -> row[0] instanceof Book)
                .limit(limit)
                .map(row -> {

                    Book book = (Book) row[0];

                    Long borrowCount =
                            ((Number) row[1]).longValue();

                    return new TopBookDto(
                            book.getId(),
                            book.getTitle(),
                            book.getAuthor(),
                            borrowCount
                    );
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<BorrowRecord> getOverdueRecords() {

        borrowService.updateOverdue();

        return borrowRecordRepository
                .findByStatus(BorrowStatus.OVERDUE);
    }
}