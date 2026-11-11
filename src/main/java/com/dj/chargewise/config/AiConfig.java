package com.dj.chargewise.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Bean
    ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    @Bean
    TokenTextSplitter tokenTextSplitter(ChargewiseProperties properties) {
        ChargewiseProperties.Ingestion ingestion = properties.ingestion();
        return TokenTextSplitter.builder()
                .withChunkSize(ingestion.chunkSize())
                .withMinChunkSizeChars(ingestion.minChunkSizeChars())
                .withMinChunkLengthToEmbed(10)
                .withMaxNumChunks(10_000)
                .withKeepSeparator(true)
                .build();
    }
}
