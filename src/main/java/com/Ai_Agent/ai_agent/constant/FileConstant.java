package com.Ai_Agent.ai_agent.constant;

import java.io.File;

/*
文件常量
 */
public interface FileConstant {
    String FILE_SAVE_DIR = System.getProperty("user.dir") + "/tmp";
    String FILE_URL_PREFIX = "/api/files";

    static String toWebUrl(String localPath) {
        if (localPath == null) return null;
        String relative = localPath.replace("\\", "/")
                .replace(FILE_SAVE_DIR.replace("\\", "/"), "");
        return FILE_URL_PREFIX + relative;
    }
}
