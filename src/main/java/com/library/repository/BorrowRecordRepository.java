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

    List<BorrowRecord> findByUserIdOrderByBorrowDateDesc(Long userId);

    List<BorrowRecord> findByUserIdAndStatusOrderByBorrowDateDesc(
            Long userId, BorrowStatus status);

    List<BorrowRecord> findByUserIdAndStatusInOrderByBorrowDateDesc(
            Long userId, List<BorrowStatus> statuses);

    List<BorrowRecord> findByStatus(BorrowStatus status);

    List<BorrowRecord> findByStatusIn(List<BorrowStatus> statuses);

    /** Phiếu có ngày trả trong khoảng [from, to], dùng cho báo cáo tiền phạt. */
    List<BorrowRecord> findByReturnDateBetween(LocalDate from, LocalDate to);

    /** Tất cả phiếu đã trả, dùng khi xem tiền phạt toàn bộ thời gian. */
    List<BorrowRecord> findByReturnDateIsNotNull();

    /** Top tài liệu được mượn nhiều nhất trên toàn bộ lịch sử. */
    @Query("""
            SELECT br.document, COUNT(br.id)
            FROM BorrowRecord br
            GROUP BY br.document
            ORDER BY COUNT(br.id) DESC
            """)
    List<Object[]> findTopBorrowedBooks();

    /**
     * Top tài liệu được mượn nhiều nhất trong khoảng ngày [from, to]
     * (tính theo ngày mượn, gồm cả hai đầu mút).
     * Mỗi phần tử: [0] = Document, [1] = Long số lượt mượn.
     */
    @Query("""
            SELECT br.document, COUNT(br.id)
            FROM BorrowRecord br
            WHERE br.borrowDate BETWEEN :from AND :to
            GROUP BY br.document
            ORDER BY COUNT(br.id) DESC
            """)
    List<Object[]> findTopBorrowedBooksBetween(@Param("from") LocalDate from,
                                               @Param("to") LocalDate to);

    /** Tổng số lượt mượn trong khoảng ngày [from, to], dùng cho thẻ thống kê. */
    @Query("""
            SELECT COUNT(br.id)
            FROM BorrowRecord br
            WHERE br.borrowDate BETWEEN :from AND :to
            """)
    long countBorrowsBetween(@Param("from") LocalDate from,
                             @Param("to") LocalDate to);

}