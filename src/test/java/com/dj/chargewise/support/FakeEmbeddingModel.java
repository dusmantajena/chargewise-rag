package com.dj.chargewise.support;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FakeEmbeddingModel implements EmbeddingModel {

    public static final String OFF_TOPIC_MARKER = "bank branch";

    private static final int DIMENSIONS = 1536;
    private static final int RULE_AXIS = 0;
    private static final int OFF_TOPIC_AXIS = 1;

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<String> inputs = request.getInstructions();
        List<Embedding> embeddings = new ArrayList<>(inputs.size());
        for (int i = 0; i < inputs.size(); i++) {
            embeddings.add(new Embedding(vectorFor(inputs.get(i)), i));
        }
        return new EmbeddingResponse(embeddings);
    }

    public float[] embed(Document document) {
        return vectorFor(document.getText());
    }

    public int dimensions() {
        return DIMENSIONS;
    }

    private static float[] vectorFor(String text) {
        float[] vector = new float[DIMENSIONS];
        boolean offTopic = text != null && text.toLowerCase(Locale.ROOT).contains(OFF_TOPIC_MARKER);
        vector[offTopic ? OFF_TOPIC_AXIS : RULE_AXIS] = 1f;
        return vector;
    }
}
