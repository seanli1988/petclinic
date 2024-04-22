package com.example.demo.configuration;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.azure.search.AzureAiSearchEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddingStoreConfiguration {
    @Value("${AZURE_SEARCH_ENDPOINT}")
    private String endpoint;

    @Value("${AZURE_SEARCH_KEY}")
    private String searchkey;
    @Bean
    EmbeddingStore<TextSegment> embeddingStore() {
        return AzureAiSearchEmbeddingStore.builder()
                .endpoint(endpoint)
                .apiKey(searchkey)
                .dimensions(1536)
                .build();
    }
}
