package com.dj.chargewise;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.ai.openai.api-key=test-key",
        "chargewise.seed.enabled=false",
        "chargewise.security.jwt-secret=test-secret-that-is-at-least-32-characters-long",
        "ADMIN_PASSWORD=admin-test",
        "AGENT_PASSWORD=agent-test"
})
@Import(TestcontainersConfiguration.class)
class ChargewiseApplicationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoadsAndSchemaIsMigrated() {
        Integer vectorExtension = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_extension WHERE extname = 'vector'", Integer.class);
        Integer tables = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM information_schema.tables
                WHERE table_name IN ('documents', 'vector_store', 'disputes', 'dispute_citations')
                """, Integer.class);

        assertThat(vectorExtension).isEqualTo(1);
        assertThat(tables).isEqualTo(4);
    }
}
