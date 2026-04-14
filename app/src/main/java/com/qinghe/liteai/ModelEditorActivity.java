package com.qinghe.liteai;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.qinghe.liteai.data.ModelRepository;
import com.qinghe.liteai.model.AiModelConfig;

public class ModelEditorActivity extends AppCompatActivity {
    public static final String EXTRA_MODEL_ID = "model_id";
    public static final String EXTRA_MODEL_NAME = "model_name";
    public static final String EXTRA_MODEL_MODE = "model_mode";
    public static final String EXTRA_MODEL_URL = "model_url";
    public static final String EXTRA_MODEL_KEY = "model_key";
    public static final String EXTRA_MODEL_CODE = "model_code";
    public static final String EXTRA_MODEL_ACTIVE = "model_active";

    private ModelRepository modelRepository;
    private TextInputEditText nameInput;
    private MaterialAutoCompleteTextView modeInput;
    private TextInputEditText urlInput;
    private TextInputEditText keyInput;
    private TextInputEditText codeInput;
    private long modelId;
    private boolean active;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_model_editor);

        modelRepository = new ModelRepository(this);
        modelId = getIntent().getLongExtra(EXTRA_MODEL_ID, 0L);
        active = getIntent().getBooleanExtra(EXTRA_MODEL_ACTIVE, false);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(modelId > 0L ? R.string.dialog_edit_model_title : R.string.dialog_add_model_title);
        toolbar.setNavigationOnClickListener(view -> finish());

        nameInput = findViewById(R.id.input_model_name);
        modeInput = findViewById(R.id.input_model_mode);
        urlInput = findViewById(R.id.input_model_url);
        keyInput = findViewById(R.id.input_model_key);
        codeInput = findViewById(R.id.input_model_code);
        MaterialButton saveButton = findViewById(R.id.button_save_model);

        configureModeInput();
        bindExistingModel();
        saveButton.setOnClickListener(view -> saveModel());
    }

    private void configureModeInput() {
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
        if (modelId <= 0L && modes.length > 0) {
            modeInput.setText(modes[0], false);
        }
    }

    private void bindExistingModel() {
        nameInput.setText(getIntent().getStringExtra(EXTRA_MODEL_NAME));
        String mode = getIntent().getStringExtra(EXTRA_MODEL_MODE);
        if (!TextUtils.isEmpty(mode)) {
            modeInput.setText(mode, false);
        }
        urlInput.setText(getIntent().getStringExtra(EXTRA_MODEL_URL));
        keyInput.setText(getIntent().getStringExtra(EXTRA_MODEL_KEY));
        codeInput.setText(getIntent().getStringExtra(EXTRA_MODEL_CODE));
    }

    private void saveModel() {
        String name = textOf(nameInput);
        String mode = textOf(modeInput);
        String url = textOf(urlInput);
        String key = textOf(keyInput);
        String code = textOf(codeInput);
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(mode) || TextUtils.isEmpty(url) || TextUtils.isEmpty(key) || TextUtils.isEmpty(code)) {
            Toast.makeText(this, R.string.toast_model_fields_required, Toast.LENGTH_SHORT).show();
            return;
        }
        modelRepository.saveModel(new AiModelConfig(modelId, name, mode, url, key, code, active));
        Toast.makeText(this, R.string.toast_model_saved, Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    private String textOf(TextView textView) {
        return textView.getText() == null ? "" : textView.getText().toString().trim();
    }
}
