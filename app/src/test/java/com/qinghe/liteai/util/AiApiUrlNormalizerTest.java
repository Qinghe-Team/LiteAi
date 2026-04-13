package com.qinghe.liteai.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AiApiUrlNormalizerTest {

    @Test
    public void normalizeOpenAiBaseUrl_appendsV1WhenMissing() throws Exception {
        assertEquals("https://api.openai.com/v1", AiApiUrlNormalizer.normalizeOpenAiBaseUrl("https://api.openai.com"));
    }

    @Test
    public void normalizeOpenAiBaseUrl_stripsChatCompletionsSuffix() throws Exception {
        assertEquals("https://example.com/v1", AiApiUrlNormalizer.normalizeOpenAiBaseUrl("https://example.com/v1/chat/completions"));
    }

    @Test
    public void normalizeClaudeBaseUrl_stripsMessagesSuffix() throws Exception {
        assertEquals("https://example.com", AiApiUrlNormalizer.normalizeClaudeBaseUrl("https://example.com/v1/messages"));
    }

    @Test
    public void normalizeGeminiBaseUrl_stripsModelActionSuffix() throws Exception {
        assertEquals("https://generativelanguage.googleapis.com/v1beta",
                AiApiUrlNormalizer.normalizeGeminiBaseUrl("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-pro:generateContent?key=test"));
    }
}
