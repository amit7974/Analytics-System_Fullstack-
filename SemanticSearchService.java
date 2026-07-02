package com.example.analytics.service;

import com.example.analytics.dto.SemanticSearchResult;
import com.example.analytics.model.SearchDocument;
import com.example.analytics.repository.SearchDocumentRepository;
import com.example.analytics.service.embedding.EmbeddingService;
import com.example.analytics.util.VectorUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SemanticSearchService {

    private static final int DEFAULT_TOP_K = 5;
    private static final int MAX_TOP_K = 50;

    private final SearchDocumentRepository documentRepository;
    private final EmbeddingService embeddingService;

    public SemanticSearchService(SearchDocumentRepository documentRepository,
                                  EmbeddingService embeddingService) {
        this.documentRepository = documentRepository;
        this.embeddingService = embeddingService;
    }

    @Transactional
    public SemanticSearchResult indexDocument(String content) {
        double[] embedding = embeddingService.embed(content);

        SearchDocument document = new SearchDocument();
        document.setContent(content);
        document.setEmbedding(VectorUtil.toJson(embedding));
        document.setEmbeddingProvider(embeddingService.providerName());

        SearchDocument saved = documentRepository.save(document);

        return SemanticSearchResult.builder()
                .documentId(saved.getId())
                .content(saved.getContent())
                .score(1.0)
                .build();
    }

    /**
     * Computes the query embedding once, then scores it against every stored document's
     * embedding via cosine similarity, returning the top K matches sorted descending by score.
     *
     * This is a brute-force O(n) scan, which is appropriate for demo/portfolio-scale corpora
     * (thousands of documents). For production-scale corpora, swap the storage/search layer
     * for a dedicated vector index (e.g. pgvector, OpenSearch k-NN, Milvus, Pinecone) behind
     * the same SemanticSearchService interface.
     */
    public List<SemanticSearchResult> search(String query, Integer topK) {
        int k = (topK == null || topK <= 0) ? DEFAULT_TOP_K : Math.min(topK, MAX_TOP_K);

        double[] queryVector = embeddingService.embed(query);
        String currentProvider = embeddingService.providerName();

        List<SearchDocument> documents = documentRepository.findAll();

        return documents.stream()
                .filter(doc -> currentProvider.equals(doc.getEmbeddingProvider()))
                .map(doc -> {
                    double[] docVector = VectorUtil.fromJson(doc.getEmbedding());
                    double score = VectorUtil.cosineSimilarity(queryVector, docVector);
                    return SemanticSearchResult.builder()
                            .documentId(doc.getId())
                            .content(doc.getContent())
                            .score(score)
                            .build();
                })
                .sorted(Comparator.comparingDouble(SemanticSearchResult::getScore).reversed())
                .limit(k)
                .collect(Collectors.toList());
    }
}
