package com.Ai_Agent.ai_agent.tools;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class WebScrapingTool {

    @Tool(description = "Scrape the text content of a web page")
    public String scrapeWebPage(@ToolParam(description = "URL of the web page to scrape") String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .timeout(10000)
                    .get();
            return doc.body().text();
        } catch (IOException e) {
            return "Error scraping web page: " + e.getMessage();
        }
    }
}
