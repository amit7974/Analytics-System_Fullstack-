package com.example.analytics.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IndexDocumentRequest {

    @NotBlank(message = "content is required")
    private String content;
}
