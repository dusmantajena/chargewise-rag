package com.dj.chargewise.support;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

public final class OpenAiStubs {

    public static final UrlPathPattern CHAT_COMPLETIONS = urlPathMatching(".*/chat/completions");

    public static final String DUPLICATE_PROCESSING_RECOMMENDATION = """
            {
              "supported": true,
              "category": "DUPLICATE_PROCESSING",
              "requiredEvidence": [
                "References of both the original and the duplicate transaction",
                "Cardholder statement confirming that only one purchase was made"
              ],
              "explanation": "The same amount was charged twice within minutes, which meets P40.2 and section S1.",
              "sources": ["S1"]
            }
            """;

    public static final String UNSUPPORTED_RECOMMENDATION = """
            {
              "supported": false,
              "category": null,
              "requiredEvidence": [],
              "explanation": "The rules do not cover complaints about customer service.",
              "sources": []
            }
            """;

    private OpenAiStubs() {
    }

    public static void stubRecommendation(WireMockServer server, String recommendationJson) {
        server.stubFor(post(CHAT_COMPLETIONS).willReturn(okJson(chatCompletion(recommendationJson))));
    }

    public static void stubFailure(WireMockServer server, int status) {
        server.stubFor(post(CHAT_COMPLETIONS).willReturn(aResponse()
                .withStatus(status)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                        {"error": {"message": "Simulated failure", "type": "invalid_request_error", "code": null}}
                        """)));
    }

    private static String chatCompletion(String content) {
        return """
                {
                  "id": "chatcmpl-test",
                  "object": "chat.completion",
                  "created": 1767225600,
                  "model": "gpt-4.1-mini",
                  "choices": [
                    {
                      "index": 0,
                      "message": {"role": "assistant", "content": "%s", "refusal": null},
                      "logprobs": null,
                      "finish_reason": "stop"
                    }
                  ],
                  "usage": {"prompt_tokens": 100, "completion_tokens": 50, "total_tokens": 150}
                }
                """.formatted(escape(content.strip()));
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}
