package com.example.analytics.service.embedding;

public interface EmbeddingService {

    /**
     * Converts the given text into a fixed-dimension embedding vector.
     */
    double[] embed(String text);

    /**
     * Identifier for the provider that produced the embedding, stored alongside the
     * document so results are never compared across incompatible vector spaces.
     */
    String providerName();
}
