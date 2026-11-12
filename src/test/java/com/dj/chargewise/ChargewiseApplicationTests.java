package com.dj.chargewise;

import com.dj.chargewise.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class ChargewiseApplicationTests extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoadsAndSchemaIsMigrated() {
        Integer vectorExtension = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_extension WHERE extname = 'vector'", Integer.class);
        Integer tables = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM information_schema.tables
                WHERE table_name IN ('documents', 'vector_store', 'disputes', 'dispute_citations', 'idempotency_keys')
                """, Integer.class);

        assertThat(vectorExtension).isEqualTo(1);
        assertThat(tables).isEqualTo(5);
    }

    @Test
    void seedRulesAreIngestedOnStartup() {
        Integer chunks = jdbcTemplate.queryForObject("SELECT count(*) FROM vector_store", Integer.class);

        assertThat(documentRepository.count()).isGreaterThanOrEqualTo(SEED_DOCUMENT_COUNT);
        assertThat(chunks).isPositive();
    }
}
