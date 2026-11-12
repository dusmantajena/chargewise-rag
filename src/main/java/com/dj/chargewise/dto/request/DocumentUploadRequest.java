package com.dj.chargewise.dto.request;

import com.dj.chargewise.model.ReasonCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public record DocumentUploadRequest(
        @Schema(description = "Rules document: PDF, DOCX or TXT, up to 10MB", type = "string", format = "binary")
        @NotNull(message = "File is required")
        MultipartFile file,

        @Schema(description = "Dispute category the rules belong to", example = "GOODS_NOT_RECEIVED")
        @NotNull(message = "Reason category is required")
        ReasonCategory reasonCategory) {
}
