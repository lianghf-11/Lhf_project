package com.Ai_Agent.ai_agent.controller;


import com.Ai_Agent.ai_agent.agent.model.Manus;
import com.Ai_Agent.ai_agent.app.LoveApp;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.awt.*;
import java.io.IOException;

@RestController
@RequestMapping("/ai")
public class Aicontroller {

    @Resource
    private LoveApp loveApp;

    @Resource
    public ToolCallback[] allTools;

    @Resource
    public ChatModel dashscopeChatModel;

    @GetMapping("/love_app/chat/sync")
    public String doChatwithLoveAppSync(String message,String chatId){
        return loveApp.doChat(message,chatId);
    }

    @GetMapping(value = "/love_app/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter doChatwithLoveAppSse(String message, String chatId) {
        SseEmitter emitter = new SseEmitter(180000L);
        loveApp.doChatWithToolsStream(message, chatId)
                .subscribe(
                        chunk -> {
                            try {
                                emitter.send(SseEmitter.event().data(chunk));
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        },
                        emitter::completeWithError,
                        emitter::complete
                );
        return emitter;
    }


    @GetMapping("/love_app/chat/sse/emitter")
    public SseEmitter doChatWithLoveAppSseEmitter(String message, String chatId) {
        // 创建一个超时时间较长的 SseEmitter
        SseEmitter emitter = new SseEmitter(180000L); // 3分钟超时
        // 获取 Flux 数据流并直接订阅
        loveApp.doChatStream(message, chatId)
                .subscribe(
                        // 处理每条消息
                        chunk -> {
                            try {
                                emitter.send(chunk);
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        },
                        // 处理错误
                        emitter::completeWithError,
                        // 处理完成
                        emitter::complete
                );
        // 返回emitter
        return emitter;
    }


    //流式调用智能体
    @GetMapping("/manus/chat")
    public  SseEmitter doChatWithManus(String message) {
        Manus manus = new Manus(allTools,dashscopeChatModel);
        return manus.runStream(message);
    }
}
