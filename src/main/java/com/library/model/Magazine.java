package com.library.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "magazines")
public class Magazine extends Document {

    private int issueNumber;

    @Min(1)
    @Max(12)
    private int publishMonth;

    public Magazine(String title, String publisher, int publishYear, int quantity, String imageUrl,
                    int issueNumber, int publishMonth) {
        super(title, publisher, publishYear, quantity, DocumentType.MAGAZINE, imageUrl);
        this.issueNumber = issueNumber;
        this.publishMonth = publishMonth;
    }

    public Magazine(String title, String publisher, int publishYear, int quantity, String imageUrl,
                    int issueNumber, int publishMonth, String summary) {
        super(title, publisher, publishYear, quantity, DocumentType.MAGAZINE, imageUrl, summary);
        this.issueNumber = issueNumber;
        this.publishMonth = publishMonth;
    }

    @Override
    public String getDocumentDetails() {
        return String.format("Số phát hành: %d | Kỳ phát hành: Tháng %d/%d", issueNumber, publishMonth, getPublishYear());
    }

    @Override
    public String getIdentifierCode() {
        return String.format("MAG-%d-%02d", getPublishYear(), issueNumber);
    }
}