package com.dj.chargewise.service.dispute;

import com.dj.chargewise.config.ChargewiseProperties;
import com.dj.chargewise.model.ReasonCategory;
import com.dj.chargewise.model.RetrievedChunk;
import com.dj.chargewise.util.MetadataKeys;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Component
public class RuleRetriever {

    private final VectorStore vectorStore;
    private final ChargewiseProperties.Retrieval properties;

    public RuleRetriever(VectorStore vectorStore, ChargewiseProperties properties) {
        this.vectorStore = vectorStore;
        this.properties = properties.retrieval();
    }

    public List<RetrievedChunk> retrieve(String query) {
        List<RetrievedChunk> hits = search(query, properties.topK(), properties.similarityThreshold(), null);
        if (hits.isEmpty()) {
            return hits;
        }

        ReasonCategory bestCategory = hits.getFirst().category();
        FilterExpressionBuilder filter = new FilterExpressionBuilder();
        List<RetrievedChunk> categoryChunks = search(query, properties.categoryContextTopK(), 0.0,
                filter.eq(MetadataKeys.REASON_CATEGORY, bestCategory.name()).build());

        Map<String, RetrievedChunk> merged = new LinkedHashMap<>();
        Stream.concat(hits.stream(), categoryChunks.stream())
                .forEach(chunk -> merged.putIfAbsent(chunk.id(), chunk));
        return List.copyOf(merged.values());
    }

    private List<RetrievedChunk> search(String query, int topK, double threshold, Filter.Expression filter) {
        SearchRequest.Builder request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(threshold);
        if (filter != null) {
            request.filterExpression(filter);
        }
        return vectorStore.similaritySearch(request.build()).stream()
                .map(RetrievedChunk::fromDocument)
                .toList();
    }
}