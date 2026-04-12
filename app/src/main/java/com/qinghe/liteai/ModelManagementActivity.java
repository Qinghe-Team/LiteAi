package com.qinghe.liteai;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.qinghe.liteai.data.ModelRepository;
import com.qinghe.liteai.model.AiModelConfig;
import com.qinghe.liteai.ui.adapter.ModelAdapter;

import java.util.ArrayList;
import java.util.List;

public class ModelManagementActivity extends AppCompatActivity {
    private ModelRepository modelRepository;
    private ModelAdapter adapter;
    private TextView emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_model_management);

        modelRepository = new ModelRepository(this);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(view -> finish());
        toolbar.setOnMenuItemClickListener(this::onMenuItemClick);

        ListView listView = findViewById(R.id.model_list);
        emptyView = findViewById(R.id.empty_view);
        adapter = new ModelAdapter(this, new ArrayList<>());
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            modelRepository.setActiveModel(id);
            Toast.makeText(this, R.string.toast_model_activated, Toast.LENGTH_SHORT).show();
            reload();
        });
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            showModelActions(adapter.getItem(position));
            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    private boolean onMenuItemClick(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_add_model) {
            showModelEditor(null);
            return true;
        }
        return false;
    }

    private void reload() {
        List<AiModelConfig> models = modelRepository.getModels();
        adapter.submit(models);
        emptyView.setVisibility(models.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showModelActions(AiModelConfig model) {
        String[] actions = {getString(R.string.dialog_edit), getString(R.string.dialog_delete)};
        new MaterialAlertDialogBuilder(this)
                .setTitle(model.getName())
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) {
                        showModelEditor(model);
                    } else {
                        modelRepository.deleteModel(model.getId());
                        Toast.makeText(this, R.string.toast_model_deleted, Toast.LENGTH_SHORT).show();
                        reload();
                    }
                })
                .show();
    }

    private void showModelEditor(AiModelConfig existing) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_model_edit, null, false);
        TextInputEditText nameInput = view.findViewById(R.id.input_model_name);
        MaterialAutoCompleteTextView modeInput = view.findViewById(R.id.input_model_mode);
        TextInputEditText urlInput = view.findViewById(R.id.input_model_url);
        TextInputEditText keyInput = view.findViewById(R.id.input_model_key);
        TextInputEditText codeInput = view.findViewById(R.id.input_model_code);

        String[] modes = getResources().getStringArray(R.array.api_modes);
        modeInput.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, modes));
        modeInput.setSimpleItems(modes);
        modeInput.setOnClickListener(v -> modeInput.showDropDown());
        modeInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                modeInput.showDropDown();
            }
        });
        modeInput.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                modeInput.showDropDown();
            }
            return false;
        });

        if (existing != null) {
            nameInput.setText(existing.getName());
            modeInput.setText(existing.getApiMode(), false);
            urlInput.setText(existing.getApiUrl());
            keyInput.setText(existing.getApiKey());
            codeInput.setText(existing.getModelCode());
        } else if (modes.length > 0) {
            modeInput.setText(modes[0], false);
        }

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(existing == null ? R.string.dialog_add_model_title : R.string.dialog_edit_model_title)
                .setView(view)
                .setPositiveButton(R.string.dialog_save, null)
                .setNegativeButton(R.string.dialog_cancel, null)
                .create();
        dialog.setOnShowListener(dialogInterface -> dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String name = textOf(nameInput);
                    String mode = textOf(modeInput);
                    String url = textOf(urlInput);
                    String key = textOf(keyInput);
                    String code = textOf(codeInput);
                    if (TextUtils.isEmpty(name) || TextUtils.isEmpty(mode) || TextUtils.isEmpty(url) || TextUtils.isEmpty(key) || TextUtils.isEmpty(code)) {
                        Toast.makeText(this, R.string.toast_model_fields_required, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    long id = existing == null ? 0L : existing.getId();
                    boolean active = existing != null && existing.isActive();
                    modelRepository.saveModel(new AiModelConfig(id, name, mode, url, key, code, active));
                    Toast.makeText(this, R.string.toast_model_saved, Toast.LENGTH_SHORT).show();
                    reload();
                    dialog.dismiss();
                }));
        dialog.show();
    }

    private String textOf(TextView textView) {
        return textView.getText() == null ? "" : textView.getText().toString().trim();
    }
}
