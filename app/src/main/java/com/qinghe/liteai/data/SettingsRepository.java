package com.qinghe.liteai.data;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsRepository {
    private static final String PREF_NAME = "liteai_settings";
    private static final String KEY_STREAM = "stream_output";
    private static final String KEY_DARK = "dark_mode";

    private final SharedPreferences preferences;

    public SettingsRepository(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean isStreamOutputEnabled() {
        return preferences.getBoolean(KEY_STREAM, true);
    }

    public void setStreamOutputEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_STREAM, enabled).apply();
    }

    public boolean isDarkModeEnabled() {
        return preferences.getBoolean(KEY_DARK, false);
    }

    public void setDarkModeEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_DARK, enabled).apply();
    }
}
