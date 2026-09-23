package com.library.model;

import com.library.enums.DocumentType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "documents")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String publisher;

    private int publishYear;
    private int quantity;

    @Enumerated(EnumType.STRING)
    private DocumentType documentType;

    private String imageUrl;

    // Đóng gói kiểm tra sách còn trong kho
    public boolean isAvailable() {
        return this.quantity > 0;
    }

    // 2 hàm trừu tượng (Đa hình OOP)
    public abstract String getDocumentDetails();
    public abstract String getIdentifierCode();
}