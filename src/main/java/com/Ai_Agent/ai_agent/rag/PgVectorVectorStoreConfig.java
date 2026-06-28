package com.Ai_Agent.ai_agent.rag;


import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType.COSINE_DISTANCE;
import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType.HNSW;

@Profile("pgvector")
@Configuration
public class PgVectorVectorStoreConfig {

    @Bean
    public VectorStore pgVectorVectorStore
            (JdbcTemplate jdbcTemplate, EmbeddingModel dashscopeEmbeddingModel) {
        PgVectorStore vectorStore = PgVectorStore.builder(jdbcTemplate, dashscopeEmbeddingModel)
                .dimensions(1024)                  // Optional: defaults to model dimensions or
                .distanceType(COSINE_DISTANCE)     // Optional: defaults to COSINE_DISTANCE
                .indexType(HNSW)                   // Optional: defaults to HNSW
                .initializeSchema(true)             // Optional: defaults to false
                .schemaName("public")              // Optional: defaults to "public"
                .vectorTableName("vector_store_1024")   // 旧表1536维度已不兼容，换新表名自动重建
                .maxDocumentBatchSize(10000)       // Optional: defaults to 10000
                .build();
        return vectorStore;
    }
}
