package com.qinghe.liteai.util;

import static org.junit.Assert.assertEquals;

import com.qinghe.liteai.model.Message;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class ConversationPromptInjectorTest {

    @Test
    public void injectsPromptIntoFirstUserMessageOnly() {
        List<Message> result = ConversationPromptInjector.injectFirstUserPrompt(Arrays.asList(
                new Message(1L, 1L, Message.ROLE_USER, "你好", 1L),
                new Message(2L, 1L, Message.ROLE_ASSISTANT, "你好呀", 2L),
                new Message(3L, 1L, Message.ROLE_USER, "请推导一下", 3L)
        ));

        assertEquals("输出LaTeX请使用$$...$$的格式\n\n你好", result.get(0).getContent());
        assertEquals("请推导一下", result.get(2).getContent());
    }

    @Test
    public void doesNotInjectPromptTwice() {
        List<Message> result = ConversationPromptInjector.injectFirstUserPrompt(Arrays.asList(
                new Message(1L, 1L, Message.ROLE_USER, "输出LaTeX请使用$$...$$的格式\n\n你好", 1L),
                new Message(2L, 1L, Message.ROLE_ASSISTANT, "你好呀", 2L)
        ));

        assertEquals("输出LaTeX请使用$$...$$的格式\n\n你好", result.get(0).getContent());
    }
}
