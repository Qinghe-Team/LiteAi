package com.qinghe.liteai.model;

public class Conversation {
    private long id;
    private String title;
    private long lastMessageAt;

    public Conversation(long id, String title, long lastMessageAt) {
        this.id = id;
        this.title = title;
        this.lastMessageAt = lastMessageAt;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(long lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }
}
