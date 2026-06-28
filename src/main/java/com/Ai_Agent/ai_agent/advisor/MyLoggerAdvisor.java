package com.Ai_Agent.ai_agent.advisor;

import java.util.function.Function;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;


//自定义日志
@Slf4j
public class MyLoggerAdvisor implements CallAdvisor, StreamAdvisor {

    private final Function<ChatClientRequest, String> requestToString;
    private final Function<ChatResponse, String> responseToString;
    @Getter
    private final int order;

    public MyLoggerAdvisor() {
        this(SimpleLoggerAdvisor.DEFAULT_REQUEST_TO_STRING,
                SimpleLoggerAdvisor.DEFAULT_RESPONSE_TO_STRING, 0);
    }

    public MyLoggerAdvisor(Function<ChatClientRequest, String> requestToString,
                           Function<ChatResponse, String> responseToString,
                           int order) {
        this.requestToString = requestToString;
        this.responseToString = responseToString;
        this.order = order;
    }

    @NotNull
    public String getName() {
        return this.getClass().getSimpleName();
    }

    protected void logRequest(ChatClientRequest chatClientRequest) {
        log.info("【请求】{}", requestToString.apply(chatClientRequest));
    }

    protected void logResponse(ChatClientResponse chatClientResponse) {
        log.info("【响应】{}", responseToString.apply(chatClientResponse.chatResponse()));
    }

    @NotNull
    public ChatClientResponse adviseCall(
            @NotNull ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        this.logRequest(chatClientRequest);
        ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(chatClientRequest);
        this.logResponse(chatClientResponse);
        return chatClientResponse;
    }

    @NotNull
    public Flux<ChatClientResponse> adviseStream(
            @NotNull ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        this.logRequest(chatClientRequest);
        Flux<ChatClientResponse> chatClientResponses = streamAdvisorChain.nextStream(chatClientRequest);
        return (new ChatClientMessageAggregator())
                .aggregateChatClientResponse(chatClientResponses, this::logResponse);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Function<ChatClientRequest, String> requestToString =
                SimpleLoggerAdvisor.DEFAULT_REQUEST_TO_STRING;
        private Function<ChatResponse, String> responseToString =
                SimpleLoggerAdvisor.DEFAULT_RESPONSE_TO_STRING;
        private int order = 0;

        private Builder() {
        }

        public Builder requestToString(Function<ChatClientRequest, String> requestToString) {
            this.requestToString = requestToString;
            return this;
        }

        public Builder responseToString(Function<ChatResponse, String> responseToString) {
            this.responseToString = responseToString;
            return this;
        }

        public Builder order(int order) {
            this.order = order;
            return this;
        }

        public MyLoggerAdvisor build() {
            return new MyLoggerAdvisor(this.requestToString, this.responseToString, this.order);
        }
    }
}
