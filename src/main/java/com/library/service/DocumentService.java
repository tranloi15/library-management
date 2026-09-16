package com.library.service;

import com.library.model.Document;
import com.library.repository.BorrowRecordRepository;
import com.library.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final BorrowRecordRepository borrowRecordRepository;

    @Transactional
    public void updateStock(Long docId, int quantityChange) {
        Document doc = documentRepository.findById(docId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài liệu ID: " + docId));

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