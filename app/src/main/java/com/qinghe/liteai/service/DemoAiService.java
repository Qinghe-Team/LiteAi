package com.qinghe.liteai.service;

import android.os.Handler;
import android.os.Looper;

import com.qinghe.liteai.model.AiModelConfig;
import com.qinghe.liteai.model.Message;

import java.util.List;

public class DemoAiService implements AiService {
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void requestReply(AiModelConfig model, List<Message> messages, boolean streamOutput, AiResponseCallback callback) {
        String latestMessage = "";
        for (int index = messages.size() - 1; index >= 0; index--) {
            Message message = messages.get(index);
            if (Message.ROLE_USER.equals(message.getRole())) {
                latestMessage = message.getContent();
                break;
            }
        }
        String reply = buildReply(model, latestMessage);
        if (!streamOutput) {
            handler.post(() -> callback.onComplete(reply));
            return;
        }

        int step = 14;
        for (int index = step; index < reply.length(); index += step) {
            int finalIndex = index;
            handler.postDelayed(() -> callback.onPartial(reply.substring(0, finalIndex)), (long) finalIndex * 18L);
        }
        handler.postDelayed(() -> callback.onComplete(reply), Math.max(200L, (long) reply.length() * 18L));
    }

    private String buildReply(AiModelConfig model, String latestMessage) {
        String safeMessage = latestMessage == null || latestMessage.trim().isEmpty() ? "（空消息）" : latestMessage.trim();
        if (model == null) {
            return "### LiteAi\n当前还没有启用远端模型，下面是本地演示响应。\n\n- 已收到消息：`" + safeMessage + "`\n- 可前往“模型管理”添加并启用 OpenAI / Claude / Gemini 兼容模型\n\n公式示例：$E = mc^2$";
        }
        return "### " + model.getName() + "\n已启用模型代号：`" + model.getModelCode() + "`\n\n> API 模式：" + model.getApiMode() + "\n\n你刚刚发送：\n\n" + safeMessage + "\n\n当前工程已预留可扩展服务层，后续可在 `DemoAiService` 基础上替换真实网络实现。\n\n公式示例：$a^2+b^2=c^2$";
    }
}
