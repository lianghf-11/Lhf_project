package com.Ai_Agent.ai_agent.rag;

import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class LoveAppVectorStoreconfig {
  @Resource
    private LoveAppDocumentation loveAppDocumentation;
  @Resource
  private MyKeywordEnricher myKeywordEnricher;

  @Bean
    VectorStore loveAppVectorStore(EmbeddingModel dashcopeEmbeddingModel) {
      SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(dashcopeEmbeddingModel)
              .build();
      //加载文档
      List<Document> documents = loveAppDocumentation.locateDocuments();

      //自动补充关键词元信息
      List<Document> enrichDocuments  = myKeywordEnricher.enrichDocuments(documents);

      simpleVectorStore.add(enrichDocuments);
      return simpleVectorStore;
  }
}
