package com.dj.chargewise.service.dispute;

import com.dj.chargewise.dto.request.AnalyseDisputeRequest;
import com.dj.chargewise.dto.response.DisputeResponse;
import com.dj.chargewise.exception.IdempotencyConflictException;
import com.dj.chargewise.exception.InsufficientContextException;
import com.dj.chargewise.exception.InvalidRequestException;
import com.dj.chargewise.model.IdempotencyRecord;
import com.dj.chargewise.model.IdempotentResult;
import com.dj.chargewise.model.ReasonCategory;
import com.dj.chargewise.repository.IdempotencyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    private static final String USER = "agent";
    private static final String KEY = "key-0001";
    private static final AnalyseDisputeRequest REQUEST =
            new AnalyseDisputeRequest("Customer was charged twice for one purchase.");

    @Mock
    private IdempotencyRepository repository;

    @Mock
    private DisputeService disputeService;

    @InjectMocks
    private IdempotencyService service;

    @Test
    void withoutKeyAnalysesDirectly() {
        DisputeResponse response = response();
        when(disputeService.analyse(REQUEST)).thenReturn(response);

        IdempotentResult result = service.analyse(USER, null, REQUEST);

        assertThat(result.response()).isEqualTo(response);
        assertThat(result.replayed()).isFalse();
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsMalformedKey() {
        assertThatThrownBy(() -> service.analyse(USER, "not a valid key!", REQUEST))
                .isInstanceOf(InvalidRequestException.class);
        verifyNoInteractions(repository, disputeService);
    }

    @Test
    void firstRequestAnalysesAndRecordsResult() {
        DisputeResponse response = response();
        when(repository.claim(eq(USER), eq(KEY), anyString(), any())).thenReturn(true);
        when(disputeService.analyse(REQUEST)).thenReturn(response);

        IdempotentResult result = service.analyse(USER, KEY, REQUEST);

        assertThat(result.replayed()).isFalse();
        verify(repository).complete(USER, KEY, response.id());
    }

    @Test
    void failedAnalysisReleasesTheKey() {
        when(repository.claim(eq(USER), eq(KEY), anyString(), any())).thenReturn(true);
        when(disputeService.analyse(REQUEST)).thenThrow(new InsufficientContextException("No rules match."));

        assertThatThrownBy(() -> service.analyse(USER, KEY, REQUEST))
                .isInstanceOf(InsufficientContextException.class);
        verify(repository).release(USER, KEY);
        verify(repository, never()).complete(any(), any(), any());
    }

    @Test
    void repeatedKeyWithSameBodyReplaysStoredResult() {
        String requestHash = claimFailsAndReturnRequestHash();
        DisputeResponse stored = response();
        when(repository.find(USER, KEY)).thenReturn(Optional.of(new IdempotencyRecord(requestHash, stored.id())));
        when(disputeService.get(stored.id())).thenReturn(stored);

        IdempotentResult result = service.analyse(USER, KEY, REQUEST);

        assertThat(result.replayed()).isTrue();
        assertThat(result.response()).isEqualTo(stored);
        verify(disputeService, never()).analyse(any());
    }

    @Test
    void repeatedKeyWithDifferentBodyIsRejected() {
        claimFailsAndReturnRequestHash();
        when(repository.find(USER, KEY)).thenReturn(Optional.of(new IdempotencyRecord("other-hash", UUID.randomUUID())));

        assertThatThrownBy(() -> service.analyse(USER, KEY, REQUEST))
                .isInstanceOf(IdempotencyConflictException.class)
                .hasMessageContaining("different request");
    }

    @Test
    void repeatedKeyStillInProgressIsRejected() {
        String requestHash = claimFailsAndReturnRequestHash();
        when(repository.find(USER, KEY)).thenReturn(Optional.of(new IdempotencyRecord(requestHash, null)));

        assertThatThrownBy(() -> service.analyse(USER, KEY, REQUEST))
                .isInstanceOf(IdempotencyConflictException.class)
                .hasMessageContaining("still being processed");
    }

    private String claimFailsAndReturnRequestHash() {
        when(repository.claim(eq(USER), eq(KEY), anyString(), any())).thenReturn(false);
        return sha256(REQUEST.description());
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static DisputeResponse response() {
        return new DisputeResponse(UUID.randomUUID(), REQUEST.description(), ReasonCategory.DUPLICATE_PROCESSING,
                List.of("Evidence"), "Explanation", List.of(), Instant.now());
    }
}
