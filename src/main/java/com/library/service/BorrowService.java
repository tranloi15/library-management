package com.library.service;

import com.library.model.BorrowRecord;
import com.library.model.BorrowStatus;
import com.library.repository.BorrowRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BorrowService {

    private final BorrowRecordRepository borrowRecordRepository;

    public List<BorrowRecord> getActiveBorrowsByUser(Long userId) {
        return borrowRecordRepository.findByUserIdAndStatusOrderByBorrowDateDesc(userId, BorrowStatus.BORROWING);
    }

    public List<BorrowRecord> getReturnedBorrowsByUser(Long userId) {
        return borrowRecordRepository.findByUserIdAndStatusOrderByBorrowDateDesc(userId, BorrowStatus.RETURNED);
    }

    public List<BorrowRecord> getBorrowHistoryByUser(Long userId) {
        return borrowRecordRepository.findByUserIdOrderByBorrowDateDesc(userId);
    }
}
