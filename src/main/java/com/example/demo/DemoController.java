package com.example.demo;

import com.example.demo.model.IngestData;
import com.example.demo.model.Question;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {
    private static final Logger log = LoggerFactory.getLogger(DemoController.class);

    private final ChatLanguageModel chatLanguageModel;

    private final EmbeddingModel embeddingModel;

    private final EmbeddingStore<TextSegment> embeddingStore;

    private Agent assistant = null;

    @Value("${persona}")
    private String persona;

    public DemoController(ChatLanguageModel chatLanguageModel, EmbeddingModel embeddingModel, EmbeddingStore<TextSegment> embeddingStore) {
        this.chatLanguageModel = chatLanguageModel;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
    }

    @PostConstruct
    public void init() {
        initChain();
    }

    private void initChain() {
        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(20);

        EmbeddingStoreContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .build();

        assistant = AiServices.builder(Agent.class)
                .chatLanguageModel(chatLanguageModel)
                .chatMemory(chatMemory)
                .contentRetriever(contentRetriever)
                .build();
    }

    @PostMapping("/ingest")
    void ingest(@RequestBody IngestData data) {
        TextSegment textSegment = TextSegment.from(data.getText());
        Embedding embedding = embeddingModel.embed(data.getText()).content();
        embeddingStore.add(embedding, textSegment);

        log.info("Ingested: {}", data.getText());
    }

    @PostMapping("/ask")
    Question ask(@RequestBody Question question) {
        question.setAnswer(assistant.chat(persona + question.getQuestion()));
        log.info("Asked: {}, Answer: {}", question.getQuestion(), question.getAnswer());

        return question;
    }
}