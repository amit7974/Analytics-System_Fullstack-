package com.example.analytics.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "search_document")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SearchDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    /**
     * Embedding vector serialized as a JSON array of doubles, e.g. "[0.123, -0.045, ...]".
     * MySQL has no first-class vector column in widely-deployed versions, so we store it as
     * JSON/TEXT and compute similarity in the application layer.
     */
    @Lob
    @Column(name = "embedding", nullable = false)
    private String embedding;

    @Column(name = "embedding_provider", length = 50)
    private String embeddingProvider;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
