package com.Ai_Agent.ai_agent.agent.model;

import com.Ai_Agent.ai_agent.advisor.MyLoggerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

@Component
public class Manus extends ToolCallAgent{

    public Manus(ToolCallback[] allTools , ChatModel dashscopeChatModel) {
        super(allTools);
        this.setName("Manus");

        String SYSTEM_PROMPT = """
                你是 YuManus，一个全能 AI 助手，用中文回答用户。
                你有以下工具可用：网页搜索、图片搜索、文件读写、PDF生成、资源下载、终端操作。
                规则：
                - 简单对话直接回答，不调工具
                - 用户要攻略/计划 → 搜索信息 → 生成PDF → 告知用户点击链接下载
                - 用户要图片/壁纸 → 搜索图片 → 下载 → 告诉用户可以查看
                - PDF和下载完成后，必须在回答中给出链接格式：[文件名](/api/files/pdf/xxx.pdf) 或 [图片](/api/files/download/xxx.jpg)
                - 普通对话不需要调工具，自然回复即可
                """;
        this.setSystemPrompt(SYSTEM_PROMPT);

        String NEXT_STEP_PROMPT = """
                判断用户需求：需要工具就调用，不需要就直接给出完整回答并调用TerminateTool结束。
                如果需要多步操作，逐步执行，最后汇总结果（要包含文件链接）再结束。
                """;
        this.setNextStepPrompt(NEXT_STEP_PROMPT);
        this.setMaxSteps(10);
        ChatClient chatClient =  ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
        this.setChatClient(chatClient);
    }
}
