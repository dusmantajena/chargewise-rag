package com.dj.chargewise.service.dispute;

import com.dj.chargewise.dto.request.AnalyseDisputeRequest;
import com.dj.chargewise.dto.response.DisputeResponse;
import com.dj.chargewise.entity.Dispute;
import com.dj.chargewise.exception.InsufficientContextException;
import com.dj.chargewise.exception.ResourceNotFoundException;
import com.dj.chargewise.model.DisputeRecommendation;
import com.dj.chargewise.model.ReasonCategory;
import com.dj.chargewise.model.RetrievedChunk;
import com.dj.chargewise.repository.DisputeRepository;
import com.dj.chargewise.service.OpenAiResilience;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisputeServiceTest {

    private static final AnalyseDisputeRequest REQUEST =
            new AnalyseDisputeRequest("Customer was charged twice for one purchase.");

    @Mock
    private RuleRetriever retriever;

    @Mock
    private DisputeAdvisor advisor;

    @Mock
    private DisputeRepository repository;

    @Mock
    private OpenAiResilience openAiResilience;

    @InjectMocks
    private DisputeService service;

    @BeforeEach
    void runResilientCallsDirectly() {
        lenient().when(openAiResilience.call(any())).thenAnswer(invocation ->
                invocation.<Supplier<?>>getArgument(0).get());
    }

    @Test
    void refusesWhenNoRulesMatch() {
        when(retriever.retrieve(anyString())).thenReturn(List.of());

        assertThatThrownBy(() -> service.analyse(REQUEST))
                .isInstanceOf(InsufficientContextException.class)
                .hasMessageContaining("No rules match");
        verifyNoInteractions(advisor, repository);
    }

    @Test
    void refusesWhenModelSaysRulesDoNotApply() {
        when(retriever.retrieve(anyString())).thenReturn(List.of(chunk("duplicate-processing.txt")));
        when(advisor.advise(anyString(), anyMap())).thenReturn(new DisputeRecommendation(
                false, null, List.of(), "Not covered by the rules.", List.of()));

        assertThatThrownBy(() -> service.analyse(REQUEST))
                .isInstanceOf(InsufficientContextException.class)
                .hasMessage("Not covered by the rules.");
        verify(repository, never()).save(any());
    }

    @Test
    void refusesWhenModelCitesUnknownSources() {
        when(retriever.retrieve(anyString())).thenReturn(List.of(chunk("duplicate-processing.txt")));
        when(advisor.advise(anyString(), anyMap())).thenReturn(new DisputeRecommendation(
                true, ReasonCategory.DUPLICATE_PROCESSING, List.of("Evidence"), "Explanation", List.of("P40.2")));

        assertThatThrownBy(() -> service.analyse(REQUEST))
                .isInstanceOf(InsufficientContextException.class)
                .hasMessageContaining("did not cite");
        verify(repository, never()).save(any());
    }

    @Test
    void storesRecommendationWithResolvedCitationsAndCleanExplanation() {
        when(retriever.retrieve(anyString())).thenReturn(List.of(
                chunk("duplicate-processing.txt"), chunk("credit-not-processed.txt")));
        when(advisor.advise(anyString(), anyMap())).thenReturn(new DisputeRecommendation(
                true, ReasonCategory.DUPLICATE_PROCESSING,
                List.of("References of both transactions"),
                "Charged twice within minutes, which meets P40.2 and section S1.",
                List.of("[S1]", "S9")));
        when(repository.save(any(Dispute.class))).then(returnsFirstArg());

        DisputeResponse response = service.analyse(REQUEST);

        ArgumentCaptor<Dispute> saved = ArgumentCaptor.forClass(Dispute.class);
        verify(repository).save(saved.capture());
        assertThat(response.category()).isEqualTo(ReasonCategory.DUPLICATE_PROCESSING);
        assertThat(response.explanation()).isEqualTo("Charged twice within minutes, which meets P40.2.");
        assertThat(response.citations()).singleElement()
                .satisfies(citation -> assertThat(citation.document()).isEqualTo("duplicate-processing.txt"));
        assertThat(saved.getValue().getRequiredEvidence()).containsExactly("References of both transactions");
    }

    @Test
    void getThrowsWhenDisputeDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(id)).isInstanceOf(ResourceNotFoundException.class);
    }

    private static RetrievedChunk chunk(String filename) {
        return new RetrievedChunk(UUID.randomUUID().toString(), UUID.randomUUID().toString(), filename,
                ReasonCategory.DUPLICATE_PROCESSING, "Rule text from " + filename, 0.9);
    }
}
