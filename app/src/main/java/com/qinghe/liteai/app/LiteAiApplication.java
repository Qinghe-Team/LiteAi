package com.qinghe.liteai.app;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.qinghe.liteai.data.SettingsRepository;

public class LiteAiApplication extends Application {
    private StreamingReplyManager streamingReplyManager;

    @Override
    public void onCreate() {
        super.onCreate();
        SettingsRepository settingsRepository = new SettingsRepository(this);
        AppCompatDelegate.setDefaultNightMode(settingsRepository.isDarkModeEnabled()
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO);
        streamingReplyManager = new StreamingReplyManager(this);
    }

    public StreamingReplyManager getStreamingReplyManager() {
        return streamingReplyManager;
    }
}
