package com.qinghe.liteai;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.qinghe.liteai.data.ConversationRepository;
import com.qinghe.liteai.model.Conversation;
import com.qinghe.liteai.ui.adapter.ConversationAdapter;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {
    private ConversationRepository conversationRepository;
    private ConversationAdapter adapter;
    private TextView emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        conversationRepository = new ConversationRepository(this);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(view -> finish());

        ListView listView = findViewById(R.id.history_list);
        emptyView = findViewById(R.id.empty_view);
        adapter = new ConversationAdapter(this, new ArrayList<>());
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            Intent result = new Intent();
            result.putExtra(MainActivity.EXTRA_CONVERSATION_ID, id);
            setResult(RESULT_OK, result);
            finish();
        });
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            Conversation conversation = adapter.getItem(position);
            showConversationActions(conversation);
            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        List<Conversation> conversations = conversationRepository.getConversations();
        adapter.submit(conversations);
        emptyView.setVisibility(conversations.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showConversationActions(Conversation conversation) {
        String[] actions = {getString(R.string.dialog_edit), getString(R.string.dialog_delete)};
        new MaterialAlertDialogBuilder(this)
                .setTitle(conversation.getTitle())
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) {
                        showRenameDialog(conversation);
                    } else {
                        conversationRepository.deleteConversation(conversation.getId());
                        Toast.makeText(this, R.string.toast_conversation_deleted, Toast.LENGTH_SHORT).show();
                        onResume();
                    }
                })
                .show();
    }

    private void showRenameDialog(Conversation conversation) {
        EditText editText = new EditText(this);
        editText.setText(conversation.getTitle());
        editText.setSelection(editText.getText().length());
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_edit)
                .setView(editText)
                .setPositiveButton(R.string.dialog_save, (dialog, which) -> {
                    String title = editText.getText() == null ? "" : editText.getText().toString().trim();
                    if (TextUtils.isEmpty(title)) {
                        Toast.makeText(this, R.string.toast_title_required, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    conversationRepository.renameConversation(conversation.getId(), title);
                    onResume();
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }
}
