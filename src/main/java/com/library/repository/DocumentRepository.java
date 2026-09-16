package com.library.repository;

import com.library.model.Document;
import com.library.model.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    @Query("SELECT d FROM Document d WHERE " +
           "(:keyword IS NULL OR LOWER(d.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           " OR LOWER(d.publisher) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:docType IS NULL OR d.documentType = :docType) " +
           "AND (:onlyInStock = false OR d.quantity > 0)")
    List<Document> searchDocuments(@Param("keyword") String keyword,
                                  @Param("docType") DocumentType docType,
                                  @Param("onlyInStock") boolean onlyInStock);
}