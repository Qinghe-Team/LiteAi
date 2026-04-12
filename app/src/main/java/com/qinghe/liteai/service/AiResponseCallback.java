package com.qinghe.liteai.service;

public interface AiResponseCallback {
    void onPartial(String partialContent);

    void onComplete(String fullContent);

    void onError(String errorMessage);
}
