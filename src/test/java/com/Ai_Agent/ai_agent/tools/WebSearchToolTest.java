package com.Ai_Agent.ai_agent.tools;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("local")
class WebSearchToolTest {

    @Autowired
    private WebSearchTool webSearchTool;

    @Test
    void testSearchWeb() {
        String result = webSearchTool.searchWeb("今天天气怎么样");
        System.out.println("===== 搜索结果 =====");
        System.out.println(result);
        System.out.println("====================");
    }
}
