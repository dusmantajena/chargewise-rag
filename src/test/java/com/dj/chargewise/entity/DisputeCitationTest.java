package com.dj.chargewise.entity;

import com.dj.chargewise.model.ReasonCategory;
import com.dj.chargewise.model.RetrievedChunk;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DisputeCitationTest {

    @Test
    void collapsesWhitespaceInExcerpt() {
        DisputeCitation citation = DisputeCitation.from(chunk("Section G20.2\n\n- The cardholder   contacted the merchant."));

        assertThat(citation.getDocument()).isEqualTo("goods-not-received.txt");
        assertThat(citation.getExcerpt()).isEqualTo("Section G20.2 - The cardholder contacted the merchant.");
    }

    @Test
    void truncatesLongExcerptAtWordBoundary() {
        DisputeCitation citation = DisputeCitation.from(chunk("evidence ".repeat(100)));

        assertThat(citation.getExcerpt()).endsWith("...").hasSizeLessThanOrEqualTo(403);
        assertThat(citation.getExcerpt()).doesNotContain("eviden...");
    }

    private static RetrievedChunk chunk(String content) {
        return new RetrievedChunk("chunk-1", "doc-1", "goods-not-received.txt",
                ReasonCategory.GOODS_NOT_RECEIVED, content, 0.9);
    }
}
