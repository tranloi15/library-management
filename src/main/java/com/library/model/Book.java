package com.library.model;

import com.library.enums.DocumentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "books")
public class Book extends Document {
    @Column(nullable = false)
    private String author;

    @Column(unique = true, nullable = false)
    private String isbn;

    private int pageCount;
    private String genre;

    public Book() {
        this.setDocumentType(DocumentType.BOOK);
    }

    @Override
    public String getDocumentDetails() {
        return String.format("Tác giả: %s | ISBN: %s | Số trang: %d", author, isbn, pageCount);
    }

    @Override
    public String getIdentifierCode() {
        return this.isbn;
    }
}