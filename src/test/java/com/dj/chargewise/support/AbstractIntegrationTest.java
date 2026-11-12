package com.dj.chargewise.support;

import com.dj.chargewise.TestcontainersConfiguration;
import com.dj.chargewise.model.IngestionStatus;
import com.dj.chargewise.repository.KnowledgeDocumentRepository;
import com.dj.chargewise.util.CorrelationIdFilter;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.ai.openai.api-key=test-key",
        "chargewise.seed.enabled=true",
        "chargewise.security.jwt-secret=test-secret-that-is-at-least-32-characters-long",
        "ADMIN_PASSWORD=admin-test",
        "AGENT_PASSWORD=agent-test",
        "chargewise.resilience.initial-backoff=50ms",
        "chargewise.resilience.sliding-window-size=1000",
        "chargewise.resilience.minimum-number-of-calls=1000",
        "chargewise.rate-limit.capacity=1000"
})
@Import({TestcontainersConfiguration.class, FakeEmbeddingModelConfig.class})
public abstract class AbstractIntegrationTest {

    protected static final String ADMIN = "admin";
    protected static final String ADMIN_PASSWORD = "admin-test";
    protected static final String AGENT = "agent";
    protected static final String AGENT_PASSWORD = "agent-test";
    protected static final int SEED_DOCUMENT_COUNT = 5;

    protected static final WireMockServer openAi = new WireMockServer(wireMockConfig().dynamicPort());

    static {
        openAi.start();
    }

    @Autowired
    protected KnowledgeDocumentRepository documentRepository;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private CorrelationIdFilter correlationIdFilter;

    protected MockMvc mockMvc;

    @DynamicPropertySource
    static void openAiProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.openai.base-url", openAi::baseUrl);
    }

    @BeforeEach
    void setUpMockMvcAndWaitForSeedData() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(correlationIdFilter)
                .apply(springSecurity())
                .build();
        openAi.resetAll();

        await().atMost(Duration.ofSeconds(30)).until(() ->
                documentRepository.count() >= SEED_DOCUMENT_COUNT
                        && documentRepository.findAll().stream()
                        .noneMatch(document -> document.getStatus() == IngestionStatus.PENDING));
    }

    protected String agentToken() throws Exception {
        return login(AGENT, AGENT_PASSWORD);
    }

    protected String adminToken() throws Exception {
        return login(ADMIN, ADMIN_PASSWORD);
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }

    private String login(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "%s", "password": "%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }
}
