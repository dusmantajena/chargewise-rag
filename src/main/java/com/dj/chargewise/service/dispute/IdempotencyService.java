package com.dj.chargewise.service.dispute;

import com.dj.chargewise.dto.request.AnalyseDisputeRequest;
import com.dj.chargewise.dto.response.DisputeResponse;
import com.dj.chargewise.exception.IdempotencyConflictException;
import com.dj.chargewise.exception.InvalidRequestException;
import com.dj.chargewise.model.IdempotencyRecord;
import com.dj.chargewise.model.IdempotentResult;
import com.dj.chargewise.repository.IdempotencyRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.regex.Pattern;

@Service
public class IdempotencyService {

    private static final Pattern VALID_KEY = Pattern.compile("[A-Za-z0-9_-]{1,100}");
    private static final Duration STALE_AFTER = Duration.ofMinutes(5);

    private final IdempotencyRepository repository;
    private final DisputeService disputeService;

    public IdempotencyService(IdempotencyRepository repository, DisputeService disputeService) {
        this.repository = repository;
        this.disputeService = disputeService;
    }

    public IdempotentResult analyse(String username, String idempotencyKey, AnalyseDisputeRequest request) {
        if (idempotencyKey == null) {
            return new IdempotentResult(disputeService.analyse(request), false);
        }
        if (!VALID_KEY.matcher(idempotencyKey).matches()) {
            throw new InvalidRequestException(
                    "Idempotency-Key must be 1 to 100 characters of letters, digits, '-' or '_'.");
        }

        String requestHash = hash(request.description().strip());
        if (repository.claim(username, idempotencyKey, requestHash, STALE_AFTER)) {
            return analyseOnce(username, idempotencyKey, request);
        }
        return replay(username, idempotencyKey, requestHash);
    }

    private IdempotentResult analyseOnce(String username, String idempotencyKey, AnalyseDisputeRequest request) {
        try {
            DisputeResponse response = disputeService.analyse(request);
            repository.complete(username, idempotencyKey, response.id());
            return new IdempotentResult(response, false);
        } catch (RuntimeException ex) {
            repository.release(username, idempotencyKey);
            throw ex;
        }
    }

    private IdempotentResult replay(String username, String idempotencyKey, String requestHash) {
        IdempotencyRecord existing = repository.find(username, idempotencyKey)
                .orElseThrow(() -> new IdempotencyConflictException(
                        "The previous request with this Idempotency-Key just finished. Retry the request."));

        if (!existing.requestHash().equals(requestHash)) {
            throw new IdempotencyConflictException(
                    "Idempotency-Key '%s' was already used with a different request.".formatted(idempotencyKey));
        }
        if (!existing.isCompleted()) {
            throw new IdempotencyConflictException(
                    "A request with Idempotency-Key '%s' is still being processed.".formatted(idempotencyKey));
        }
        return new IdempotentResult(disputeService.get(existing.disputeId()), true);
    }

    private static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
