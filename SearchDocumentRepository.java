package com.example.analytics.repository;

import com.example.analytics.model.SearchDocument;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SearchDocumentRepository extends JpaRepository<SearchDocument, Long> {
}
