package com.dj.chargewise.controller;

import com.dj.chargewise.dto.request.AnalyseDisputeRequest;
import com.dj.chargewise.dto.response.DisputeResponse;
import com.dj.chargewise.service.dispute.DisputeService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/disputes")
@Tag(name = "Disputes", description = "Analyse card disputes against the loaded rules")
@ApiResponse(responseCode = "401", description = "Missing, invalid or expired access token",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
@ApiResponse(responseCode = "403", description = "Requires the AGENT role",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
public class DisputeController {

    private final DisputeService disputeService;

    public DisputeController(DisputeService disputeService) {
        this.disputeService = disputeService;
    }

    @PostMapping("/analyse")
    @Operation(summary = "Analyse a dispute",
            description = "Finds the matching rules, then returns the dispute category, the evidence the issuer "
                    + "must provide and citations to the rules used.")
    @ApiResponse(responseCode = "201", description = "Analysis created and stored")
    @ApiResponse(responseCode = "400", description = "Invalid request",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "The rules don't support an answer for this dispute",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "503", description = "The AI service is unavailable or timed out; see Retry-After",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<DisputeResponse> analyse(@Valid @RequestBody AnalyseDisputeRequest request) {
        DisputeResponse response = disputeService.analyse(request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/disputes/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
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
