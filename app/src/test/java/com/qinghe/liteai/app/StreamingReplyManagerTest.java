package com.qinghe.liteai.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class StreamingReplyManagerTest {

    @Test
    public void pendingStateTracksStreamingContent() {
        PendingReplyStore store = new PendingReplyStore();

        store.start(1L, 100L);
        store.update(1L, "a", true);
        store.update(1L, "ab", true);

        StreamingReplyManager.PendingReplySnapshot snapshot = store.get(1L);
        assertNotNull(snapshot);
        assertEquals("ab", snapshot.getContent());
        assertEquals(true, snapshot.isStreaming());
    }

    @Test
    public void pendingStateIsRemovedAfterFinish() {
        PendingReplyStore store = new PendingReplyStore();

        store.start(2L, 200L);
        store.update(2L, "done", false);
        store.remove(2L);

        assertNull(store.get(2L));
    }

    static final class PendingReplyStore {
        private final java.util.Map<Long, StreamingReplyManager.PendingReplySnapshot> snapshots = new java.util.HashMap<>();

        void start(long conversationId, long createdAt) {
            snapshots.put(conversationId, new StreamingReplyManager.PendingReplySnapshot(conversationId, createdAt, "", true));
        }

        void update(long conversationId, String content, boolean streaming) {
            StreamingReplyManager.PendingReplySnapshot previous = snapshots.get(conversationId);
            long createdAt = previous == null ? 0L : previous.getCreatedAt();
            snapshots.put(conversationId, new StreamingReplyManager.PendingReplySnapshot(conversationId, createdAt, content, streaming));
        }

        StreamingReplyManager.PendingReplySnapshot get(long conversationId) {
            return snapshots.get(conversationId);
        }

        void remove(long conversationId) {
            snapshots.remove(conversationId);
        }
    }
}
