package com.dj.chargewise.dto.response;

import com.dj.chargewise.entity.DisputeCitation;

public record CitationResponse(String document, String excerpt) {

    public static CitationResponse from(DisputeCitation citation) {
        return new CitationResponse(citation.getDocument(), citation.getExcerpt());
    }
}
