package com.library.controller;

import com.library.model.Book;
import com.library.model.Document;
import com.library.model.DocumentType;
import com.library.model.Magazine;
import com.library.service.DocumentService;
import com.library.service.QrCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final QrCodeService qrCodeService;

    @GetMapping
    public String listDocuments(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "status", required = false) String status,
            Model model) {

        List<Document> allDocs = documentService.getAllDocuments();

        // Thống kê KPI tổng thể
        long totalDocuments = allDocs.size();
        long totalBooks = allDocs.stream().filter(d -> d.getDocumentType() == DocumentType.BOOK).count();
        long totalMagazines = allDocs.stream().filter(d -> d.getDocumentType() == DocumentType.MAGAZINE).count();
        long totalStockQuantity = allDocs.stream().mapToLong(Document::getQuantity).sum();
        long inStockCount = allDocs.stream().filter(Document::isAvailable).count();
        long outOfStockCount = allDocs.stream().filter(d -> !d.isAvailable()).count();

        // Lọc đa hình qua Service
        List<Document> filtered = documentService.searchDocuments(keyword, type, status);

        model.addAttribute("documents", filtered);
        model.addAttribute("keyword", keyword != null ? keyword.trim() : "");
        model.addAttribute("selectedType", type != null ? type.trim().toUpperCase() : "ALL");
        model.addAttribute("selectedStatus", status != null ? status.trim().toUpperCase() : "ALL");

        model.addAttribute("totalDocuments", totalDocuments);
        model.addAttribute("totalBooks", totalBooks);
        model.addAttribute("totalMagazines", totalMagazines);
        model.addAttribute("totalStockQuantity", totalStockQuantity);
        model.addAttribute("inStockCount", inStockCount);
        model.addAttribute("outOfStockCount", outOfStockCount);

        model.addAttribute("activeMenu", "documents");
        model.addAttribute("pageTitle", "Quản Lý Kho Tài Liệu");

        return "documents/list";
    }

    @PostMapping("/add-book")
    public String addBook(
            @RequestParam("title") String title,
            @RequestParam("author") String author,
            @RequestParam("isbn") String isbn,
            @RequestParam(value = "publisher", defaultValue = "NXB Bưu Điện") String publisher,
            @RequestParam(value = "publishYear", defaultValue = "2024") int publishYear,
            @RequestParam(value = "pageCount", defaultValue = "300") int pageCount,
            @RequestParam(value = "genre", defaultValue = "Giáo trình Công nghệ") String genre,
            @RequestParam(value = "quantity", defaultValue = "5") int quantity,
            @RequestParam(value = "imageUrl", required = false) String imageUrl,
            @RequestParam(value = "summary", required = false) String summary,
            @RequestParam(value = "shelfLocation", required = false) String shelfLocation,
            RedirectAttributes redirectAttributes) {

        try {
            Book book = new Book(title, publisher, publishYear, quantity, imageUrl, author, isbn, pageCount, genre, summary);
            book.setShelfLocation(shelfLocation);
            documentService.saveBook(book);
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm sách mới thành công! (Mã ISBN: " + isbn + ")");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi thêm sách: " + e.getMessage());
        }

        return "redirect:/documents";
    }

    @PostMapping("/add-magazine")
    public String addMagazine(
            @RequestParam("title") String title,
            @RequestParam(value = "publisher", defaultValue = "NXB Khoa Học Kỹ Thuật") String publisher,
            @RequestParam(value = "publishYear", defaultValue = "2026") int publishYear,
            @RequestParam(value = "issueNumber", defaultValue = "1") int issueNumber,
            @RequestParam(value = "publishMonth", defaultValue = "1") int publishMonth,
            @RequestParam(value = "quantity", defaultValue = "10") int quantity,
            @RequestParam(value = "imageUrl", required = false) String imageUrl,
            @RequestParam(value = "summary", required = false) String summary,
            @RequestParam(value = "shelfLocation", required = false) String shelfLocation,
            RedirectAttributes redirectAttributes) {

        try {
            Magazine magazine = new Magazine(title, publisher, publishYear, quantity, imageUrl, issueNumber, publishMonth, summary);
            magazine.setShelfLocation(shelfLocation);
            documentService.saveMagazine(magazine);
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm tạp chí mới thành công! (Số phát hành: #" + issueNumber + ")");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi thêm tạp chí: " + e.getMessage());
        }

        return "redirect:/documents";
    }

    @PostMapping("/{id}/update-shelf")
    public String updateShelfLocation(
            @PathVariable("id") Long id,
            @RequestParam(value = "shelfLocation", required = false) String shelfLocation,
            RedirectAttributes redirectAttributes) {

        try {
            documentService.updateShelfLocation(id, shelfLocation);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật vị trí kệ cho tài liệu #" + id + " thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi cập nhật vị trí: " + e.getMessage());
        }

        return "redirect:/documents";
    }

    @PostMapping("/{id}/update-stock")
    public String updateStock(
            @PathVariable("id") Long id,
            @RequestParam("quantity") int quantity,
            RedirectAttributes redirectAttributes) {

        try {
            documentService.setStockQuantity(id, quantity);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật tồn kho tài liệu #" + id + " thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi cập nhật kho: " + e.getMessage());
        }

        return "redirect:/documents";
    }

    @PostMapping("/{id}/delete")
    public String deleteDocument(
            @PathVariable("id") Long id,
            RedirectAttributes redirectAttributes) {

        try {
            Document doc = documentService.getDocumentById(id);
            String title = doc.getTitle();
            documentService.deleteDocument(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa tài liệu '" + title + "' thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa: " + e.getMessage());
        }

        return "redirect:/documents";
    }

    @GetMapping(value = "/{id}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> getDocumentQrCode(
            @PathVariable Long id,
            @RequestParam(defaultValue = "300") int size) {
        try {
            Document doc = documentService.getDocumentById(id);
            byte[] qrBytes = qrCodeService.generateDocumentQrCode(doc, size, size);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                    .body(qrBytes);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping(value = "/{id}/qr/download")
    public ResponseEntity<byte[]> downloadDocumentQrCode(@PathVariable Long id) {
        try {
            Document doc = documentService.getDocumentById(id);
            byte[] qrBytes = qrCodeService.generateDocumentQrCode(doc, 400, 400);
            String filename = String.format("QR_BK_%04d.png", doc.getId());
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(qrBytes);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/qr/bulk-zip")
    public ResponseEntity<byte[]> downloadBulkQrZip(@RequestParam(name = "documentIds", required = false) List<Long> documentIds) {
        try {
            List<Document> docs;
            if (documentIds == null || documentIds.isEmpty()) {
                docs = documentService.getAllDocuments();
            } else {
                docs = documentIds.stream().map(documentService::getDocumentById).toList();
            }
            byte[] zipBytes = qrCodeService.generateBulkQrZip(docs);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/zip"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"qr_codes_library.zip\"")
                    .body(zipBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
