package com.qinghe.liteai;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.qinghe.liteai.data.ModelRepository;
import com.qinghe.liteai.model.AiModelConfig;
import com.qinghe.liteai.ui.adapter.ModelAdapter;

import java.util.ArrayList;
import java.util.List;

public class ModelManagementActivity extends AppCompatActivity {
    private ModelRepository modelRepository;
    private ModelAdapter adapter;
    private TextView emptyView;
    private ActivityResultLauncher<Intent> modelEditorLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_model_management);

        modelRepository = new ModelRepository(this);
        modelEditorLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) {
                reload();
            }
        });
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(view -> finish());
        toolbar.setOnMenuItemClickListener(this::onMenuItemClick);

        ListView listView = findViewById(R.id.model_list);
        emptyView = findViewById(R.id.empty_view);
        adapter = new ModelAdapter(this, new ArrayList<>(), this::activateModel, this::launchModelEditor);
        listView.setAdapter(adapter);
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            showDeleteModelDialog(adapter.getItem(position));
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
            launchModelEditor(null);
            return true;
        }
        return false;
    }

    private void reload() {
        List<AiModelConfig> models = modelRepository.getModels();
        adapter.submit(models);
        emptyView.setVisibility(models.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showDeleteModelDialog(AiModelConfig model) {
        if (model == null) {
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(model.getName())
                .setMessage(getString(R.string.dialog_delete_model_message, model.getName()))
                .setPositiveButton(R.string.dialog_delete, (dialog, which) -> {
                    modelRepository.deleteModel(model.getId());
                    Toast.makeText(this, R.string.toast_model_deleted, Toast.LENGTH_SHORT).show();
                    reload();
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void activateModel(AiModelConfig model) {
        if (model == null || model.isActive()) {
            return;
        }
        modelRepository.setActiveModel(model.getId());
        Toast.makeText(this, R.string.toast_model_activated, Toast.LENGTH_SHORT).show();
        reload();
    }

    private void launchModelEditor(AiModelConfig model) {
        Intent intent = new Intent(this, ModelEditorActivity.class);
        if (model != null) {
            intent.putExtra(ModelEditorActivity.EXTRA_MODEL_ID, model.getId());
            intent.putExtra(ModelEditorActivity.EXTRA_MODEL_NAME, model.getName());
            intent.putExtra(ModelEditorActivity.EXTRA_MODEL_MODE, model.getApiMode());
            intent.putExtra(ModelEditorActivity.EXTRA_MODEL_URL, model.getApiUrl());
            intent.putExtra(ModelEditorActivity.EXTRA_MODEL_KEY, model.getApiKey());
            intent.putExtra(ModelEditorActivity.EXTRA_MODEL_CODE, model.getModelCode());
            intent.putExtra(ModelEditorActivity.EXTRA_MODEL_ACTIVE, model.isActive());
        }
        modelEditorLauncher.launch(intent);
    }
}
