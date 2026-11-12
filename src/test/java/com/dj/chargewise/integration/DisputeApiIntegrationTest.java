package com.dj.chargewise.integration;

import com.dj.chargewise.support.AbstractIntegrationTest;
import com.dj.chargewise.support.FakeEmbeddingModel;
import com.dj.chargewise.support.OpenAiStubs;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.moreThanOrExactly;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DisputeApiIntegrationTest extends AbstractIntegrationTest {

    private static final String ANALYSE = "/api/v1/disputes/analyse";
    private static final String DUPLICATE_CLAIM =
            "Customer paid once at the supermarket but was charged the same amount twice, two minutes apart.";
    private static final String OFF_TOPIC_CLAIM =
            "Customer is unhappy that the " + FakeEmbeddingModel.OFF_TOPIC_MARKER + " closed early on Saturday.";

    @Test
    void analyseReturnsGroundedRecommendationAndStoresIt() throws Exception {
        OpenAiStubs.stubRecommendation(openAi, OpenAiStubs.DUPLICATE_PROCESSING_RECOMMENDATION);
        String token = agentToken();

        String body = analyse(token, null, DUPLICATE_CLAIM)
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, containsString("/api/v1/disputes/")))
                .andExpect(jsonPath("$.category").value("DUPLICATE_PROCESSING"))
                .andExpect(jsonPath("$.requiredEvidence.length()").value(2))
                .andExpect(jsonPath("$.citations[0].document").value(notNullValue()))
                .andExpect(jsonPath("$.explanation").value(not(containsString("S1"))))
                .andReturn().getResponse().getContentAsString();

        String id = JsonPath.read(body, "$.id");
        mockMvc.perform(get("/api/v1/disputes/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.category").value("DUPLICATE_PROCESSING"));
    }

    @Test
    void offTopicDisputeIsRefusedWithoutCallingTheChatModel() throws Exception {
        OpenAiStubs.stubRecommendation(openAi, OpenAiStubs.DUPLICATE_PROCESSING_RECOMMENDATION);

        analyse(agentToken(), null, OFF_TOPIC_CLAIM)
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.title").value("Insufficient context"));

        openAi.verify(exactly(0), postRequestedFor(OpenAiStubs.CHAT_COMPLETIONS));
    }

    @Test
    void disputeIsRefusedWhenTheModelSaysTheRulesDoNotApply() throws Exception {
        OpenAiStubs.stubRecommendation(openAi, OpenAiStubs.UNSUPPORTED_RECOMMENDATION);

        analyse(agentToken(), null, DUPLICATE_CLAIM)
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.detail").value(containsString("do not cover")));
    }

    @Test
    void invalidRequestReturnsFieldErrors() throws Exception {
        analyse(agentToken(), null, "")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("description"))
                .andExpect(jsonPath("$.correlationId").value(notNullValue()));
    }

    @Test
    void missingTokenReturns401() throws Exception {
        mockMvc.perform(post(ANALYSE).contentType(MediaType.APPLICATION_JSON).content(json(DUPLICATE_CLAIM)))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.correlationId").value(notNullValue()));
    }

    @Test
    void adminTokenReturns403() throws Exception {
        analyse(adminToken(), null, DUPLICATE_CLAIM)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Forbidden"));
    }

    @Test
    void unknownDisputeReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/disputes/{id}", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken())))
                .andExpect(status().isNotFound());
    }

    @Test
    void repeatedIdempotencyKeyReplaysStoredResultWithoutNewAnalysis() throws Exception {
        OpenAiStubs.stubRecommendation(openAi, OpenAiStubs.DUPLICATE_PROCESSING_RECOMMENDATION);
        String token = agentToken();
        String key = UUID.randomUUID().toString();

        String first = analyse(token, key, DUPLICATE_CLAIM)
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Idempotent-Replayed"))
                .andReturn().getResponse().getContentAsString();

        analyse(token, key, DUPLICATE_CLAIM)
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotent-Replayed", "true"))
                .andExpect(jsonPath("$.id").value(JsonPath.<String>read(first, "$.id")));

        openAi.verify(exactly(1), postRequestedFor(OpenAiStubs.CHAT_COMPLETIONS));
    }

    @Test
    void idempotencyKeyReusedWithDifferentBodyReturns409() throws Exception {
        OpenAiStubs.stubRecommendation(openAi, OpenAiStubs.DUPLICATE_PROCESSING_RECOMMENDATION);
        String token = agentToken();
        String key = UUID.randomUUID().toString();

        analyse(token, key, DUPLICATE_CLAIM).andExpect(status().isCreated());

        analyse(token, key, "Customer says the parcel never arrived and the merchant does not reply.")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Idempotency conflict"));
    }

    @Test
    void chatModelFailureIsRetriedThenReturns503WithRetryAfter() throws Exception {
        OpenAiStubs.stubFailure(openAi, 400);

        analyse(agentToken(), null, DUPLICATE_CLAIM)
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(jsonPath("$.title").value("AI service unavailable"));

        openAi.verify(moreThanOrExactly(3), postRequestedFor(OpenAiStubs.CHAT_COMPLETIONS));
    }

    private ResultActions analyse(String token, String idempotencyKey, String description) throws Exception {
        var request = post(ANALYSE)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(description));
        if (idempotencyKey != null) {
            request.header("Idempotency-Key", idempotencyKey);
        }
        return mockMvc.perform(request);
    }

    private static String json(String description) {
        return """
                {"description": "%s"}
                """.formatted(description);
    }
}
