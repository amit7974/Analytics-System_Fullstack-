package com.example.analytics.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public final class VectorUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private VectorUtil() {
    }

    public static String toJson(double[] vector) {
        try {
            List<Double> boxed = new java.util.ArrayList<>(vector.length);
            for (double v : vector) {
                boxed.add(v);
            }
            return MAPPER.writeValueAsString(boxed);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize embedding vector", e);
        }
    }

    public static double[] fromJson(String json) {
        try {
            List<Double> values = MAPPER.readValue(json, new TypeReference<List<Double>>() {
            });
            return values.stream().mapToDouble(Double::doubleValue).toArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize embedding vector", e);
        }
    }

    /**
     * Cosine similarity between two vectors. Returns 0 if either vector has zero magnitude
     * or if the dimensions don't match.
     */
    public static double cosineSimilarity(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length || a.length == 0) {
            return 0.0;
        }
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public static double[] normalize(double[] v) {
        double norm = 0.0;
        for (double value : v) {
            norm += value * value;
        }
        norm = Math.sqrt(norm);
        if (norm == 0.0) {
            return v;
        }
        double finalNorm = norm;
        return java.util.Arrays.stream(v).map(x -> x / finalNorm).toArray();
    }
}
