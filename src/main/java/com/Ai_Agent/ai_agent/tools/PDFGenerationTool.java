package com.Ai_Agent.ai_agent.tools;

import cn.hutool.core.io.FileUtil;
import com.Ai_Agent.ai_agent.constant.FileConstant;
import com.itextpdf.io.exceptions.IOException;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class PDFGenerationTool {

    @Tool(description = "Generate a PDF file with given content. Content must be full text, not empty.")
    public String generatePDF(
            @ToolParam(description = "File name, e.g. plan.pdf") String fileName,
            @ToolParam(description = "Full content for the PDF, at least 100 characters") String content) {
        if (content == null || content.length() < 10) {
            return "Error: PDF content too short or empty. Provide full plan text (at least 100 chars).";
        }
        String fileDir = FileConstant.FILE_SAVE_DIR + "/pdf";
        String filePath = fileDir + "/" + fileName;
        try {
            // 创建目录
            FileUtil.mkdir(fileDir);
            // 创建 PdfWriter 和 PdfDocument 对象
            try (PdfWriter writer = new PdfWriter(filePath);
                 PdfDocument pdf = new PdfDocument(writer);
                 Document document = new Document(pdf)) {
                // 自定义字体（需要人工下载字体文件到特定目录）
//                String fontPath = Paths.get("src/main/resources/static/fonts/simsun.ttf")
//                        .toAbsolutePath().toString();
//                PdfFont font = PdfFontFactory.createFont(fontPath,
//                        PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
                // 使用系统自带中文字体，兼容 Windows/Linux
                String fontPath = "C:\\Windows\\Fonts\\simsun.ttc,0";
                java.io.File fontFile = new java.io.File("C:\\Windows\\Fonts\\simsun.ttc");
                PdfFont font;
                if (fontFile.exists()) {
                    font = PdfFontFactory.createFont(fontPath,
                            PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
                } else {
                    font = PdfFontFactory.createFont("STSongStd-Light", "UniGB-UCS2-H");
                }
                document.setFont(font);
                // 创建段落
                Paragraph paragraph = new Paragraph(content);
                // 添加段落并关闭文档
                document.add(paragraph);
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
            String webUrl = FileConstant.toWebUrl(filePath);
            return "PDF generated! [点击查看](" + webUrl + ") 本地路径: " + filePath;
        } catch (IOException e) {
            return "Error generating PDF: " + e.getMessage();
        }
    }
}
