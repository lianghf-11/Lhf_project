package com.Ai_Agent.ai_agent.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class WebScrapingToolTest {

    @Test
    void testScrapeWebPage() {
        WebScrapingTool tool = new WebScrapingTool();
        String result = tool.scrapeWebPage("https://www.codefather.cn");
        assertNotNull(result);
        System.out.println("===== 抓取结果 =====");
        System.out.println(result.substring(0, Math.min(result.length(), 200)));
        System.out.println("====================");
    }
}
