package com.Ai_Agent.ai_agent.rag;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.transformer.KeywordMetadataEnricher;
import org.springframework.stereotype.Component;

import java.util.List;

/*
基于AI文档的元信息增强器（为文档补充元信息）
 */
@Component
public class MyKeywordEnricher {

    @Resource
    private ChatModel dashscopeChatModel;

    private static final String CHINESE_KEYWORDS_TEMPLATE =
            "{context_str}。从中提取 %s 个关键词，用中文逗号分隔。关键词：";

    public List<Document> enrichDocuments(List<Document> documents) {
        KeywordMetadataEnricher enricher = KeywordMetadataEnricher.builder(dashscopeChatModel)
                .keywordCount(5)
                .keywordsTemplate(new PromptTemplate(CHINESE_KEYWORDS_TEMPLATE))
                .build();
        return enricher.apply(documents);
    }
}
