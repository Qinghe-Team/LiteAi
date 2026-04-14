package com.qinghe.liteai;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.qinghe.liteai.data.SettingsRepository;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        SettingsRepository settingsRepository = new SettingsRepository(this);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(view -> finish());

        MaterialSwitch streamSwitch = findViewById(R.id.switch_stream);
        MaterialSwitch darkSwitch = findViewById(R.id.switch_dark);
        findViewById(R.id.row_stream).setOnClickListener(view -> streamSwitch.toggle());
        findViewById(R.id.row_dark).setOnClickListener(view -> darkSwitch.toggle());

        streamSwitch.setChecked(settingsRepository.isStreamOutputEnabled());
        darkSwitch.setChecked(settingsRepository.isDarkModeEnabled());

        streamSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settingsRepository.setStreamOutputEnabled(isChecked);
            Toast.makeText(this, R.string.toast_settings_saved, Toast.LENGTH_SHORT).show();
        });

        darkSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settingsRepository.setDarkModeEnabled(isChecked);
            AppCompatDelegate.setDefaultNightMode(isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
            Toast.makeText(this, R.string.toast_settings_saved, Toast.LENGTH_SHORT).show();
        });
    }
}
