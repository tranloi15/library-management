package com.library.service;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.library.model.Book;
import com.library.model.Document;
import com.library.service.impl.QrCodeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử dịch vụ sinh mã QR và đóng gói ZIP")
class QrCodeServiceTest {

    private QrCodeService qrCodeService;

    @BeforeEach
    void setUp() {
        qrCodeService = new QrCodeServiceImpl();
    }

    @Test
    @DisplayName("Tạo ảnh mã QR từ chuỗi văn bản bất kỳ thành công")
    void testGenerateQrCodeImageSuccess() throws Exception {
        String testContent = "https://thuvien.edu.vn/test";
        byte[] qrBytes = qrCodeService.generateQrCodeImage(testContent, 300, 300);

        assertNotNull(qrBytes);
        assertTrue(qrBytes.length > 0);

        // Đọc lại nội dung từ ảnh byte array để xác thực tính toàn vẹn
        BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(qrBytes));
        assertNotNull(bufferedImage);
        assertEquals(300, bufferedImage.getWidth());
        assertEquals(300, bufferedImage.getHeight());

        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(bufferedImage)));
        Result result = new MultiFormatReader().decode(bitmap);
        assertEquals(testContent, result.getText());
    }

    @Test
    @DisplayName("Tạo mã QR cho tài liệu chứa đúng đường dẫn tự phục vụ")
    void testGenerateDocumentQrCodeSuccess() throws Exception {
        Book book = new Book();
        book.setId(105L);
        book.setTitle("Lập trình Java Hiện đại");

        byte[] qrBytes = qrCodeService.generateDocumentQrCode(book, 250, 250);
        assertNotNull(qrBytes);

        BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(qrBytes));
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(bufferedImage)));
        Result result = new MultiFormatReader().decode(bitmap);

        assertEquals("/borrow/qr-request?bookId=105", result.getText());
    }

    @Test
    @DisplayName("Ném ngoại lệ khi tài liệu không hợp lệ hoặc thiếu ID")
    void testGenerateDocumentQrCodeInvalid() {
        assertThrows(IllegalArgumentException.class, () -> qrCodeService.generateDocumentQrCode(null, 200, 200));

        Book bookWithoutId = new Book();
        bookWithoutId.setTitle("Sách chưa lưu");
        assertThrows(IllegalArgumentException.class, () -> qrCodeService.generateDocumentQrCode(bookWithoutId, 200, 200));
    }

    @Test
    @DisplayName("Đóng gói nhiều mã QR vào file ZIP an toàn và đầy đủ")
    void testGenerateBulkQrZipSuccess() throws IOException {
        Book book1 = new Book();
        book1.setId(1L);
        book1.setTitle("Lập trình Web");

        Book book2 = new Book();
        book2.setId(2L);
        book2.setTitle("Cấu trúc dữ liệu & Giải thuật");

        List<Document> docs = Arrays.asList(book1, book2);
        byte[] zipBytes = qrCodeService.generateBulkQrZip(docs);

        assertNotNull(zipBytes);
        assertTrue(zipBytes.length > 0);

        // Giải nén kiểm tra các file bên trong ZIP
        int entryCount = 0;
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entryCount++;
                assertTrue(entry.getName().endsWith(".png"));
                zis.closeEntry();
            }
        }
        assertEquals(2, entryCount);
    }

    @Test
    @DisplayName("Tạo file ZIP rỗng khi danh sách tài liệu trống")
    void testGenerateBulkQrZipEmptyList() throws IOException {
        byte[] zipBytes = qrCodeService.generateBulkQrZip(Collections.emptyList());
        assertNotNull(zipBytes);
        assertTrue(zipBytes.length > 0);
    }
}
