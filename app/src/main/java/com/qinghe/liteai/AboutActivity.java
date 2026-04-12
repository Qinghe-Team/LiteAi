package com.qinghe.liteai;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

public class AboutActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(view -> finish());

        ((TextView) findViewById(R.id.text_name)).setText(getString(R.string.app_name));
        ((TextView) findViewById(R.id.text_version)).setText(getString(R.string.about_version, BuildConfig.VERSION_NAME));
        TextView repoText = findViewById(R.id.text_repo);
        repoText.setOnClickListener(view -> openRepo());
    }

    private void openRepo() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Qinghe-Team/LiteAi")));
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(this, R.string.toast_repo_unavailable, Toast.LENGTH_SHORT).show();
        }
    }
}
