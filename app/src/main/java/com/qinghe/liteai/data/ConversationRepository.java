package com.qinghe.liteai.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.qinghe.liteai.model.Conversation;
import com.qinghe.liteai.model.Message;

import java.util.ArrayList;
import java.util.List;

public class ConversationRepository {
    private final LiteAiDatabaseHelper databaseHelper;

    public ConversationRepository(Context context) {
        databaseHelper = new LiteAiDatabaseHelper(context.getApplicationContext());
    }

    public Conversation createConversation(String title) {
        long now = System.currentTimeMillis();
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("last_message_at", now);
        long id = db.insert("conversations", null, values);
        return new Conversation(id, title, now);
    }

    public List<Conversation> getConversations() {
        ArrayList<Conversation> conversations = new ArrayList<>();
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        try (Cursor cursor = db.query("conversations", new String[]{"id", "title", "last_message_at"}, null, null, null, null, "last_message_at DESC, id DESC")) {
            while (cursor.moveToNext()) {
                conversations.add(new Conversation(
                        cursor.getLong(0),
                        cursor.getString(1),
                        cursor.getLong(2)
                ));
            }
        }
        return conversations;
    }

    public Conversation getConversation(long id) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        try (Cursor cursor = db.query("conversations", new String[]{"id", "title", "last_message_at"}, "id=?", new String[]{String.valueOf(id)}, null, null, null)) {
            if (cursor.moveToFirst()) {
                return new Conversation(cursor.getLong(0), cursor.getString(1), cursor.getLong(2));
            }
        }
        return null;
    }

    public void renameConversation(long id, String title) {
        ContentValues values = new ContentValues();
        values.put("title", title);
        databaseHelper.getWritableDatabase().update("conversations", values, "id=?", new String[]{String.valueOf(id)});
    }

    public void deleteConversation(long id) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.delete("messages", "conversation_id=?", new String[]{String.valueOf(id)});
        db.delete("conversations", "id=?", new String[]{String.valueOf(id)});
    }

    public List<Message> getMessages(long conversationId) {
        ArrayList<Message> messages = new ArrayList<>();
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        try (Cursor cursor = db.query("messages", new String[]{"id", "conversation_id", "role", "content", "created_at"}, "conversation_id=?", new String[]{String.valueOf(conversationId)}, null, null, "created_at ASC, id ASC")) {
            while (cursor.moveToNext()) {
                messages.add(new Message(
                        cursor.getLong(0),
                        cursor.getLong(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getLong(4)
                ));
            }
        }
        return messages;
    }

    public long addMessage(long conversationId, String role, String content) {
        long now = System.currentTimeMillis();
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues messageValues = new ContentValues();
        messageValues.put("conversation_id", conversationId);
        messageValues.put("role", role);
        messageValues.put("content", content);
        messageValues.put("created_at", now);
        long messageId = db.insert("messages", null, messageValues);

        ContentValues conversationValues = new ContentValues();
        conversationValues.put("last_message_at", now);
        db.update("conversations", conversationValues, "id=?", new String[]{String.valueOf(conversationId)});
        return messageId;
    }
}
