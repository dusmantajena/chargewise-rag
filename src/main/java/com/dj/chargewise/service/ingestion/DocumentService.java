package com.dj.chargewise.service.ingestion;

import com.dj.chargewise.dto.request.DocumentUploadRequest;
import com.dj.chargewise.dto.response.DocumentResponse;
import com.dj.chargewise.dto.response.PageResponse;
import com.dj.chargewise.entity.KnowledgeDocument;
import com.dj.chargewise.exception.DocumentStateException;
import com.dj.chargewise.exception.InvalidDocumentException;
import com.dj.chargewise.exception.ResourceNotFoundException;
import com.dj.chargewise.model.ReasonCategory;
import com.dj.chargewise.repository.KnowledgeDocumentRepository;
import com.dj.chargewise.repository.VectorChunkRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class DocumentService {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("pdf", "docx", "txt");

    private final KnowledgeDocumentRepository documentRepository;
    private final VectorChunkRepository vectorChunkRepository;
    private final IngestionPipeline ingestionPipeline;

    public DocumentService(KnowledgeDocumentRepository documentRepository,
                           VectorChunkRepository vectorChunkRepository,
                           IngestionPipeline ingestionPipeline) {
        this.documentRepository = documentRepository;
        this.vectorChunkRepository = vectorChunkRepository;
        this.ingestionPipeline = ingestionPipeline;
    }

    public DocumentResponse upload(DocumentUploadRequest request) {
        MultipartFile file = request.file();
        String filename = validateFile(file);
        return register(filename, file.getContentType(), readBytes(file), request.reasonCategory());
    }

    public DocumentResponse register(String filename, String contentType, byte[] content,
                                     ReasonCategory reasonCategory) {
        KnowledgeDocument document = documentRepository.save(
                new KnowledgeDocument(filename, contentType, content.length, reasonCategory));
        ingestionPipeline.ingest(document.getId(), content);
        return DocumentResponse.from(document);
    }

    public PageResponse<DocumentResponse> list(Pageable pageable) {
        return PageResponse.from(documentRepository.findAll(pageable).map(DocumentResponse::from));
    }

    @Transactional
    public void delete(UUID id) {
        KnowledgeDocument document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", id));
        if (document.isPending()) {
            throw new DocumentStateException("Document '%s' is still being ingested".formatted(id));
        }

        vectorChunkRepository.deleteByDocumentId(id);
        documentRepository.delete(document);
    }

    public boolean isEmpty() {
        return documentRepository.count() == 0;
    }

    private String validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidDocumentException("Uploaded file is empty");
        }

        String filename = StringUtils.cleanPath(String.valueOf(file.getOriginalFilename()));
        String extension = StringUtils.getFilenameExtension(filename);
        if (extension == null || !SUPPORTED_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT))) {
            throw new InvalidDocumentException("Unsupported file type. Supported types: " + SUPPORTED_EXTENSIONS);
        }
        return filename;
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new InvalidDocumentException("Uploaded file could not be read");
        }
    }
}
