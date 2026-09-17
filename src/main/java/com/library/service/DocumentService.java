package com.library.service;

import com.library.model.Document;
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
    private final BorrowRecordRepository borrowRecordRepository;

    public List<Document> getAllDocuments() {
        return documentRepository.findAll();
    }

    public List<Document> searchDocuments(String keyword) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            return documentRepository.searchDocuments(keyword.trim(), null, false);
        }
        return getAllDocuments();
    }

    public Document getDocumentById(Long id) {
        return documentRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài liệu ID: " + id));
    }

    @Transactional
    public void updateStock(Long docId, int quantityChange) {
        Document doc = getDocumentById(docId);

        int newQuantity = doc.getQuantity() + quantityChange;
        if (newQuantity < 0) {
            throw new IllegalStateException("Số lượng sách trong kho không đủ!");
        }
        doc.setQuantity(newQuantity);
        documentRepository.save(doc);
    }

    @Transactional
    public void deleteDocument(Long docId) {
        boolean isBorrowing = borrowRecordRepository.existsByBookIdAndStatus(docId, "BORROWING");
        if (isBorrowing) {
            throw new IllegalStateException("Không thể xóa sách đang có người mượn!");
        }
        documentRepository.deleteById(docId);
    }
}