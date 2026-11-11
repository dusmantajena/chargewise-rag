package com.dj.chargewise.service.dispute;

import com.dj.chargewise.model.DisputeRecommendation;
import com.dj.chargewise.model.RetrievedChunk;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

@Component
public class DisputeAdvisor {

    private final ChatClient chatClient;
    private final Resource systemPrompt;
    private final Resource userPrompt;

    public DisputeAdvisor(ChatClient chatClient,
                          @Value("classpath:prompts/dispute-system.st") Resource systemPrompt,
                          @Value("classpath:prompts/dispute-user.st") Resource userPrompt) {
        this.chatClient = chatClient;
        this.systemPrompt = systemPrompt;
        this.userPrompt = userPrompt;
    }

    public DisputeRecommendation advise(String description, Map<String, RetrievedChunk> sources) {
        Map<String, Object> params = Map.of(
                "description", description,
                "rules", formatSources(sources));

        return chatClient.prompt()
                .system(systemPrompt)
                .user(user -> user.text(userPrompt).params(params))
                .call()
                .entity(DisputeRecommendation.class);
    }

    private static String formatSources(Map<String, RetrievedChunk> sources) {
        return sources.entrySet().stream()
                .map(entry -> "[%s] %s (%s)%n%s".formatted(
                        entry.getKey(),
                        entry.getValue().filename(),
                        entry.getValue().category(),
                        entry.getValue().content()))
                .collect(Collectors.joining("\n\n"));
    }
}
