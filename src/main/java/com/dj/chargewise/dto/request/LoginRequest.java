package com.dj.chargewise.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(example = "agent")
        @NotBlank(message = "Username is required")
        String username,

        @Schema(example = "agent-change-me")
        @NotBlank(message = "Password is required")
        String password) {
}
