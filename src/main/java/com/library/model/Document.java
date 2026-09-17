package com.library.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "documents")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Document extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String publisher;
    private int publishYear;
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false)
    private DocumentType documentType;

    private String imageUrl;

    public Document(String title, String publisher, int publishYear, int quantity, DocumentType documentType, String imageUrl) {
        this.title = title;
        this.publisher = publisher;
        this.publishYear = publishYear;
        this.quantity = quantity;
        this.documentType = documentType;
        this.imageUrl = imageUrl;
    }

    public abstract String getDocumentDetails();
    public abstract String getIdentifierCode();

    public boolean isAvailable() {
        return this.quantity > 0;
    }
}