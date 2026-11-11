package com.dj.chargewise.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class VectorChunkRepository {

    private static final String DELETE_BY_DOCUMENT_ID =
            "DELETE FROM vector_store WHERE metadata ->> 'documentId' = ?";

    private final JdbcTemplate jdbcTemplate;

    public VectorChunkRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int deleteByDocumentId(UUID documentId) {
        return jdbcTemplate.update(DELETE_BY_DOCUMENT_ID, documentId.toString());
    }
}
