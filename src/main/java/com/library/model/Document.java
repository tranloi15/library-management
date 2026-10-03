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

    @Column(length = 1000)
    private String summary;

    public Document(String title, String publisher, int publishYear, int quantity, DocumentType documentType, String imageUrl) {
        this.title = title;
        this.publisher = publisher;
        this.publishYear = publishYear;
        this.quantity = quantity;
        this.documentType = documentType;
        this.imageUrl = imageUrl;
    }

    public Document(String title, String publisher, int publishYear, int quantity, DocumentType documentType, String imageUrl, String summary) {
        this(title, publisher, publishYear, quantity, documentType, imageUrl);
        this.summary = summary;
    }

    public abstract String getDocumentDetails();
    public abstract String getIdentifierCode();

    public String getSummary() {
        if (summary != null && !summary.trim().isEmpty()) {
            return summary;
        }
        if (documentType == DocumentType.BOOK) {
            return "Tài liệu học thuật và giáo trình tiêu biểu được chọn lọc phục vụ công tác tra cứu, học tập và nghiên cứu chuyên sâu.";
        }
        return "Ấn phẩm định kỳ chuyên ngành cung cấp các báo cáo khoa học, phân tích chuyên môn và thông tin cập nhật mới nhất.";
    }

    public boolean isAvailable() {
        return this.quantity > 0;
    }

    public void adjustQuantity(int delta) {
        if (this.quantity + delta < 0) {
            throw new IllegalStateException("Số lượng sách trong kho không đủ để thực hiện thao tác!");
        }
        this.quantity += delta;
    }

    public void setStockQuantity(int newQuantity) {
        if (newQuantity < 0) {
            throw new IllegalArgumentException("Số lượng tồn kho không được nhỏ hơn 0!");
        }
        this.quantity = newQuantity;
    }
}