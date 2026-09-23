package com.library.model;

import com.library.enums.DocumentType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "magazines")
public class Magazine extends Document {
    private int issueNumber;
    private int publishMonth;

    public Magazine() {
        this.setDocumentType(DocumentType.MAGAZINE);
    }

    @Override
    public String getDocumentDetails() {
        return String.format("Số phát hành: %d | Tháng: %d/%d", issueNumber, publishMonth, getPublishYear());
    }

    @Override
    public String getIdentifierCode() {
        return "ISSUE-" + this.issueNumber;
    }
}