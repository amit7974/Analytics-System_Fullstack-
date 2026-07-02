package com.example.analytics.service.embedding;

import com.example.analytics.util.VectorUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Default embedding provider. Requires no external API and no API key, so the whole
 * project builds, runs and returns real (if less semantically rich) similarity results
 * out of the box.
 *
 * It implements the "feature hashing" trick: each token in the text is hashed into one
 * of {@code dimension} buckets, the bucket is incremented (with a sign derived from the
 * hash to reduce collisions cancelling each other out), and the resulting vector is
 * L2-normalized. Cosine similarity between two such vectors approximates token-overlap
 * similarity between the two texts, which is a legitimate, well-known baseline technique
 * (used by e.g. Vowpal Wabbit) for turning text into fixed-size vectors without a model.
 *
 * Swap in {@link OpenAiEmbeddingService} (embedding.provider=openai) for true semantic
 * embeddings once an API key is available.
 */
@Service
@ConditionalOnProperty(name = "embedding.provider", havingValue = "local", matchIfMissing = true)
public class LocalHashingEmbeddingService implements EmbeddingService {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("[a-zA-Z0-9]+");

    private final int dimension;

    public LocalHashingEmbeddingService(@Value("${embedding.dimension:384}") int dimension) {
        this.dimension = dimension;
    }

    @Override
    public double[] embed(String text) {
        double[] vector = new double[dimension];
        if (text == null || text.isBlank()) {
            return vector;
        }

        var matcher = TOKEN_PATTERN.matcher(text.toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            String token = matcher.group();
            int hash = stableHash(token);
            int bucket = Math.floorMod(hash, dimension);
            double sign = (hash & 1) == 0 ? 1.0 : -1.0;
            vector[bucket] += sign;
        }

        return VectorUtil.normalize(vector);
    }

    @Override
    public String providerName() {
        return "local-hashing-v1";
    }

    private int stableHash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return ((bytes[0] & 0xFF) << 24) | ((bytes[1] & 0xFF) << 16)
                    | ((bytes[2] & 0xFF) << 8) | (bytes[3] & 0xFF);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed to be available on every standard JVM.
            throw new IllegalStateException(e);
        }
    }
}
