package com.example.analytics.controller;

import com.example.analytics.dto.IndexDocumentRequest;
import com.example.analytics.dto.SemanticSearchRequest;
import com.example.analytics.dto.SemanticSearchResult;
import com.example.analytics.service.SemanticSearchService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SemanticSearchService semanticSearchService;

    public SearchController(SemanticSearchService semanticSearchService) {
        this.semanticSearchService = semanticSearchService;
    }

    @PostMapping("/index")
    public ResponseEntity<SemanticSearchResult> indexDocument(@Valid @RequestBody IndexDocumentRequest request) {
        SemanticSearchResult result = semanticSearchService.indexDocument(request.getContent());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/semantic")
    public List<SemanticSearchResult> search(@Valid @RequestBody SemanticSearchRequest request) {
        return semanticSearchService.search(request.getQuery(), request.getTopK());
    }
}
