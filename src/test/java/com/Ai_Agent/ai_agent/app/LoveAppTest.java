package com.Ai_Agent.ai_agent.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles({"local", "test"})
class LoveAppTest {

    @Resource
    private LoveApp loveApp;

    @Resource
    private VectorStore loveAppVectorStore;

    @Test
    void checkVectorStore() {
        // 不传 filter → 看能搜到什么
        var all = loveAppVectorStore.similaritySearch(
                SearchRequest.builder().query("老婆吵架").topK(3).build());
        System.out.println("=== 无过滤检索结果 (" + all.size() + "条) ===");
        all.forEach(doc -> System.out.println("  status=" + doc.getMetadata().get("status")
                + " filename=" + doc.getMetadata().get("filename")
                + " text前50字=" + doc.getText().substring(0, Math.min(50, doc.getText().length()))));

        // 传 filter → 看已婚过滤是否生效
        var filtered = loveAppVectorStore.similaritySearch(
                SearchRequest.builder()
                        .query("老婆吵架")
                        .topK(3)
                        .filterExpression(new FilterExpressionBuilder().eq("status", "已婚").build())
                        .build());
        System.out.println("=== status=已婚 过滤检索结果 (" + filtered.size() + "条) ===");
        filtered.forEach(doc -> System.out.println("  status=" + doc.getMetadata().get("status")
                + " filename=" + doc.getMetadata().get("filename")));

        // 确认元数据
        System.out.println("=== 所有文档的status元数据 ===");
        // 没法直接列全部，用几个不同查询试试
        for (String s : List.of("恋爱", "单身", "已婚")) {
            var r = loveAppVectorStore.similaritySearch(
                    SearchRequest.builder().query("测试").topK(3)
                            .filterExpression(new FilterExpressionBuilder().eq("status", s).build())
                            .build());
            System.out.println("  status=" + s + " → " + r.size() + "条");
        }
    }

//    @Test
//    void doChat() {
//        String chatID = UUID.randomUUID().toString();
//        //第一轮
//        String message = "你好，我是奶龙";
//        String answer = loveApp.doChat(message, chatID);
//        System.out.println("【第一轮】" + answer);
//        Assertions.assertNotNull(answer);
//        //第二轮
//        message = "我怎么跟暴暴龙结婚";
//        answer = loveApp.doChat(message, chatID);
//        System.out.println("【第二轮】" + answer);
//        Assertions.assertNotNull(answer);
//        //第三轮
//        message = "我之前说想跟谁结婚？";
//        answer = loveApp.doChat(message, chatID);
//        System.out.println("【第三轮】" + answer);
//        Assertions.assertNotNull(answer);
//
//    }
//
//    @Test
//    void doChatWithReport() {
//        String chatID = UUID.randomUUID().toString();
//        String message = "你好，我是奶龙，我想跟暴暴龙搞基，我该怎么做";
//        LoveApp.LoveReport loveReport = loveApp.doChatWithReport(message, chatID);
//        Assertions.assertNotNull(loveReport);
//    }

    @Test
    void doChatWithRAG() {
        String chatID = UUID.randomUUID().toString();
        String message = "今天天气怎么样呀" ;
        String answer = loveApp.doChatWithRAG(message, chatID);
        System.out.println("【RAG回答】" + answer);
        Assertions.assertNotNull(answer);
    }

    @Test
    void doChatWithTools() {
        // 测试联网搜索问题的答案
        testMessage("周末想带女朋友去上海约会，推荐几个适合情侣的小众打卡地？");

        // 测试图片搜索
        testMessage("帮我搜索一些哄另一半开心的情侣图片");

        // 测试资源下载：图片下载
        testMessage("直接下载一张适合做手机壁纸的星空情侣图片为文件");

        // 测试 PDF 生成
//        testMessage("生成一份’七夕约会计划’PDF，包含餐厅预订、活动流程和礼物清单");
    }

    private void testMessage(String message) {
        String chatId = UUID.randomUUID().toString();
        String answer = loveApp.doChatWithTools(message, chatId);
        Assertions.assertNotNull(answer);
    }

}