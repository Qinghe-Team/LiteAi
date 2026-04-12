package com.qinghe.liteai.model;

public class AiModelConfig {
    private long id;
    private String name;
    private String apiMode;
    private String apiUrl;
    private String apiKey;
    private String modelCode;
    private boolean active;

    public AiModelConfig(long id, String name, String apiMode, String apiUrl, String apiKey, String modelCode, boolean active) {
        this.id = id;
        this.name = name;
        this.apiMode = apiMode;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.modelCode = modelCode;
        this.active = active;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getApiMode() {
        return apiMode;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getModelCode() {
        return modelCode;
    }

    public boolean isActive() {
        return active;
    }
}
