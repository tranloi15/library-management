package com.library.service.impl;

import com.library.dto.BookFormDto;
import com.library.dto.MagazineFormDto;
import com.library.enums.DocumentType;
import com.library.model.Book;
import com.library.model.Document;
import com.library.model.Magazine;
import com.library.repository.BookRepository;
import com.library.repository.DocumentRepository;
import com.library.repository.MagazineRepository;
import com.library.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class DocumentServiceImpl implements DocumentService {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private MagazineRepository magazineRepository;

    @Override
    public List<Document> getAllDocuments() {
        return documentRepository.findAll();
    }

    @Override
    public List<Document> searchDocuments(String keyword, DocumentType docType, boolean onlyInStock) {
        return documentRepository.searchDocuments(keyword, docType, onlyInStock);
    }

    @Override
    public Document getDocumentById(Long id) {
        return documentRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài liệu có ID: " + id));
    }

    @Override
    @Transactional
    public Book saveBook(BookFormDto dto) {
        if (bookRepository.existsByIsbn(dto.getIsbn())) {
            throw new IllegalArgumentException("Mã ISBN đã tồn tại trên hệ thống!");
        }
        Book book = new Book();
        mapBookData(book, dto);
        return bookRepository.save(book);
    }

    @Override
    @Transactional
    public void updateBook(Long id, BookFormDto dto) {
        Document doc = getDocumentById(id);
        if (!(doc instanceof Book)) {
            throw new IllegalArgumentException("Tài liệu không phải là sách!");
        }
        Book book = (Book) doc;
        mapBookData(book, dto);
        bookRepository.save(book);
    }

    @Override
    @Transactional
    public Magazine saveMagazine(MagazineFormDto dto) {
        Magazine magazine = new Magazine();
        magazine.setTitle(dto.getTitle());
        magazine.setPublisher(dto.getPublisher());
        magazine.setIssueNumber(dto.getIssueNumber());
        magazine.setPublishMonth(dto.getPublishMonth());
        magazine.setPublishYear(dto.getPublishYear());
        magazine.setQuantity(dto.getQuantity());
        magazine.setImageUrl(dto.getImageUrl());
        return magazineRepository.save(magazine);
    }

    @Override
    @Transactional
    public void deleteDocument(Long id) {
        documentRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void updateStock(Long docId, int quantityChange) {
        Document doc = getDocumentById(docId);
        int newQuantity = doc.getQuantity() + quantityChange;
        if (newQuantity < 0) {
            throw new IllegalStateException("Số lượng sách trong kho không đủ để xuất!");
        }
        doc.setQuantity(newQuantity);
        documentRepository.save(doc);
    }

    private void mapBookData(Book book, BookFormDto dto) {
        book.setTitle(dto.getTitle());
        book.setAuthor(dto.getAuthor());
        book.setPublisher(dto.getPublisher());
        book.setIsbn(dto.getIsbn());
        book.setQuantity(dto.getQuantity());
        book.setPageCount(dto.getPageCount());
        book.setPublishYear(dto.getPublishYear());
        book.setGenre(dto.getGenre());
        book.setImageUrl(dto.getImageUrl());
    }
}