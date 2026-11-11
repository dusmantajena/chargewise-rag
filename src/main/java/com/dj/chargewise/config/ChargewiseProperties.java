package com.dj.chargewise.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "chargewise")
public record ChargewiseProperties(Ingestion ingestion, Retrieval retrieval, Seed seed) {

    public record Ingestion(int chunkSize, int minChunkSizeChars) {
    }

    public record Retrieval(int topK, double similarityThreshold, int categoryContextTopK) {
    }

    public record Seed(boolean enabled) {
    }
}