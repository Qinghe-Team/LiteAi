package com.qinghe.liteai.util;

import com.qinghe.liteai.model.Message;

import java.util.ArrayList;
import java.util.List;

public final class ConversationPromptInjector {
    public static final String FIRST_MESSAGE_SYSTEM_PROMPT = "输出LaTeX请使用$$...$$的格式";

    private ConversationPromptInjector() {
    }

    public static List<Message> injectFirstUserPrompt(List<Message> messages) {
        ArrayList<Message> result = new ArrayList<>(messages.size());
        boolean handledFirstUserMessage = false;
        for (Message message : messages) {
            if (!handledFirstUserMessage && Message.ROLE_USER.equals(message.getRole())) {
                handledFirstUserMessage = true;
                result.add(copyMessage(message, prependPrompt(message.getContent())));
                continue;
            }
            result.add(message);
        }
        return result;
    }

    static String prependPrompt(String content) {
        if (content == null || content.isEmpty() || content.startsWith(FIRST_MESSAGE_SYSTEM_PROMPT)) {
            return content;
        }
        return FIRST_MESSAGE_SYSTEM_PROMPT + "\n\n" + content;
    }

    private static Message copyMessage(Message source, String content) {
        return new Message(
                source.getId(),
                source.getConversationId(),
                source.getRole(),
                content,
                source.getCreatedAt()
        );
    }
}
