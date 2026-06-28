package com.Ai_Agent.ai_agent.config;

import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class DeepSeekConfig {

    @Bean
    @Primary
    public DeepSeekChatModel deepSeekChatModel(@Value("${deepseek.api-key}") String apiKey) {
        DeepSeekApi api = DeepSeekApi.builder()
                .apiKey(apiKey)
                .build();
        return DeepSeekChatModel.builder()
                .deepSeekApi(api)
                .build();
    }
}
