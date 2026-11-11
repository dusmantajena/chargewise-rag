package com.dj.chargewise.dto.response;

import com.dj.chargewise.entity.KnowledgeDocument;
import com.dj.chargewise.model.IngestionStatus;
import com.dj.chargewise.model.ReasonCategory;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        String filename,
        String contentType,
        long sizeBytes,
        ReasonCategory reasonCategory,
        IngestionStatus status,
        int chunkCount,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt) {

    public static DocumentResponse from(KnowledgeDocument document) {
        return new DocumentResponse(
                document.getId(),
                document.getFilename(),
                document.getContentType(),
                document.getSizeBytes(),
                document.getReasonCategory(),
                document.getStatus(),
                document.getChunkCount(),
                document.getErrorMessage(),
                document.getCreatedAt(),
                document.getUpdatedAt());
    }
}
