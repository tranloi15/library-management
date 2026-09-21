package com.library.service.impl;

import com.library.dto.DashboardStatsDto;
import com.library.dto.TopBookDto;
import com.library.model.Book;
import com.library.model.BorrowStatus;
import com.library.model.Document;
import com.library.repository.BorrowRecordRepository;
import com.library.repository.DocumentRepository;
import com.library.repository.UserRepository;
import com.library.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final BorrowRecordRepository borrowRecordRepository;

    @Override
    public DashboardStatsDto getDashboardStats() {

        long totalDocuments = documentRepository.count();

        long totalUsers = userRepository.count();

        long borrowingCount =
                borrowRecordRepository.findByStatus(BorrowStatus.BORROWING).size();

        long overdueCount = borrowRecordRepository
                .findByStatus(BorrowStatus.BORROWING)
                .stream()
                .filter(record -> record.isOverdue())
                .count();

        return new DashboardStatsDto(
                totalDocuments,
                totalUsers,
                borrowingCount,
                overdueCount
        );
    }

    @Override
    public List<TopBookDto> getTopBorrowedBooks(int limit) {

        List<Object[]> results =
                borrowRecordRepository.findTopBorrowedBooks();

        List<TopBookDto> topBooks = new ArrayList<>();

        for (Object[] row : results) {

            Document document = (Document) row[0];
            Long borrowCount = ((Number) row[1]).longValue();

            if (!(document instanceof Book)) {
                continue;
            }

            Book book = (Book) document;

            topBooks.add(new TopBookDto(
                    book.getId(),
                    book.getTitle(),
                    book.getAuthor(),
                    borrowCount
            ));

            if (topBooks.size() >= limit) {
                break;
            }
        }

        return topBooks;
    }
}