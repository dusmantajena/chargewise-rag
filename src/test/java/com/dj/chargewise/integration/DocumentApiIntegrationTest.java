package com.dj.chargewise.integration;

import com.dj.chargewise.model.IngestionStatus;
import com.dj.chargewise.support.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DocumentApiIntegrationTest extends AbstractIntegrationTest {

    private static final String DOCUMENTS = "/api/v1/documents";
    private static final String RULE_TEXT = """
            Section T10.1 Definition
            Reason code T10 applies when a test merchant fails to deliver goods.
            Section T10.2 Required issuer evidence
            - Cardholder statement
            """;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void uploadedDocumentIsIngestedAndDeletedWithItsVectors() throws Exception {
        String token = adminToken();

        String body = mockMvc.perform(multipart(DOCUMENTS)
                        .file(textFile("test-rules.txt"))
                        .param("reasonCategory", "GOODS_NOT_RECEIVED")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.reasonCategory").value("GOODS_NOT_RECEIVED"))
                .andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(JsonPath.read(body, "$.id"));

        await().atMost(Duration.ofSeconds(15)).until(() ->
                documentRepository.findById(id).orElseThrow().getStatus() == IngestionStatus.COMPLETED);
        assertThat(vectorCount(id)).isPositive();

        mockMvc.perform(delete(DOCUMENTS + "/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());

        assertThat(documentRepository.existsById(id)).isFalse();
        assertThat(vectorCount(id)).isZero();
    }

    @Test
    void listReturnsPagedDocuments() throws Exception {
        mockMvc.perform(get(DOCUMENTS).param("size", "2").header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(SEED_DOCUMENT_COUNT)));
    }

    @Test
    void unsupportedFileTypeReturns400() throws Exception {
        mockMvc.perform(multipart(DOCUMENTS)
                        .file(textFile("rules.exe"))
                        .param("reasonCategory", "GOODS_NOT_RECEIVED")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid document"));
    }

    @Test
    void missingReasonCategoryReturnsFieldError() throws Exception {
        mockMvc.perform(multipart(DOCUMENTS)
                        .file(textFile("rules.txt"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("reasonCategory"));
    }

    @Test
    void agentCannotManageDocuments() throws Exception {
        mockMvc.perform(get(DOCUMENTS).header(HttpHeaders.AUTHORIZATION, bearer(agentToken())))
                .andExpect(status().isForbidden());
    }

    @Test
    void deletingUnknownDocumentReturns404() throws Exception {
        mockMvc.perform(delete(DOCUMENTS + "/{id}", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isNotFound());
    }

    private static MockMultipartFile textFile(String filename) {
        return new MockMultipartFile("file", filename, "text/plain", RULE_TEXT.getBytes(StandardCharsets.UTF_8));
    }

    private int vectorCount(UUID documentId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM vector_store WHERE metadata ->> 'documentId' = ?",
                Integer.class, documentId.toString());
        return count == null ? 0 : count;
    }
}
