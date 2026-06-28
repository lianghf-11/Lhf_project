package com.Ai_Agent.ai_agent.rag;

import cn.hutool.core.lang.UUID;
import com.Ai_Agent.ai_agent.app.LoveApp;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("local")
class LoveAppDocumentationTest {

    @Autowired
    private LoveApp loveApp;

    @Test
    void doChatWithTools() {
        testMessage("周末想带女朋友去上海约会，推荐几个适合情侣的小众打卡地？");
        testMessage("最近和对象吵架了，看看编程导航网站（codefather.cn）的其他情侣是怎么解决矛盾的？");
        testMessage("直接下载一张适合做手机壁纸的星空情侣图片为文件");
        testMessage("执行 Python3 脚本来生成数据分析报告");
        testMessage("保存我的恋爱档案为文件");
        testMessage("生成一份'七夕约会计划'PDF，包含餐厅预订、活动流程和礼物清单");
    }

    private void testMessage(String message) {
        String chatId = UUID.randomUUID().toString();
        String answer = loveApp.doChatWithTools(message, chatId);
        assertNotNull(answer);
    }
}
