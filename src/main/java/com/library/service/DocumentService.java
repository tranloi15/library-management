package com.library.service;

import com.library.model.ActionType;
import com.library.model.Book;
import com.library.model.BorrowStatus;
import com.library.model.Document;
import com.library.model.Magazine;
import com.library.model.TargetType;
import com.library.repository.BookRepository;
import com.library.repository.BorrowRecordRepository;
import com.library.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final BookRepository bookRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final ActivityLogService activityLogService;

    public List<Document> getAllDocuments() {
        return documentRepository.findAll();
    }

    public Document getDocumentById(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài liệu ID: " + id));
    }

    public List<Document> searchDocuments(String keyword) {
        return searchDocuments(keyword, null, null);
    }

    /**
     * Tìm kiếm và lọc tài liệu áp dụng đa hình OOP (Polymorphism)
     * Tận dụng getIdentifierCode(), getDocumentDetails() và isAvailable() của các
     * lớp con
     */
    public List<Document> searchDocuments(String keyword, String type, String status) {
        List<Document> allDocs = documentRepository.findAll();

        return allDocs.stream()
                .filter(d -> d.matchesKeyword(keyword))
                .filter(d -> {
                    if (type == null || type.trim().isEmpty() || "ALL".equalsIgnoreCase(type))
                        return true;
                    return d.getDocumentType() != null && d.getDocumentType().name().equalsIgnoreCase(type.trim());
                })
                .filter(d -> {
                    if (status == null || status.trim().isEmpty() || "ALL".equalsIgnoreCase(status))
                        return true;
                    if ("IN_STOCK".equalsIgnoreCase(status))
                        return d.isAvailable();
                    if ("OUT_OF_STOCK".equalsIgnoreCase(status))
                        return !d.isAvailable();
                    return true;
                })
                .toList();
    }

    @Transactional
    public Book saveBook(Book book) {
        if (book == null) {
            throw new IllegalArgumentException("Thông tin sách không được để trống!");
        }
        boolean isNew = (book.getId() == null);
        if (book.getIsbn() != null && isNew && bookRepository.existsByIsbn(book.getIsbn())) {
            throw new IllegalArgumentException("Mã ISBN '" + book.getIsbn() + "' đã tồn tại trong hệ thống!");
        }
        Book saved = documentRepository.save(book);
        if (isNew) {
            activityLogService.log(ActionType.BOOK_CREATE, TargetType.DOCUMENT, saved.getId(), saved.getTitle(),
                    "Thêm mới sách: " + saved.getTitle() + " (ISBN: " + saved.getIsbn() + ")");
        } else {
            activityLogService.log(ActionType.BOOK_UPDATE, TargetType.DOCUMENT, saved.getId(), saved.getTitle(),
                    "Cập nhật thông tin sách: " + saved.getTitle());
        }
        return saved;
    }

    @Transactional
    public Magazine saveMagazine(Magazine magazine) {
        if (magazine == null) {
            throw new IllegalArgumentException("Thông tin tạp chí không được để trống!");
        }
        boolean isNew = (magazine.getId() == null);
        Magazine saved = documentRepository.save(magazine);
        if (isNew) {
            activityLogService.log(ActionType.BOOK_CREATE, TargetType.DOCUMENT, saved.getId(), saved.getTitle(),
                    "Thêm mới tạp chí: " + saved.getTitle() + " (Số phát hành: " + saved.getIssueNumber() + ")");
        } else {
            activityLogService.log(ActionType.BOOK_UPDATE, TargetType.DOCUMENT, saved.getId(), saved.getTitle(),
                    "Cập nhật thông tin tạp chí: " + saved.getTitle());
        }
        return saved;
    }

    @Transactional
    public Document saveDocument(Document document) {
        if (document instanceof Book book) {
            return saveBook(book);
        } else if (document instanceof Magazine magazine) {
            return saveMagazine(magazine);
        }
        return documentRepository.save(document);
    }

    @Transactional
    public void updateStock(Long docId, int quantityChange) {
        Document doc = getDocumentById(docId);
        doc.adjustQuantity(quantityChange);
        documentRepository.save(doc);
        activityLogService.log(ActionType.STOCK_ADJUST, TargetType.DOCUMENT, doc.getId(), doc.getTitle(),
                "Điều chỉnh số lượng kho tài liệu '" + doc.getTitle() + "' thay đổi: " + (quantityChange >= 0 ? "+" : "") + quantityChange + " (Tồn: " + doc.getQuantity() + ")");
    }

    @Transactional
    public void setStockQuantity(Long docId, int newQuantity) {
        Document doc = getDocumentById(docId);
        doc.setStockQuantity(newQuantity);
        documentRepository.save(doc);
        activityLogService.log(ActionType.STOCK_ADJUST, TargetType.DOCUMENT, doc.getId(), doc.getTitle(),
                "Thiết lập số lượng tồn kho tài liệu '" + doc.getTitle() + "' thành " + newQuantity);
    }

    @Transactional
    public void updateShelfLocation(Long docId, String shelfLocation) {
        Document doc = getDocumentById(docId);
        doc.setShelfLocation(shelfLocation);
        documentRepository.save(doc);
        String desc = (doc.getShelfLocation() != null)
                ? "Cập nhật vị trí kệ tài liệu '" + doc.getTitle() + "' thành: " + doc.getShelfLocation()
                : "Tắt/xóa vị trí kệ tài liệu '" + doc.getTitle() + "'";
        activityLogService.log(ActionType.BOOK_UPDATE, TargetType.DOCUMENT, doc.getId(), doc.getTitle(), desc);
    }

    @Transactional
    public void deleteDocument(Long docId) {
        boolean isBorrowing = borrowRecordRepository.existsByBookIdAndStatus(docId, BorrowStatus.BORROWING);
        if (isBorrowing) {
            throw new IllegalStateException("Không thể xóa tài liệu đang có người mượn!");
        }
        Document doc = documentRepository.findById(docId).orElse(null);
        String title = doc != null ? doc.getTitle() : ("Tài liệu #" + docId);
        documentRepository.deleteById(docId);
        activityLogService.log(ActionType.BOOK_DELETE, TargetType.DOCUMENT, docId, title,
                "Xóa tài liệu khỏi hệ thống: " + title);
    }
}