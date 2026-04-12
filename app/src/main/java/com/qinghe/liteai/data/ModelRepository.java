package com.qinghe.liteai.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.qinghe.liteai.model.AiModelConfig;

import java.util.ArrayList;
import java.util.List;

public class ModelRepository {
    private final LiteAiDatabaseHelper databaseHelper;

    public ModelRepository(Context context) {
        databaseHelper = new LiteAiDatabaseHelper(context.getApplicationContext());
    }

    public List<AiModelConfig> getModels() {
        ArrayList<AiModelConfig> models = new ArrayList<>();
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        try (Cursor cursor = db.query("models", new String[]{"id", "name", "api_mode", "api_url", "api_key", "model_code", "is_active"}, null, null, null, null, "is_active DESC, id DESC")) {
            while (cursor.moveToNext()) {
                models.add(readModel(cursor));
            }
        }
        return models;
    }

    public AiModelConfig getActiveModel() {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        try (Cursor cursor = db.query("models", new String[]{"id", "name", "api_mode", "api_url", "api_key", "model_code", "is_active"}, "is_active=1", null, null, null, "id DESC", "1")) {
            if (cursor.moveToFirst()) {
                return readModel(cursor);
            }
        }
        return null;
    }

    public void saveModel(AiModelConfig model) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", model.getName());
        values.put("api_mode", model.getApiMode());
        values.put("api_url", model.getApiUrl());
        values.put("api_key", model.getApiKey());
        values.put("model_code", model.getModelCode());
        values.put("is_active", model.isActive() ? 1 : 0);

        if (model.isActive()) {
            ContentValues clear = new ContentValues();
            clear.put("is_active", 0);
            db.update("models", clear, null, null);
        }

        if (model.getId() > 0) {
            db.update("models", values, "id=?", new String[]{String.valueOf(model.getId())});
        } else {
            db.insert("models", null, values);
        }
    }

    public void deleteModel(long id) {
        databaseHelper.getWritableDatabase().delete("models", "id=?", new String[]{String.valueOf(id)});
    }

    public void setActiveModel(long id) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues clear = new ContentValues();
        clear.put("is_active", 0);
        db.update("models", clear, null, null);

        ContentValues set = new ContentValues();
        set.put("is_active", 1);
        db.update("models", set, "id=?", new String[]{String.valueOf(id)});
    }

    private AiModelConfig readModel(Cursor cursor) {
        return new AiModelConfig(
                cursor.getLong(0),
                cursor.getString(1),
                cursor.getString(2),
                cursor.getString(3),
                cursor.getString(4),
                cursor.getString(5),
                cursor.getInt(6) == 1
        );
    }
}
