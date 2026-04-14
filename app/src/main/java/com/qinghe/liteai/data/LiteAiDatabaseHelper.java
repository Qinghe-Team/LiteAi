package com.qinghe.liteai.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class LiteAiDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "liteai.db";
    private static final int DATABASE_VERSION = 1;

    public LiteAiDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE conversations (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title TEXT NOT NULL," +
                "last_message_at INTEGER NOT NULL)");

        db.execSQL("CREATE TABLE messages (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "conversation_id INTEGER NOT NULL," +
                "role TEXT NOT NULL," +
                "content TEXT NOT NULL," +
                "created_at INTEGER NOT NULL)");

        db.execSQL("CREATE TABLE models (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "api_mode TEXT NOT NULL," +
                "api_url TEXT NOT NULL," +
                "api_key TEXT NOT NULL," +
                "model_code TEXT NOT NULL," +
                "is_active INTEGER NOT NULL DEFAULT 0)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS messages");
        db.execSQL("DROP TABLE IF EXISTS conversations");
        db.execSQL("DROP TABLE IF EXISTS models");
        onCreate(db);
    }
}
