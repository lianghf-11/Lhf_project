package com.Ai_Agent.ai_agent.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@ActiveProfiles({"local", "test", "pgvector"})
public class PgVectorVectorStoreConfigTest {

    @Resource
    private VectorStore pgVectorVectorStore;

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void test() {
        List<Document> documents = List.of(
                new Document(
                        "奶龙是一只可爱的小恐龙",
                        Map.of("meta1", "meta1")),
                new Document(
                        "奶龙喜欢和朋友们一起玩耍",
                        Map.of("meta2", "meta2")));
        pgVectorVectorStore.add(documents);
        List<Document> results = pgVectorVectorStore.similaritySearch(
                SearchRequest.builder().query("奶龙喜欢什么").topK(2).build());
        Assertions.assertNotNull(results);
        System.out.println("查询结果数量: " + results.size());
        results.forEach(doc -> System.out.println("  -> " + doc.getText()));
    }
}
