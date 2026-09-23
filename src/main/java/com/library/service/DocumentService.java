package com.library.service;

import com.library.dto.BookFormDto;
import com.library.dto.MagazineFormDto;
import com.library.enums.DocumentType;
import com.library.model.Book;
import com.library.model.Document;
import com.library.model.Magazine;
import java.util.List;

public interface DocumentService {
    List<Document> getAllDocuments();
    List<Document> searchDocuments(String keyword, DocumentType docType, boolean onlyInStock);
    Document getDocumentById(Long id);
    Book saveBook(BookFormDto dto);
    Magazine saveMagazine(MagazineFormDto dto);
    void updateBook(Long id, BookFormDto dto);
    void deleteDocument(Long id);
    void updateStock(Long docId, int quantityChange);
}