package com.qinghe.liteai.app;

import android.content.Context;
import android.text.TextUtils;

import com.qinghe.liteai.data.ConversationRepository;
import com.qinghe.liteai.model.AiModelConfig;
import com.qinghe.liteai.model.Message;
import com.qinghe.liteai.service.AiResponseCallback;
import com.qinghe.liteai.service.AiService;
import com.qinghe.liteai.service.HttpAiService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class StreamingReplyManager {
    private final ConversationRepository conversationRepository;
    private final AiService aiService;
    private final Map<Long, PendingReplyState> pendingReplies = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();

    public StreamingReplyManager(Context context) {
        this(context, new HttpAiService());
    }

    StreamingReplyManager(Context context, AiService aiService) {
        this.conversationRepository = new ConversationRepository(context.getApplicationContext());
        this.aiService = aiService;
    }

    public void addListener(Listener listener) {
        if (listener != null) {
            listeners.addIfAbsent(listener);
        }
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public PendingReplySnapshot getPendingReply(long conversationId) {
        PendingReplyState state = pendingReplies.get(conversationId);
        return state == null ? null : state.snapshot();
    }

    public void requestReply(long conversationId, AiModelConfig model, List<Message> requestMessages, boolean streamOutput) {
        PendingReplyState state = new PendingReplyState(conversationId, System.currentTimeMillis());
        pendingReplies.put(conversationId, state);
        notifyReplyUpdated(state.snapshot());
        aiService.requestReply(model, requestMessages, streamOutput, new AiResponseCallback() {
            @Override
            public void onPartial(String partialContent) {
                PendingReplyState current = pendingReplies.get(conversationId);
                if (current == null) {
                    return;
                }
                if (current.isSameContent(partialContent) && current.streaming) {
                    return;
                }
                current.update(partialContent, true);
                notifyReplyUpdated(current.snapshot());
            }

            @Override
            public void onComplete(String fullContent) {
                finishReply(conversationId, fullContent);
            }

            @Override
            public void onError(String errorMessage) {
                finishReply(conversationId, errorMessage);
            }
        });
    }

    private void finishReply(long conversationId, String content) {
        PendingReplyState state = pendingReplies.remove(conversationId);
        if (state == null) {
            if (!TextUtils.isEmpty(content)) {
                conversationRepository.addMessage(conversationId, Message.ROLE_ASSISTANT, content);
            }
            notifyReplyFinished(conversationId);
            return;
        }
        state.update(content, false);
        if (!TextUtils.isEmpty(state.content)) {
            conversationRepository.addMessage(conversationId, Message.ROLE_ASSISTANT, state.content);
        }
        notifyReplyFinished(conversationId);
    }

    private void notifyReplyUpdated(PendingReplySnapshot snapshot) {
        for (Listener listener : listeners) {
            listener.onReplyUpdated(snapshot);
        }
    }

    private void notifyReplyFinished(long conversationId) {
        for (Listener listener : listeners) {
            listener.onReplyFinished(conversationId);
        }
    }

    public interface Listener {
        void onReplyUpdated(PendingReplySnapshot snapshot);

        void onReplyFinished(long conversationId);
    }

    public static final class PendingReplySnapshot {
        private final long conversationId;
        private final long createdAt;
        private final String content;
        private final boolean streaming;

        PendingReplySnapshot(long conversationId, long createdAt, String content, boolean streaming) {
            this.conversationId = conversationId;
            this.createdAt = createdAt;
            this.content = content;
            this.streaming = streaming;
        }

        public long getConversationId() {
            return conversationId;
        }

        public long getCreatedAt() {
            return createdAt;
        }

        public String getContent() {
            return content;
        }

        public boolean isStreaming() {
            return streaming;
        }
    }

    private static final class PendingReplyState {
        private final long conversationId;
        private final long createdAt;
        private volatile String content = "";
        private volatile boolean streaming = true;

        PendingReplyState(long conversationId, long createdAt) {
            this.conversationId = conversationId;
            this.createdAt = createdAt;
        }

        void update(String content, boolean streaming) {
            this.content = content == null ? "" : content;
            this.streaming = streaming;
        }

        PendingReplySnapshot snapshot() {
            return new PendingReplySnapshot(conversationId, createdAt, content, streaming);
        }

        boolean isSameContent(String value) {
            return content.equals(value == null ? "" : value);
        }
    }
}
