package com.qinghe.liteai;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.button.MaterialButton;
import com.qinghe.liteai.data.ConversationRepository;
import com.qinghe.liteai.data.ModelRepository;
import com.qinghe.liteai.data.SettingsRepository;
import com.qinghe.liteai.model.AiModelConfig;
import com.qinghe.liteai.model.Conversation;
import com.qinghe.liteai.model.FeatureOption;
import com.qinghe.liteai.model.Message;
import com.qinghe.liteai.service.AiResponseCallback;
import com.qinghe.liteai.service.AiService;
import com.qinghe.liteai.service.HttpAiService;
import com.qinghe.liteai.ui.adapter.FeatureAdapter;
import com.qinghe.liteai.util.DateTimeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_CONVERSATION_ID = "conversation_id";

    private ConversationRepository conversationRepository;
    private ModelRepository modelRepository;
    private SettingsRepository settingsRepository;
    private AiService aiService;

    private MaterialToolbar toolbar;
    private NestedScrollView messageScroll;
    private ViewGroup messageContainer;
    private EditText inputMessage;
    private long currentConversationId = -1L;
    private List<Message> currentMessages = new ArrayList<>();

    private ActivityResultLauncher<Intent> historyLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        conversationRepository = new ConversationRepository(this);
        modelRepository = new ModelRepository(this);
        settingsRepository = new SettingsRepository(this);
        aiService = new HttpAiService();

        toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_add);
        toolbar.setNavigationContentDescription(R.string.action_new_conversation);
        messageScroll = findViewById(R.id.message_scroll);
        messageContainer = findViewById(R.id.message_container);
        inputMessage = findViewById(R.id.input_message);
        MaterialButton sendButton = findViewById(R.id.button_send);

        toolbar.setNavigationOnClickListener(view -> createNewConversation());
        toolbar.setOnMenuItemClickListener(this::onToolbarMenuItemSelected);
        sendButton.setOnClickListener(view -> sendMessage());

        historyLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                long conversationId = result.getData().getLongExtra(EXTRA_CONVERSATION_ID, -1L);
                if (conversationId > 0L) {
                    loadConversation(conversationId);
                }
            }
        });

        List<Conversation> conversations = conversationRepository.getConversations();
        if (conversations.isEmpty()) {
            createNewConversation();
        } else {
            loadConversation(conversations.get(0).getId());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentConversationId > 0L) {
            loadConversation(currentConversationId);
        }
    }

    private boolean onToolbarMenuItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_more) {
            showMoreDialog();
            return true;
        }
        return false;
    }

    private void createNewConversation() {
        Conversation conversation = conversationRepository.createConversation(DateTimeUtils.buildConversationTitle(System.currentTimeMillis()));
        currentConversationId = conversation.getId();
        currentMessages = new ArrayList<>();
        toolbar.setTitle(conversation.getTitle());
        renderMessages();
    }

    private void loadConversation(long conversationId) {
        Conversation conversation = conversationRepository.getConversation(conversationId);
        if (conversation == null) {
            createNewConversation();
            return;
        }
        currentConversationId = conversationId;
        toolbar.setTitle(conversation.getTitle());
        currentMessages = new ArrayList<>(conversationRepository.getMessages(conversationId));
        renderMessages();
    }

    private void renderMessages() {
        messageContainer.removeAllViews();
        for (Message message : currentMessages) {
            messageContainer.addView(createMessageView(message));
        }
        messageScroll.post(() -> messageScroll.fullScroll(View.FOCUS_DOWN));
    }

    private View createMessageView(Message message) {
        View view = LayoutInflater.from(this).inflate(R.layout.view_message, messageContainer, false);
        FrameLayout row = view.findViewById(R.id.message_row);
        MaterialCardView cardView = view.findViewById(R.id.message_card);
        TextView messageContent = view.findViewById(R.id.message_content);
        TextView messageTime = view.findViewById(R.id.message_time);

        boolean isUser = Message.ROLE_USER.equals(message.getRole());
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) cardView.getLayoutParams();
        params.gravity = isUser ? Gravity.END : Gravity.START;
        cardView.setLayoutParams(params);
        cardView.setCardBackgroundColor(ContextCompat.getColor(this, resolveBubbleColor(isUser)));
        messageContent.setTextColor(ContextCompat.getColor(this, resolveTextColor(isUser)));
        messageContent.setText(message.getContent());
        messageTime.setTextColor(ContextCompat.getColor(this, resolveTextColor(isUser)));
        messageTime.setText(getString(R.string.label_message_time, DateTimeUtils.formatDisplayTime(message.getCreatedAt())));
        cardView.setOnLongClickListener(v -> {
            ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboardManager != null) {
                clipboardManager.setPrimaryClip(ClipData.newPlainText("message", message.getContent()));
                Toast.makeText(this, R.string.toast_copied, Toast.LENGTH_SHORT).show();
            }
            return true;
        });
        return row;
    }

    private int resolveBubbleColor(boolean isUser) {
        if (isUser) {
            return R.color.chat_user_bubble;
        }
        return isDarkMode() ? R.color.chat_assistant_bubble_dark : R.color.chat_assistant_bubble;
    }

    private int resolveTextColor(boolean isUser) {
        if (isUser) {
            return R.color.chat_user_text;
        }
        return isDarkMode() ? R.color.chat_assistant_text_dark : R.color.chat_assistant_text;
    }

    private boolean isDarkMode() {
        return (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    private void sendMessage() {
        String content = inputMessage.getText() == null ? "" : inputMessage.getText().toString().trim();
        if (TextUtils.isEmpty(content)) {
            Toast.makeText(this, R.string.toast_input_required, Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentConversationId <= 0L) {
            createNewConversation();
        }

        Message userMessage = new Message(conversationRepository.addMessage(currentConversationId, Message.ROLE_USER, content), currentConversationId, Message.ROLE_USER, content, System.currentTimeMillis());
        currentMessages.add(userMessage);
        inputMessage.setText(null);
        renderMessages();

        long conversationId = currentConversationId;
        AiModelConfig activeModel = modelRepository.getActiveModel();
        boolean streamEnabled = settingsRepository.isStreamOutputEnabled();
        List<Message> requestMessages = new ArrayList<>(currentMessages);
        Message pendingAssistant = new Message(-1L, conversationId, Message.ROLE_ASSISTANT, "", System.currentTimeMillis());
        currentMessages.add(pendingAssistant);
        renderMessages();

        aiService.requestReply(activeModel, requestMessages, streamEnabled, new AiResponseCallback() {
            @Override
            public void onPartial(String partialContent) {
                if (conversationId == currentConversationId) {
                    pendingAssistant.setContent(partialContent);
                    renderMessages();
                }
            }

            @Override
            public void onComplete(String fullContent) {
                conversationRepository.addMessage(conversationId, Message.ROLE_ASSISTANT, fullContent);
                if (conversationId == currentConversationId) {
                    loadConversation(conversationId);
                }
            }

            @Override
            public void onError(String errorMessage) {
                conversationRepository.addMessage(conversationId, Message.ROLE_ASSISTANT, errorMessage);
                if (conversationId == currentConversationId) {
                    loadConversation(conversationId);
                }
            }
        });
    }

    private void showMoreDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_feature_list, null, false);
        ListView listView = dialogView.findViewById(R.id.feature_list);
        List<FeatureOption> features = Arrays.asList(
                new FeatureOption(R.drawable.ic_history, getString(R.string.feature_history)),
                new FeatureOption(R.drawable.ic_models, getString(R.string.feature_models)),
                new FeatureOption(R.drawable.ic_settings, getString(R.string.feature_settings)),
                new FeatureOption(R.drawable.ic_info, getString(R.string.feature_about))
        );
        listView.setAdapter(new FeatureAdapter(this, features));
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_more_title)
                .setView(dialogView);
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        listView.setOnItemClickListener((parent, view, position, id) -> {
            dialog.dismiss();
            if (position == 0) {
                historyLauncher.launch(new Intent(this, HistoryActivity.class));
            } else if (position == 1) {
                startActivity(new Intent(this, ModelManagementActivity.class));
            } else if (position == 2) {
                startActivity(new Intent(this, SettingsActivity.class));
            } else if (position == 3) {
                startActivity(new Intent(this, AboutActivity.class));
            }
        });
        dialog.show();
    }
}
