package com.library.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử tính Đa hình và Đóng gói của Document")
class DocumentPolymorphismTest {

    @Test
    @DisplayName("Kiểm thử đa hình getDocumentDetails() và getIdentifierCode() của Book")
    void testBookPolymorphism() {
        Document book = new Book("Clean Code", "Prentice Hall", 2008, 10, "/img.jpg",
                "Robert C. Martin", "9780132350884", 464, "Software Engineering");

        // Gọi phương thức đa hình thông qua biến tham chiếu Document
        assertEquals("9780132350884", book.getIdentifierCode());
        assertTrue(book.getDocumentDetails().contains("Robert C. Martin"));
        assertTrue(book.getDocumentDetails().contains("464"));
        assertTrue(book.isAvailable());
        assertEquals(DocumentType.BOOK, book.getDocumentType());
    }

    @Test
    @DisplayName("Kiểm thử đa hình getDocumentDetails() và getIdentifierCode() của Magazine")
    void testMagazinePolymorphism() {
        Document mag = new Magazine("Tạp chí Tin học & Điều khiển", "Viện Hàn Lâm KH&CN", 2026, 5, "/mag.jpg", 1, 3);

        // Gọi phương thức đa hình thông qua biến tham chiếu Document
        assertEquals("MAG-2026-01", mag.getIdentifierCode());
        assertTrue(mag.getDocumentDetails().contains("Số phát hành: 1"));
        assertTrue(mag.getDocumentDetails().contains("Tháng 3/2026"));
        assertTrue(mag.isAvailable());
        assertEquals(DocumentType.MAGAZINE, mag.getDocumentType());
    }

    @Test
    @DisplayName("Kiểm thử đóng gói: isAvailable trả về false khi quantity = 0")
    void testDocumentAvailabilityWhenOutOfStock() {
        Document book = new Book();
        book.setQuantity(0);

        assertFalse(book.isAvailable());
    }

    @Test
    @DisplayName("Kiểm thử đa hình getAuthor() và isBorrowable()")
    void testPolymorphicAuthorAndBorrowable() {
        Document book = new Book("Effective Java", "Addison-Wesley", 2018, 5, "/img.jpg",
                "Joshua Bloch", "9780134685991", 416, "Programming");
        Document mag = new Magazine("National Geographic", "NXB Trẻ", 2026, 3, "/img.jpg", 45, 6);

        assertEquals("Joshua Bloch", book.getAuthor());
        assertTrue(book.isBorrowable());

        assertTrue(mag.getAuthor().contains("NXB Trẻ"));
        assertFalse(mag.isBorrowable());
    }

    @Test
    @DisplayName("Kiểm thử đóng gói shelfLocation: tự động trim và gán null khi rỗng")
    void testShelfLocationEncapsulation() {
        Document doc = new Book();
        doc.setShelfLocation("   Kệ A1 - Tầng 2   ");
        assertEquals("Kệ A1 - Tầng 2", doc.getShelfLocation());

        doc.setShelfLocation("   ");
        assertNull(doc.getShelfLocation());

        doc.setShelfLocation(null);
        assertNull(doc.getShelfLocation());
    }

    @Test
    @DisplayName("Kiểm thử matchesKeyword tìm kiếm chính xác qua các trường thông tin")
    void testMatchesKeyword() {
        Document book = new Book("Clean Code", "Prentice Hall", 2008, 10, "/img.jpg",
                "Robert C. Martin", "9780132350884", 464, "Software Engineering");
        book.setShelfLocation("Kệ B1");

        assertTrue(book.matchesKeyword("clean"));
        assertTrue(book.matchesKeyword("robert"));
        assertTrue(book.matchesKeyword("9780132350884"));
        assertTrue(book.matchesKeyword("Kệ B1"));
        assertFalse(book.matchesKeyword("NonExistentTermXYZ"));
    }
}
