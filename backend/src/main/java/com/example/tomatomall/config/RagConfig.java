package com.example.tomatomall.config;

import com.example.tomatomall.dao.ProductRepository;
import com.example.tomatomall.dto.RecommendationResponse;
import com.example.tomatomall.po.Product;
import com.example.tomatomall.service.RagService;
import com.example.tomatomall.service.serviceImpl.ShoppingTools;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.V;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class RagConfig {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ShoppingTools shoppingTools; // 新增注入

    // 1. 模型配置 - 向量模型
    @Bean
    public EmbeddingModel qwenEmbeddingModel() {
        return QwenEmbeddingModel.builder()
                .apiKey("sk-57bfbab1181d4a1095189f704ab968f9")
                .modelName("text-embedding-v3")
                .build();
    }

    // 2. 模型配置 - 大语言模型
    @Bean
    public ChatLanguageModel qwenChatModel() {
        return QwenChatModel.builder()
                .apiKey("sk-57bfbab1181d4a1095189f704ab968f9")
                .modelName("qwen-plus-latest")
                .build();
    }
    /*
    QwenChatModel.builder()
                .apiKey("sk-57bfbab1181d4a1095189f704ab968f9")
                .modelName("deepseek-v3")
                .build();这个不支持工具调用 先放一放


    .baseUrl("http://langchain4j.dev/demo/openai/v1") // 新增baseUrl配置
                .apiKey("demo") // 确保使用demo密钥
                .modelName(GPT_4_O_MINI)
                .build();

     */
    /*

| modelName                  | 速度   | 性能等级 | 上下文长度 |
|----------------------------|--------|----------|------------|
| `qwen-plus-latest`         | 中速   | 高       | 128K       |
| `qwen-turbo-latest`        | 极速   | 中       | 1M         |
| `qwen3-235b-a22b`          | 低速   | 最高     | 128K       |
| `qwen3-30b-a3b`            | 中低速 | 高       | 128K       |
| `qwen3-32b`                | 中低速 | 高       | 128K       |
| `qwen3-14b`                | 中速   | 中高     | 128K       |
| `qwen3-8b`                 | 中速   | 中       | 128K       |
| `qwen3-4b`                 | 中高速 | 中       | 128K       |
| `qwen3-1.7b`               | 高速   | 中低     | 32K        |
| `qwen3-0.6b`               | 极速   | 低       | 32K        |
| `qwen-turbo-2025-04-28`    | 极速   | 中       | 1M         |
     */

    // 3. 内存向量存储
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(EmbeddingModel embeddingModel) {
        InMemoryEmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<>();

        // 分页批量加载商品数据
        int pageSize = 100;
        int pageNumber = 0;
        List<Product> allProducts = new ArrayList<>();

        Page<Product> page;
        do {
            page = productRepository.findAllForRag(PageRequest.of(pageNumber, pageSize));
            allProducts.addAll(page.getContent());
            pageNumber++;
        } while (page.hasNext());

        // 转换文本片段
        List<TextSegment> segments = allProducts.stream()
                .map(p -> TextSegment.from(p.toRagText()))
                .collect(Collectors.toList());

        // 生成向量并存储
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
        store.addAll(embeddings, segments);

        return store;
    }

    // 4. RAG内容检索器
    @Bean
    public ContentRetriever contentRetriever(EmbeddingStore<TextSegment> embeddingStore,
                                             EmbeddingModel embeddingModel) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(3)
                .minScore(0.3)
                .build();
    }

    // 5. 聊天记忆提供者
    // 在RagConfig中增强记忆配置
    // 在RagConfig.java中强化记忆配置
    @Bean
    public ChatMemoryProvider chatMemoryProvider() {

        ChatMemoryProvider chatMemoryProvider = memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(100)
                .build();
        return chatMemoryProvider;
    }

    // 6. RAG服务组装
    @Bean
    public RagService ragService(ChatLanguageModel chatModel,
                                 ContentRetriever contentRetriever,
                                 ChatMemoryProvider chatMemoryProvider, // 使用已定义的Bean
                                 ShoppingTools shoppingTools) {
        return AiServices.builder(RagService.class)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .chatLanguageModel(chatModel)
                .contentRetriever(contentRetriever)
                .tools(shoppingTools)
                .build();

    }

}