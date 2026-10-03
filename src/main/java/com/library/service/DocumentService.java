package com.library.service;

import com.library.model.Book;
import com.library.model.BorrowStatus;
import com.library.model.Document;
import com.library.model.Magazine;
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
                .filter(d -> {
                    if (keyword == null || keyword.trim().isEmpty())
                        return true;
                    String k = keyword.trim().toLowerCase();
                    boolean matchTitle = d.getTitle() != null && d.getTitle().toLowerCase().contains(k);
                    boolean matchPublisher = d.getPublisher() != null && d.getPublisher().toLowerCase().contains(k);
                    boolean matchCode = d.getIdentifierCode() != null
                            && d.getIdentifierCode().toLowerCase().contains(k);
                    boolean matchDetails = d.getDocumentDetails() != null
                            && d.getDocumentDetails().toLowerCase().contains(k);
                    boolean matchId = String.valueOf(d.getId()).equals(k) || ("#doc-" + d.getId()).equalsIgnoreCase(k);
                    return matchTitle || matchPublisher || matchCode || matchDetails || matchId;
                })
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
        if (book.getIsbn() != null && book.getId() == null && bookRepository.existsByIsbn(book.getIsbn())) {
            throw new IllegalArgumentException("Mã ISBN '" + book.getIsbn() + "' đã tồn tại trong hệ thống!");
        }
        return documentRepository.save(book);
    }

    @Transactional
    public Magazine saveMagazine(Magazine magazine) {
        if (magazine == null) {
            throw new IllegalArgumentException("Thông tin tạp chí không được để trống!");
        }
        return documentRepository.save(magazine);
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
    }

    @Transactional
    public void setStockQuantity(Long docId, int newQuantity) {
        Document doc = getDocumentById(docId);
        doc.setStockQuantity(newQuantity);
        documentRepository.save(doc);
    }

    @Transactional
    public void deleteDocument(Long docId) {
        boolean isBorrowing = borrowRecordRepository.existsByBookIdAndStatus(docId, BorrowStatus.BORROWING);
        if (isBorrowing) {
            throw new IllegalStateException("Không thể xóa tài liệu đang có người mượn!");
        }
        documentRepository.deleteById(docId);
    }
}