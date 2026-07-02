package com.example.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class SemanticSearchResult {
    private Long documentId;
    private String content;
    private double score;
}
