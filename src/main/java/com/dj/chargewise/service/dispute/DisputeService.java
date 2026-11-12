package com.dj.chargewise.service.dispute;

import com.dj.chargewise.dto.request.AnalyseDisputeRequest;
import com.dj.chargewise.dto.response.DisputeResponse;
import com.dj.chargewise.entity.Dispute;
import com.dj.chargewise.entity.DisputeCitation;
import com.dj.chargewise.exception.InsufficientContextException;
import com.dj.chargewise.exception.ResourceNotFoundException;
import com.dj.chargewise.model.DisputeRecommendation;
import com.dj.chargewise.model.RetrievedChunk;
import com.dj.chargewise.repository.DisputeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class DisputeService {

    private static final Logger log = LoggerFactory.getLogger(DisputeService.class);

    private static final String NO_MATCHING_RULES =
            "No rules match this dispute closely enough to make a recommendation.";
    private static final String MODEL_DECLINED =
            "The matching rules do not support a category for this dispute.";
    private static final String NO_CITATIONS =
            "The recommendation did not cite any of the matching rules.";

    private final RuleRetriever retriever;
    private final DisputeAdvisor advisor;
    private final DisputeRepository disputeRepository;

    public DisputeService(RuleRetriever retriever, DisputeAdvisor advisor, DisputeRepository disputeRepository) {
        this.retriever = retriever;
        this.advisor = advisor;
        this.disputeRepository = disputeRepository;
    }

    public DisputeResponse analyse(AnalyseDisputeRequest request) {
        String description = request.description().strip();

        List<RetrievedChunk> chunks = retriever.retrieve(description);
        if (chunks.isEmpty()) {
            throw new InsufficientContextException(NO_MATCHING_RULES);
        }

        Map<String, RetrievedChunk> sources = labelSources(chunks);
        DisputeRecommendation recommendation = advisor.advise(description, sources);
        if (recommendation == null || !recommendation.supported() || recommendation.category() == null) {
            throw new InsufficientContextException(declineReason(recommendation));
        }

        List<DisputeCitation> citations = resolveCitations(recommendation.sourcesOrEmpty(), sources);
        if (citations.isEmpty()) {
            log.warn("Model sources {} match none of the retrieved labels {}",
                    recommendation.sourcesOrEmpty(), sources.keySet());
            throw new InsufficientContextException(NO_CITATIONS);
        }

        Dispute dispute = new Dispute(
                description,
                recommendation.category(),
                recommendation.requiredEvidenceOrEmpty(),
                recommendation.explanation(),
                citations);

        return DisputeResponse.from(disputeRepository.save(dispute));
    }

    public DisputeResponse get(UUID id) {
        return disputeRepository.findById(id)
                .map(DisputeResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute", id));
    }

    private static Map<String, RetrievedChunk> labelSources(List<RetrievedChunk> chunks) {
        Map<String, RetrievedChunk> sources = new LinkedHashMap<>();
        for (int i = 0; i < chunks.size(); i++) {
            sources.put("S" + (i + 1), chunks.get(i));
        }
        return sources;
    }

    private static List<DisputeCitation> resolveCitations(List<String> labels, Map<String, RetrievedChunk> sources) {
        return labels.stream()
                .filter(Objects::nonNull)
                .map(label -> label.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT))
                .distinct()
                .filter(sources::containsKey)
                .map(label -> DisputeCitation.from(sources.get(label)))
                .toList();
    }

    private static String declineReason(DisputeRecommendation recommendation) {
        return recommendation != null && StringUtils.hasText(recommendation.explanation())
                ? recommendation.explanation()
                : MODEL_DECLINED;
    }
}