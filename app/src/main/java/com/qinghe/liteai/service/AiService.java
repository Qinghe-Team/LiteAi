package com.qinghe.liteai.service;

import com.qinghe.liteai.model.AiModelConfig;
import com.qinghe.liteai.model.Message;

import java.util.List;

public interface AiService {
    void requestReply(AiModelConfig model, List<Message> messages, boolean streamOutput, AiResponseCallback callback);
}
