package com.dj.chargewise.dto.response;

import com.dj.chargewise.entity.Dispute;
import com.dj.chargewise.model.ReasonCategory;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DisputeResponse(
        UUID id,
        String description,
        ReasonCategory category,
        List<String> requiredEvidence,
        String explanation,
        List<CitationResponse> citations,
        Instant createdAt) {

    public static DisputeResponse from(Dispute dispute) {
        return new DisputeResponse(
                dispute.getId(),
                dispute.getDescription(),
                dispute.getCategory(),
                dispute.getRequiredEvidence(),
                dispute.getExplanation(),
                dispute.getCitations().stream().map(CitationResponse::from).toList(),
                dispute.getCreatedAt());
    }
}
