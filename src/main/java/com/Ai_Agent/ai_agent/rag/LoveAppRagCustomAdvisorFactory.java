package com.Ai_Agent.ai_agent.rag;

import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

/*
创建自定义的RAG检索增强顾问（检索 + 上下文增强）
 */
public class LoveAppRagCustomAdvisorFactory {

    public static Advisor createLoveAppRagCustomAdvisor(VectorStore vectorStore, String status) {
        // 1. 文档检索器：从向量库搜索 + status 过滤
        DocumentRetriever documentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .filterExpression(
                        new FilterExpressionBuilder().eq("status", status).build())
                .similarityThreshold(0.5)
                .topK(3)
                .build();

        // 2. 上下文增强器：加工检索到的文档（搜不到时走兜底话术）
        ContextualQueryAugmenter augmenter =
                LoveAppContextualQueryAugmenterFactory.createInstance();

        // 3. 组装成完整的 RAG 顾问
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever)
                .queryAugmenter(augmenter)
                .build();
    }
}
