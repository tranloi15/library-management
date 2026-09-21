package com.library.service.impl;

import com.library.dto.TopBookDto;
import com.library.enums.DocumentType;
import com.library.repository.BorrowRecordRepository;
import com.library.service.ReportService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReportServiceImpl implements ReportService {

    private final BorrowRecordRepository borrowRecordRepository;

    public ReportServiceImpl(BorrowRecordRepository borrowRecordRepository) {
        this.borrowRecordRepository = borrowRecordRepository;
    }

    @Override
    public List<TopBookDto> getTopBorrowedBooks() {
        // Truyền Enum DocumentType.BOOK trực tiếp vào phương thức của repository
        return borrowRecordRepository.findTopBorrowedBooks(DocumentType.BOOK);
    }
}