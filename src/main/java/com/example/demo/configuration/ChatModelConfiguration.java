package com.example.demo.configuration;

import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatModelConfiguration {
    @Value("${AZURE_OPENAI_ENDPOINT}")
    private String endpoint;

    @Value("${AZURE_OPENAI_KEY}")
    private String aikey;

    @Value("${AZURE_OPENAI_DEPLOYMENTNAME_CHAT}")
    private String deploymentName;

    @Bean
    ChatLanguageModel openAIChatLanguageModel() {
        return AzureOpenAiChatModel.builder()
                .endpoint(endpoint)
                .apiKey(aikey)
                .deploymentName(deploymentName)
                .logRequestsAndResponses(true)
                .build();
    }


}
