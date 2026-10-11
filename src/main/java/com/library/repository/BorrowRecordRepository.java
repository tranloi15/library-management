package com.library.repository;

import com.library.model.BorrowRecord;
import com.library.model.BorrowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BorrowRecordRepository extends JpaRepository<BorrowRecord, Long> {

    boolean existsByBookIdAndStatus(Long documentId, BorrowStatus status);

    List<BorrowRecord> findByUserId(Long userId);

    List<BorrowRecord> findByUserIdOrderByBorrowDateDesc(Long userId);

    List<BorrowRecord> findByUserIdAndStatusOrderByBorrowDateDesc(
            Long userId, BorrowStatus status);

    List<BorrowRecord> findByUserIdAndStatusInOrderByBorrowDateDesc(
            Long userId, List<BorrowStatus> statuses);

    List<BorrowRecord> findByStatus(BorrowStatus status);

    List<BorrowRecord> findByStatusIn(List<BorrowStatus> statuses);

    List<BorrowRecord> findByReturnDateBetween(LocalDate from, LocalDate to);

    List<BorrowRecord> findByReturnDateIsNotNull();

    @Query("""
            SELECT br.document, COUNT(br.id)
            FROM BorrowRecord br
            GROUP BY br.document
            ORDER BY COUNT(br.id) DESC
            """)
    List<Object[]> findTopBorrowedBooks();

        @Query("""
            SELECT br.document, COUNT(br.id)
            FROM BorrowRecord br
            WHERE br.borrowDate BETWEEN :from AND :to
            GROUP BY br.document
            ORDER BY COUNT(br.id) DESC
            """)
    List<Object[]> findTopBorrowedBooksBetween(@Param("from") LocalDate from,
                                               @Param("to") LocalDate to);

    @Query("""
            SELECT COUNT(br.id)
            FROM BorrowRecord br
            WHERE br.borrowDate BETWEEN :from AND :to
            """)
    long countBorrowsBetween(@Param("from") LocalDate from,
                             @Param("to") LocalDate to);

}