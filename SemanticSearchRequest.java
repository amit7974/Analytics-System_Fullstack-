package com.example.analytics.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SemanticSearchRequest {

    @NotBlank(message = "query is required")
    private String query;

    /**
     * Number of top results to return. Defaults to 5 if not provided or invalid.
     */
    private Integer topK;
}
