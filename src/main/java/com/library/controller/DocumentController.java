package com.library.controller;

import com.library.model.Book;
import com.library.model.Document;
import com.library.model.DocumentType;
import com.library.model.Magazine;
import com.library.service.DocumentService;
import lombok.RequiredArgsConstructor;
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
            RedirectAttributes redirectAttributes) {

        try {
            Book book = new Book(title, publisher, publishYear, quantity, imageUrl, author, isbn, pageCount, genre, summary);
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
            RedirectAttributes redirectAttributes) {

        try {
            Magazine magazine = new Magazine(title, publisher, publishYear, quantity, imageUrl, issueNumber, publishMonth, summary);
            documentService.saveMagazine(magazine);
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm tạp chí mới thành công! (Số phát hành: #" + issueNumber + ")");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi thêm tạp chí: " + e.getMessage());
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
}
