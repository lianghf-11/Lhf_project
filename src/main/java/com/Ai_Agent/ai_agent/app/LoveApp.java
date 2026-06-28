package com.Ai_Agent.ai_agent.app;

import com.Ai_Agent.ai_agent.chatmemory.FileBasedChatMemory;
import com.Ai_Agent.ai_agent.rag.LoveAppRagCustomAdvisorFactory;
import com.Ai_Agent.ai_agent.rag.QueryRewriter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Objects;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import com.Ai_Agent.ai_agent.advisor.MyLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LoveApp {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    private static final String SYSTEM_PROMPT =
            "扮演深耕恋爱心理领域的专家。开场向用户表明身份，告知用户可倾诉恋爱难题。"
            + "围绕单身、恋爱、已婚三种状态提问：单身状态询问社交圈拓展及追求心仪对象的困扰；" +
            "恋爱状态询问沟通、习惯差异引发的矛盾；已婚状态询问家庭责任与亲属关系处理的问题。" +
            "引导用户详述事情经过、对方反应及自身想法，以便给出专属解决方案。";

    //初始化基于内存的对话记忆
    public LoveApp(ChatModel dashscopeChatModel) {
        //基于文件的对话记忆
        String fileDir = System.getProperty("user.dir")+"/tmp/chat-memory";
         chatMemory = new FileBasedChatMemory(fileDir);
        //初始化基于内存的对话记忆
//        this.chatMemory = MessageWindowChatMemory.builder()
//                .chatMemoryRepository(
//                        new InMemoryChatMemoryRepository()
//                )
//                .maxMessages(10)
//                .build();
        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                //自定义拦截器
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
    }
    /*
    AI基础对话功能
     */
    public String doChat(String message, String chatId) {
        MessageChatMemoryAdvisor advisor =
                (MessageChatMemoryAdvisor) MessageChatMemoryAdvisor.builder(chatMemory)
                .conversationId(chatId)
                .build();
        return chatClient
                .prompt()
                .user(message)
                .advisors(advisor)
                .call()
                .content();
    }

    /*
    AI基础对话功能(流式输出)
     */
    public reactor.core.publisher.Flux<String> doChatStream(String message, String chatId) {
        MessageChatMemoryAdvisor advisor =
                (MessageChatMemoryAdvisor) MessageChatMemoryAdvisor.builder(chatMemory)
                .conversationId(chatId)
                .build();
        return chatClient
                .prompt()
                .user(message)
                .advisors(advisor)
                .stream()
                .content();
    }

    record LoveReport(String title, List<String> suggestions) {}

    /*
    AI工具调用对话(流式输出)
     */
    public reactor.core.publisher.Flux<String> doChatWithToolsStream(String message, String chatId) {
        MessageChatMemoryAdvisor advisor =
                (MessageChatMemoryAdvisor) MessageChatMemoryAdvisor.builder(chatMemory)
                .conversationId(chatId)
                .build();
        return chatClient
                .prompt()
                .system("""
                        你是一个恋爱心理专家助手，拥有搜索、图片搜索、PDF生成、文件操作等工具。
                        重要规则：必须调用工具完成用户请求，不要只给文字建议。
                        用户要图片 → 调用 ImageSearchTool 搜索。
                        用户要PDF → 调用 PDFGenerationTool 生成。
                        用户要下载 → 调用 ResourceDownloadTool。
                        执行完毕后给出结果。""")
                .user(message)
                .advisors(advisor)
                .toolCallbacks(allTools)
                .stream()
                .content();
    }

    /*
    AI恋爱报告功能
     */
    public LoveReport doChatWithReport(String message, String chatId) {
        MessageChatMemoryAdvisor advisor = (MessageChatMemoryAdvisor) MessageChatMemoryAdvisor.builder(chatMemory)
                .conversationId(chatId)
                .build();
        return Objects.requireNonNull(chatClient
                        .prompt()
                        .system(SYSTEM_PROMPT +
                                "每次对话后都要生成恋爱结果，标题为 {用户名} 的恋爱报告，内容建议为列表")
                        .user(message)
                        .advisors(advisor)
                        .call()
                        .entity(LoveReport.class));

    }

    /*
    与RAG知识库进行问答
     */
    @Resource
    private VectorStore loveAppVectorStore;
    @Resource
    private Advisor loveAppRagCloudAdvisor;
    @Resource
    private QueryRewriter queryRewriter;

    /**
     * 根据用户问题，让 AI 判断用户的恋爱状态（单身/恋爱/已婚）
     */
    private String detectStatus(String message) {
        String result = chatClient.prompt()
                .system("""
                        根据用户的问题判断他/她处于哪种恋爱状态。
                        只回答以下三个词中的一个：单身、恋爱、已婚。
                        不要回答其他任何内容。""")
                .user(message)
                .call()
                .content();
        // 清洗 AI 回复，确保是有效值
        String status = result.trim();
        if (status.contains("单身")) return "单身";
        if (status.contains("恋爱")) return "恋爱";
        if (status.contains("已婚")) return "已婚";
        return "单身"; // 默认
    }

    public String doChatWithRAG(String message, String chatId) {
        // 1. 动态判断用户状态
        String status = detectStatus(message);
        // 2. 查询重写
        String rewrittenMessage = queryRewriter.doQueryRewriter(message);

        // 3. 用状态过滤知识库
        MessageChatMemoryAdvisor advisor = (MessageChatMemoryAdvisor) MessageChatMemoryAdvisor.builder(chatMemory)
                .conversationId(chatId)
                .build();
        return chatClient
                .prompt()
                .user(rewrittenMessage)
                .advisors(advisor)
                //本地Rag知识库,应用知识库问答
//                .advisors(QuestionAnswerAdvisor.builder(loveAppVectorStore).build())
                //云知识库，Rag检索增强服务
//                .advisors(loveAppRagCloudAdvisor)
                .advisors(
                        LoveAppRagCustomAdvisorFactory.createLoveAppRagCustomAdvisor(
                                loveAppVectorStore, status
                        )
                )
                .call()
                .content();
    }

    @Resource
    private ToolCallback[] allTools;

    public String doChatWithTools(String message, String chatId) {
        MessageChatMemoryAdvisor memoryAdvisor =
                (MessageChatMemoryAdvisor) MessageChatMemoryAdvisor.builder(chatMemory)
                .conversationId(chatId)
                .build();
        String content = chatClient
                .prompt()
                .system("""
                        你是一个助手，拥有搜索、下载、文件操作、图片搜索等工具。
                        重要规则：必须调用工具来完成用户的请求，不要只给建议或文字回复。
                        如果用户要下载图片，就调用 ResourceDownloadTool。
                        如果用户要搜索图片，就调用 ImageSearchTool。
                        如果用户要生成PDF，就调用 PDFGenerationTool。
                        任何可以执行的操作都必须实际执行，而不是告诉用户怎么做。""")
                .user(message)
                .advisors(memoryAdvisor)
                .advisors(new MyLoggerAdvisor())
                .toolCallbacks(allTools)
                .call()
                .content();
        log.info("content: {}", content);
        return content;
    }
}
