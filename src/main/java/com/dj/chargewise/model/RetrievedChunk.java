package com.dj.chargewise.model;

import com.dj.chargewise.util.MetadataKeys;
import org.springframework.ai.document.Document;

import java.util.Map;

public record RetrievedChunk(
        String id,
        String documentId,
        String filename,
        ReasonCategory category,
        String content,
        double score) {

    public static RetrievedChunk fromDocument(Document document) {
        Map<String, Object> metadata = document.getMetadata();
        return new RetrievedChunk(
                document.getId(),
                String.valueOf(metadata.get(MetadataKeys.DOCUMENT_ID)),
                String.valueOf(metadata.get(MetadataKeys.FILENAME)),
                ReasonCategory.valueOf(String.valueOf(metadata.get(MetadataKeys.REASON_CATEGORY))),
                document.getText(),
                document.getScore() == null ? 0.0 : document.getScore());
    }
}
