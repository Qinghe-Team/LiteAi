package com.qinghe.liteai.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class DateTimeUtils {
    private static final SimpleDateFormat CONVERSATION_NAME_FORMAT = new SimpleDateFormat("yyyyMMdd_HH:mm:ss", Locale.CHINA);
    private static final SimpleDateFormat DISPLAY_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA);

    private DateTimeUtils() {
    }

    public static String buildConversationTitle(long timestamp) {
        return "新对话_" + CONVERSATION_NAME_FORMAT.format(new Date(timestamp));
    }

    public static String formatDisplayTime(long timestamp) {
        return DISPLAY_FORMAT.format(new Date(timestamp));
    }
}
