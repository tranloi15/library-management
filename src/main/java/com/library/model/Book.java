package com.library.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "books")
public class Book extends Document {

    private String author;

    @Column(unique = true, nullable = false)
    private String isbn;

    private int pageCount;
    private String genre;

    public Book(String title, String publisher, int publishYear, int quantity, String imageUrl,
                String author, String isbn, int pageCount, String genre) {
        super(title, publisher, publishYear, quantity, DocumentType.BOOK, imageUrl);
        this.author = author;
        this.isbn = isbn;
        this.pageCount = pageCount;
        this.genre = genre;
    }

    @Override
    public String getDocumentDetails() {
        return String.format("Tác giả: %s | Số trang: %d | Thể loại: %s", author, pageCount, genre);
    }

    @Override
    public String getIdentifierCode() {
        return this.isbn;
    }
}