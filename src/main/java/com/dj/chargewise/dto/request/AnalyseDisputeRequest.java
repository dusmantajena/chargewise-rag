package com.dj.chargewise.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnalyseDisputeRequest(
        @Schema(description = "What the cardholder is disputing",
                example = "Customer says the parcel never arrived. The delivery date was three weeks ago "
                        + "and the merchant has not replied to two emails.")
        @NotBlank(message = "Description is required")
        @Size(min = 10, max = 4000, message = "Description must be between 10 and 4000 characters")
        String description) {
}
