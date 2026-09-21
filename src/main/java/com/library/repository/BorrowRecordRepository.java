package com.library.repository;

import com.library.model.BorrowRecord;
import com.library.model.BorrowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BorrowRecordRepository extends JpaRepository<BorrowRecord, Long> {

    boolean existsByBookIdAndStatus(Long documentId, BorrowStatus status);

    List<BorrowRecord> findByUserIdOrderByBorrowDateDesc(Long userId);

    List<BorrowRecord> findByUserIdAndStatusOrderByBorrowDateDesc(
            Long userId, BorrowStatus status);

    List<BorrowRecord> findByStatus(BorrowStatus status);

    @Query("""
            SELECT br.document, COUNT(br.id)
            FROM BorrowRecord br
            GROUP BY br.document
            ORDER BY COUNT(br.id) DESC
            """)
    List<Object[]> findTopBorrowedBooks();
}
