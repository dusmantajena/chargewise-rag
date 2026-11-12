package com.dj.chargewise.controller;

import com.dj.chargewise.dto.request.AnalyseDisputeRequest;
import com.dj.chargewise.dto.response.DisputeResponse;
import com.dj.chargewise.model.IdempotentResult;
import com.dj.chargewise.service.dispute.DisputeService;
import com.dj.chargewise.service.dispute.IdempotencyService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/disputes")
@Tag(name = "Disputes", description = "Analyse card disputes against the loaded rules")
@ApiResponse(responseCode = "401", description = "Missing, invalid or expired access token",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
@ApiResponse(responseCode = "403", description = "Requires the AGENT role",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
public class DisputeController {

    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    public static final String IDEMPOTENT_REPLAYED_HEADER = "Idempotent-Replayed";

    private final DisputeService disputeService;
    private final IdempotencyService idempotencyService;

    public DisputeController(DisputeService disputeService, IdempotencyService idempotencyService) {
        this.disputeService = disputeService;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping("/analyse")
    @Operation(summary = "Analyse a dispute",
            description = "Finds the matching rules, then returns the dispute category, the evidence the issuer "
                    + "must provide and citations to the rules used. Send an Idempotency-Key to make retries safe: "
                    + "repeating a key returns the stored result without a new analysis.")
    @ApiResponse(responseCode = "201", description = "Analysis created, or replayed for a repeated Idempotency-Key")
    @ApiResponse(responseCode = "400", description = "Invalid request or Idempotency-Key",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Idempotency-Key reused with a different request, "
            + "or the first request is still being processed",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "The rules don't support an answer for this dispute",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "429", description = "Rate limit exceeded; see Retry-After",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "503", description = "The AI service is unavailable or timed out; see Retry-After",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<DisputeResponse> analyse(
            @Parameter(description = "Unique key per dispute submission, for example a UUID",
                    example = "3f2b9c1e-7d4a-4c8e-9a51-0b6d2e8f4a17")
            @RequestHeader(name = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody AnalyseDisputeRequest request,
            Principal principal) {
        IdempotentResult result = idempotencyService.analyse(principal.getName(), idempotencyKey, request);

        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/disputes/{id}")
                .buildAndExpand(result.response().id())
                .toUri();

        ResponseEntity.BodyBuilder response = ResponseEntity.created(location);
        if (result.replayed()) {
            response.header(IDEMPOTENT_REPLAYED_HEADER, "true");
        }
        return response.body(result.response());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a stored analysis result")
    @ApiResponse(responseCode = "200", description = "Analysis result")
    @ApiResponse(responseCode = "404", description = "Analysis not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public DisputeResponse get(@PathVariable UUID id) {
        return disputeService.get(id);
    }
}
