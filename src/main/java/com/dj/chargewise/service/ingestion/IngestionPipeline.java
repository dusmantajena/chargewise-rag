package com.dj.chargewise.service.ingestion;

import com.dj.chargewise.entity.KnowledgeDocument;
import com.dj.chargewise.exception.IngestionFailedException;
import com.dj.chargewise.exception.ResourceNotFoundException;
import com.dj.chargewise.repository.KnowledgeDocumentRepository;
import com.dj.chargewise.util.MetadataKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class IngestionPipeline {

    private static final Logger log = LoggerFactory.getLogger(IngestionPipeline.class);

    private final KnowledgeDocumentRepository documentRepository;
    private final DocumentTextExtractor textExtractor;
    private final TokenTextSplitter splitter;
    private final VectorStore vectorStore;

    public IngestionPipeline(KnowledgeDocumentRepository documentRepository, DocumentTextExtractor textExtractor,
                             TokenTextSplitter splitter, VectorStore vectorStore) {
        this.documentRepository = documentRepository;
        this.textExtractor = textExtractor;
        this.splitter = splitter;
        this.vectorStore = vectorStore;
    }

    @Async
    public void ingest(UUID documentId, byte[] content) {
        KnowledgeDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", documentId));

        try {
            String text = textExtractor.extract(content, document.getFilename());
            if (text.isBlank()) {
                throw new IngestionFailedException("No text could be extracted from " + document.getFilename());
            }

            List<Document> chunks = splitter.apply(List.of(Document.builder()
                    .text(text)
                    .metadata(Map.of(
                            MetadataKeys.DOCUMENT_ID, document.getId().toString(),
                            MetadataKeys.REASON_CATEGORY, document.getReasonCategory().name(),
                            MetadataKeys.FILENAME, document.getFilename()))
                    .build()));

            vectorStore.add(chunks);
            document.markCompleted(chunks.size());
            log.info("Ingested {} into {} chunks", document.getFilename(), chunks.size());
        } catch (Exception ex) {
            log.error("Ingestion failed for {}", document.getFilename(), ex);
            document.markFailed(ex.getMessage());
        }

        documentRepository.save(document);
    }
}
