package com.dj.chargewise.dto.request;

import com.dj.chargewise.model.ReasonCategory;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public record DocumentUploadRequest(
        @NotNull MultipartFile file,
        @NotNull ReasonCategory reasonCategory) {
}
