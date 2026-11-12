package com.dj.chargewise.controller;

import com.dj.chargewise.dto.request.DocumentUploadRequest;
import com.dj.chargewise.dto.response.DocumentResponse;
import com.dj.chargewise.dto.response.PageResponse;
import com.dj.chargewise.service.ingestion.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@Tag(name = "Documents", description = "Load and manage scheme rule documents")
@ApiResponse(responseCode = "401", description = "Missing, invalid or expired access token",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
@ApiResponse(responseCode = "403", description = "Requires the ADMIN role",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Upload a rules document",
            description = "Accepts a PDF, DOCX or TXT file. Parsing, chunking and embedding run in the background; "
                    + "poll the list endpoint until the status is COMPLETED.")
    @ApiResponse(responseCode = "202", description = "Document accepted for ingestion")
    @ApiResponse(responseCode = "400", description = "Missing file, unsupported file type or invalid category",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "413", description = "File is larger than 10MB",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public DocumentResponse upload(@Valid @ModelAttribute DocumentUploadRequest request) {
        return documentService.upload(request);
    }

    @GetMapping
    @Operation(summary = "List documents with their ingestion status")
    @ApiResponse(responseCode = "200", description = "Page of documents")
    public PageResponse<DocumentResponse> list(
            @ParameterObject @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return documentService.list(pageable);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a document and all its chunk embeddings")
    @ApiResponse(responseCode = "204", description = "Document and vectors deleted")
    @ApiResponse(responseCode = "404", description = "Document not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Document is still being ingested",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public void delete(@PathVariable UUID id) {
        documentService.delete(id);
    }
}
