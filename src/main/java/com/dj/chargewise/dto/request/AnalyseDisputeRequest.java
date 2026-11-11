package com.dj.chargewise.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnalyseDisputeRequest(
        @NotBlank @Size(min = 10, max = 4000) String description) {
}
