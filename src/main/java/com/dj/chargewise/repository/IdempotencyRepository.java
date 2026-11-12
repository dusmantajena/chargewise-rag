package com.dj.chargewise.repository;

import com.dj.chargewise.model.IdempotencyRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Repository
public class IdempotencyRepository {

    private static final String CLAIM = """
            INSERT INTO idempotency_keys (username, idempotency_key, request_hash)
            VALUES (?, ?, ?)
            ON CONFLICT (username, idempotency_key) DO UPDATE
                SET request_hash = EXCLUDED.request_hash, created_at = now()
                WHERE idempotency_keys.dispute_id IS NULL
                  AND idempotency_keys.created_at < now() - (? * interval '1 second')
            """;

    private static final String FIND = """
            SELECT request_hash, dispute_id FROM idempotency_keys
            WHERE username = ? AND idempotency_key = ?
            """;

    private static final String COMPLETE = """
            UPDATE idempotency_keys SET dispute_id = ?
            WHERE username = ? AND idempotency_key = ?
            """;

    private static final String RELEASE = """
            DELETE FROM idempotency_keys
            WHERE username = ? AND idempotency_key = ? AND dispute_id IS NULL
            """;

    private final JdbcTemplate jdbcTemplate;

    public IdempotencyRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean claim(String username, String key, String requestHash, Duration staleAfter) {
        return jdbcTemplate.update(CLAIM, username, key, requestHash, staleAfter.toSeconds()) == 1;
    }

    public Optional<IdempotencyRecord> find(String username, String key) {
        return jdbcTemplate.query(FIND,
                (rs, rowNum) -> new IdempotencyRecord(rs.getString("request_hash"),
                        rs.getObject("dispute_id", UUID.class)),
                username, key).stream().findFirst();
    }

    public void complete(String username, String key, UUID disputeId) {
        jdbcTemplate.update(COMPLETE, disputeId, username, key);
    }

    public void release(String username, String key) {
        jdbcTemplate.update(RELEASE, username, key);
    }
}
