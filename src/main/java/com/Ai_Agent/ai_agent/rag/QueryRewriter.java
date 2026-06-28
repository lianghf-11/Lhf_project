package com.Ai_Agent.ai_agent.rag;


import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.stereotype.Component;

/*
查询重写器
 */
@Component
public class QueryRewriter {

    private final QueryTransformer queryTransformer;

    private static final String CHINESE_REWRITE_TEMPLATE =
            "将用户查询重写为更适合在{target}中检索的形式。\n{query}";

    public QueryRewriter(ChatModel dashscopeChatModel) {
        ChatClient.Builder builder = ChatClient.builder(dashscopeChatModel);
        //创建查询重写转换器（使用中文提示词模板）
        queryTransformer = RewriteQueryTransformer.builder()
                .chatClientBuilder(builder)
                .promptTemplate(new PromptTemplate(CHINESE_REWRITE_TEMPLATE))
                .build();
    }

    public String doQueryRewriter(String prompt){
        Query query = new Query(prompt);
        //执行查询重写
        Query transformedQuery = queryTransformer.transform(query);
        //输出重写后的查询
        return transformedQuery.text();
    }
}
