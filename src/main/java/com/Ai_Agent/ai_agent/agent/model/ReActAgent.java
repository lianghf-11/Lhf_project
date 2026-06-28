package com.Ai_Agent.ai_agent.agent.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * ReAct (Reasoning and Acting) 模式的代理抽象类
 * 实现了思考-行动的循环模式
 */
@EqualsAndHashCode(callSuper = true)
@Data
public abstract class ReActAgent extends BaseAgent {

    public abstract boolean think();

    public abstract String act();

    @Override
    public String step() {
        try {
            boolean shouldAct = think();
            if (!shouldAct) {
                setState(AgentState.FINISHED);
                List<Message> messages = getMessageList();
                if (!messages.isEmpty()) {
                    Message last = messages.get(messages.size() - 1);
                    if (last instanceof AssistantMessage assistant) {
                        return "[AI回复] " + assistant.getText();
                    }
                }
                return "[AI回复] 思考完成";
            }
            return "[工具] " + act();
        } catch (Exception e) {
            e.printStackTrace();
            return "步骤执行失败: " + e.getMessage();
        }
    }
}
