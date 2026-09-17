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
        BorrowRecord record = new BorrowRecord(1L, 1L, today.minusDays(3), today.plusDays(7), "BORROWING");

        assertFalse(record.isOverdue());
        assertTrue(record.getDaysRemaining() >= 6);
        assertEquals(0, record.getOverdueDays());
    }

    @Test
    @DisplayName("Kiểm thử sách đã quá hạn")
    void testOverdue() {
        LocalDate today = LocalDate.now();
        BorrowRecord record = new BorrowRecord(1L, 1L, today.minusDays(15), today.minusDays(5), "BORROWING");

        assertTrue(record.isOverdue());
        assertEquals(5, record.getOverdueDays());
    }

    @Test
    @DisplayName("Kiểm thử sách đã trả thì không tính là quá hạn")
    void testReturnedNotOverdue() {
        LocalDate today = LocalDate.now();
        BorrowRecord record = new BorrowRecord(1L, 1L, today.minusDays(20), today.minusDays(5), today.minusDays(6), "RETURNED");

        assertFalse(record.isOverdue());
    }
}
