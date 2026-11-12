package com.dj.chargewise.config;

import com.dj.chargewise.model.Role;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "chargewise")
public record ChargewiseProperties(Ingestion ingestion, Retrieval retrieval, Seed seed, Security security) {

    public record Ingestion(int chunkSize, int minChunkSizeChars) {
    }

    public record Retrieval(int topK, double similarityThreshold, int categoryContextTopK) {
    }

    public record Seed(boolean enabled) {
    }

    public record Security(String jwtSecret, Duration tokenTtl, List<User> users) {

        public record User(String username, String password, Role role) {
        }
    }
}
