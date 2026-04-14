package com.qinghe.liteai.util;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class DateTimeUtilsTest {

    @Test
    public void buildConversationTitle_containsPrefix() {
        String title = DateTimeUtils.buildConversationTitle(0L);
        assertTrue(title.startsWith("新对话_"));
    }
}
