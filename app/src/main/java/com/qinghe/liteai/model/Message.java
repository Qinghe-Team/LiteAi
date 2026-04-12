package com.qinghe.liteai.model;

public class Message {
    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";

    private long id;
    private long conversationId;
    private String role;
    private String content;
    private long createdAt;

    public Message(long id, long conversationId, String role, String content, long createdAt) {
        this.id = id;
        this.conversationId = conversationId;
        this.role = role;
        this.content = content;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public long getConversationId() {
        return conversationId;
    }

    public String getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
