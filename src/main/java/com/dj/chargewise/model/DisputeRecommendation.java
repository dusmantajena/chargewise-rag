package com.dj.chargewise.model;

import java.util.List;

public record DisputeRecommendation(
        boolean supported,
        ReasonCategory category,
        List<String> requiredEvidence,
        String explanation,
        List<String> sources) {

    public List<String> requiredEvidenceOrEmpty() {
        return requiredEvidence == null ? List.of() : requiredEvidence;
    }

    public List<String> sourcesOrEmpty() {
        return sources == null ? List.of() : sources;
    }
}
