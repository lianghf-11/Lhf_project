package com.Ai_Agent.ai_agent.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class LoveAppDocumentation {

    private final ResourcePatternResolver resourcePatternResolver;

    public LoveAppDocumentation(ResourcePatternResolver resourcePatternResolver) {
        this.resourcePatternResolver = resourcePatternResolver;
    }

    public List<Document> locateDocuments() {
        List<Document> allDocuments = new ArrayList<>();
        try {
            Resource[] resources =
                    resourcePatternResolver.getResources("classpath:document/*.md");
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                String content = resource.getContentAsString(StandardCharsets.UTF_8);
                // 根据文件名自动提取状态标签
                String status = "通用";
                if (filename != null) {
                    if (filename.contains("单身")) {
                        status = "单身";
                    } else if (filename.contains("已婚")) {
                        status = "已婚";
                    } else if (filename.contains("恋爱")) {
                        status = "恋爱";
                    }
                }

                Document document = Document.builder()
                        .text(content)
                        .metadata("filename", filename != null ? filename : "unknown.md")
                        .metadata("风格","爱情")
                        .metadata("status", status)
                        .build();
                allDocuments.add(document);
            }
        } catch (IOException e) {
            log.error("Mark文档加载失败", e);
        }
        return allDocuments;
    }
}
