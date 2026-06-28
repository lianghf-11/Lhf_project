package com.imagesearchmcpserver.tools;

import com.imagesearchmcpserver.tools.ImageSearchTool;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ImageSearchToolTest {

    private final ImageSearchTool imageSearchTool = new ImageSearchTool();

    @Test
    void searchImage() {
        String result = imageSearchTool.searchImage("computer");
        Assertions.assertNotNull(result);
    }

}