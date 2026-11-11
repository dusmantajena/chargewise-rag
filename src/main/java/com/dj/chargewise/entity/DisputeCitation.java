package com.dj.chargewise.entity;

import com.dj.chargewise.model.RetrievedChunk;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class DisputeCitation {

    private static final int MAX_EXCERPT_LENGTH = 400;

    @Column(nullable = false)
    private String document;

    @Column(nullable = false)
    private String excerpt;

    protected DisputeCitation() {
    }

    public static DisputeCitation from(RetrievedChunk chunk) {
        DisputeCitation citation = new DisputeCitation();
        citation.document = chunk.filename();
        citation.excerpt = excerpt(chunk.content());
        return citation;
    }

    private static String excerpt(String content) {
        String text = content.replaceAll("\\s+", " ").strip();
        if (text.length() <= MAX_EXCERPT_LENGTH) {
            return text;
        }
        int cut = text.lastIndexOf(' ', MAX_EXCERPT_LENGTH);
        return text.substring(0, cut > 0 ? cut : MAX_EXCERPT_LENGTH) + "...";
    }

    public String getDocument() {
        return document;
    }

    public String getExcerpt() {
        return excerpt;
    }
}
