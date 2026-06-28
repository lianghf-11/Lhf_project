package com.Ai_Agent.ai_agent.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.Ai_Agent.ai_agent.constant.FileConstant;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.File;

public class ResourceDownloadTool {

    @Tool(description = "Download a resource from a given URL. NOTE: only use URLs returned by WebSearchTool, never invent URLs yourself.")
    public String downloadResource(
            @ToolParam(description = "URL of the resource to download") String url,
            @ToolParam(description = "Name of the file to save") String fileName) {
        String fileDir = FileConstant.FILE_SAVE_DIR + "/download";
        String filePath = fileDir + "/" + fileName;
        FileUtil.mkdir(fileDir);
        try (HttpResponse response = HttpRequest.get(url)
                .timeout(15000)
                .execute()) {
            String contentType = response.header("Content-Type");
            if (contentType == null) {
                return "Download failed: no Content-Type header in response";
            }
            String cl = response.header("Content-Length");
            long contentLength = cl != null ? Long.parseLong(cl) : 0;
            if (contentLength > 50 * 1024 * 1024) {
                return "Download failed: file too large (max 50MB)";
            }
            // 拒绝 HTML 响应（通常是 404 页面或错误页）
            if (contentType.contains("text/html")) {
                return "Download failed: URL returned an HTML page, not a downloadable resource. URL: " + url;
            }
            byte[] bodyBytes = response.bodyBytes();
            FileUtil.writeBytes(bodyBytes, new File(filePath));
            String webUrl = com.Ai_Agent.ai_agent.constant.FileConstant.toWebUrl(filePath);
            return String.format("Downloaded: %s (type=%s, size=%d bytes) [查看](%s)",
                    fileName, contentType, bodyBytes.length, webUrl);
        } catch (Exception e) {
            return "Error downloading resource: " + e.getMessage();
        }
    }
}
