package com.Ai_Agent.ai_agent.tools;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class WebSearchTool {

    private static final String IQS_URL = "https://cloud-iqs.aliyuncs.com/search/unified";

    private final String apiKey;

    public WebSearchTool(@Value("${search-api.api-key}") String apiKey) {
        this.apiKey = apiKey;
    }

    @Tool(description = "Search information from internet via Alibaba Cloud IQS")
    public String searchWeb(
            @ToolParam(description = "Search query keyword") String query) {
        JSONObject body = new JSONObject();
        body.set("query", query);
        body.set("engineType", "Generic");
        JSONObject advancedParams = new JSONObject();
        advancedParams.set("numResults", 5);
        body.set("advancedParams", advancedParams);

        try (HttpResponse response = HttpRequest.post(IQS_URL)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(body.toString())
                .execute()) {
            JSONObject result = JSONUtil.parseObj(response.body());
            JSONArray pageItems = result.getJSONArray("pageItems");
            if (pageItems == null || pageItems.isEmpty()) {
                return "No search results found for: " + query;
            }
            return pageItems.stream()
                    .map(obj -> {
                        JSONObject item = (JSONObject) obj;
                        JSONObject out = new JSONObject();
                        out.set("title", item.getStr("title"));
                        out.set("link", item.getStr("link"));
                        out.set("snippet", item.getStr("snippet"));
                        return out.toString();
                    })
                    .reduce((a, b) -> a + "\n---\n" + b)
                    .orElse("No results");
        } catch (Exception e) {
            return "Search error: " + e.getMessage();
        }
    }
}
