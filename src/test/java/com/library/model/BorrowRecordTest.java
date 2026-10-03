package com.library.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử logic đóng gói trong BorrowRecord")
class BorrowRecordTest {

    @Test
    @DisplayName("Kiểm thử sách chưa quá hạn")
    void testNotOverdue() {
        LocalDate today = LocalDate.now();
        BorrowRecord record = new BorrowRecord(1L, 1L, today.minusDays(3), today.plusDays(7), BorrowStatus.BORROWING);

        assertFalse(record.isOverdue());
        assertTrue(record.getDaysRemaining() >= 6);
        assertEquals(0, record.getOverdueDays());
    }

    @Test
    @DisplayName("Kiểm thử sách đã quá hạn")
    void testOverdue() {
        LocalDate today = LocalDate.now();
        BorrowRecord record = new BorrowRecord(1L, 1L, today.minusDays(15), today.minusDays(5), BorrowStatus.BORROWING);

        assertTrue(record.isOverdue());
        assertEquals(5, record.getOverdueDays());
    }

    @Test
    @DisplayName("Kiểm thử sách đã trả thì không tính là quá hạn")
    void testReturnedNotOverdue() {
        LocalDate today = LocalDate.now();
        BorrowRecord record = new BorrowRecord(1L, 1L, today.minusDays(20), today.minusDays(5), today.minusDays(6),
                BorrowStatus.RETURNED);

        assertFalse(record.isOverdue());
    }

    @Test
    @DisplayName("Kiểm thử sách có trạng thái OVERDUE vẫn giữ đúng số ngày quá hạn và tiền phạt")
    void testOverdueWithStatusOverdue() {
        LocalDate today = LocalDate.now();
        BorrowRecord record = new BorrowRecord(1L, 1L, today.minusDays(18), today.minusDays(8), BorrowStatus.OVERDUE);

        assertTrue(record.isOverdue());
        assertEquals(8, record.getOverdueDays());
        assertEquals(40000L, record.calculateLateFine(5000L));
    }

    @Test
    @DisplayName("Kiểm thử quy trình hoàn tất trả sách kèm ghi nhận tiền phạt và phương thức thanh toán")
    void testReturnDocumentWithFineAndPaymentMethod() {
        LocalDate today = LocalDate.now();
        BorrowRecord record = new BorrowRecord(1L, 1L, today.minusDays(10), today.minusDays(2), BorrowStatus.OVERDUE);

        record.returnDocument(10000L, "QR_CODE", "Thanh toán qua VietQR");

        assertEquals(BorrowStatus.RETURNED, record.getStatus());
        assertEquals(today, record.getReturnDate());
        assertEquals(10000L, record.getFineAmount());
        assertEquals("QR_CODE", record.getPaymentMethod());
        assertEquals("Thanh toán qua VietQR", record.getNote());
        assertFalse(record.isOverdue());
    }
}
