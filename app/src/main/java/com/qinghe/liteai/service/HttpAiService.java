package com.qinghe.liteai.service;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.http.StreamResponse;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.RawContentBlockDelta;
import com.anthropic.models.messages.RawMessageStreamEvent;
import com.google.genai.Client;
import com.google.genai.ResponseStream;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.Part;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.qinghe.liteai.model.AiModelConfig;
import com.qinghe.liteai.model.Message;
import com.qinghe.liteai.util.AiApiUrlNormalizer;
import com.qinghe.liteai.util.ConversationPromptInjector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpAiService implements AiService {
    private static final String MODE_OPENAI = "OpenAI";
    private static final String MODE_CLAUDE = "Claude";
    private static final String MODE_GEMINI = "Gemini";
    private static final int DEFAULT_MAX_TOKENS = 2048;

    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    public void requestReply(AiModelConfig model, List<Message> messages, boolean streamOutput, AiResponseCallback callback) {
        executorService.execute(() -> {
            if (model == null) {
                postError(callback, "请先在“模型管理”中添加并启用一个模型。");
                return;
            }

            List<Message> requestMessages = ConversationPromptInjector.injectFirstUserPrompt(sanitizeMessages(messages));
            if (requestMessages.isEmpty()) {
                postError(callback, "没有可发送的对话内容。");
                return;
            }

            try {
                validateModel(model);
                String mode = safeValue(model.getApiMode());
                if (mode.contains(MODE_OPENAI)) {
                    requestOpenAi(model, requestMessages, streamOutput, callback);
                    return;
                }
                if (mode.contains(MODE_CLAUDE)) {
                    requestClaude(model, requestMessages, streamOutput, callback);
                    return;
                }
                if (mode.contains(MODE_GEMINI)) {
                    requestGemini(model, requestMessages, streamOutput, callback);
                    return;
                }
                postError(callback, "暂不支持当前 API 模式：" + mode);
            } catch (Exception exception) {
                postError(callback, buildErrorMessage(exception));
            }
        });
    }

    private void requestOpenAi(AiModelConfig model, List<Message> messages, boolean streamOutput, AiResponseCallback callback) throws Exception {
        OpenAIClient client = OpenAIOkHttpClient.builder()
                .apiKey(safeValue(model.getApiKey()))
                .baseUrl(AiApiUrlNormalizer.normalizeOpenAiBaseUrl(model.getApiUrl()))
                .build();
        try {
            ChatCompletionCreateParams params = buildOpenAiParams(model, messages);
            if (!streamOutput) {
                ChatCompletion completion = client.chat().completions().create(params);
                postComplete(callback, extractOpenAiText(completion));
                return;
            }

            StringBuilder builder = new StringBuilder();
            try (com.openai.core.http.StreamResponse<ChatCompletionChunk> streamResponse = client.chat().completions().createStreaming(params)) {
                streamResponse.stream().forEach(chunk -> {
                    String delta = extractOpenAiChunkText(chunk);
                    if (!TextUtils.isEmpty(delta)) {
                        builder.append(delta);
                        postPartial(callback, builder.toString());
                    }
                });
            }
            postComplete(callback, builder.toString());
        } finally {
            client.close();
        }
    }

    private void requestClaude(AiModelConfig model, List<Message> messages, boolean streamOutput, AiResponseCallback callback) throws Exception {
        AnthropicClient client = AnthropicOkHttpClient.builder()
                .apiKey(safeValue(model.getApiKey()))
                .baseUrl(AiApiUrlNormalizer.normalizeClaudeBaseUrl(model.getApiUrl()))
                .build();
        try {
            MessageCreateParams params = buildClaudeParams(model, messages);
            if (!streamOutput) {
                com.anthropic.models.messages.Message response = client.messages().create(params);
                postComplete(callback, extractClaudeText(response));
                return;
            }

            StringBuilder builder = new StringBuilder();
            try (StreamResponse<RawMessageStreamEvent> streamResponse = client.messages().createStreaming(params)) {
                streamResponse.stream().forEach(event -> {
                    String delta = extractClaudeDelta(event);
                    if (!TextUtils.isEmpty(delta)) {
                        builder.append(delta);
                        postPartial(callback, builder.toString());
                    }
                });
            }
            postComplete(callback, builder.toString());
        } finally {
            client.close();
        }
    }

    private void requestGemini(AiModelConfig model, List<Message> messages, boolean streamOutput, AiResponseCallback callback) throws Exception {
        HttpOptions httpOptions = HttpOptions.builder()
                .baseUrl(AiApiUrlNormalizer.normalizeGeminiBaseUrl(model.getApiUrl()))
                .build();
        try (Client client = Client.builder()
                .apiKey(safeValue(model.getApiKey()))
                .httpOptions(httpOptions)
                .build()) {
            GenerateContentConfig config = GenerateContentConfig.builder()
                    .candidateCount(1)
                    .maxOutputTokens(DEFAULT_MAX_TOKENS)
                    .build();
            List<Content> contents = buildGeminiContents(messages);
            String modelCode = safeValue(model.getModelCode());
            if (!streamOutput) {
                GenerateContentResponse response = client.models.generateContent(modelCode, contents, config);
                postComplete(callback, response == null ? "" : response.text());
                return;
            }

            StringBuilder builder = new StringBuilder();
            try (ResponseStream<GenerateContentResponse> streamResponse = client.models.generateContentStream(modelCode, contents, config)) {
                for (GenerateContentResponse response : streamResponse) {
                    String delta = response == null ? "" : response.text();
                    if (!TextUtils.isEmpty(delta)) {
                        builder.append(delta);
                        postPartial(callback, builder.toString());
                    }
                }
            }
            postComplete(callback, builder.toString());
        }
    }

    private ChatCompletionCreateParams buildOpenAiParams(AiModelConfig model, List<Message> messages) {
        ChatCompletionCreateParams.Builder builder = ChatCompletionCreateParams.builder()
                .model(safeValue(model.getModelCode()))
                .maxTokens(DEFAULT_MAX_TOKENS);
        for (Message message : messages) {
            if (Message.ROLE_ASSISTANT.equals(message.getRole())) {
                builder.addAssistantMessage(safeValue(message.getContent()));
            } else {
                builder.addUserMessage(safeValue(message.getContent()));
            }
        }
        return builder.build();
    }

    private MessageCreateParams buildClaudeParams(AiModelConfig model, List<Message> messages) {
        MessageCreateParams.Builder builder = MessageCreateParams.builder()
                .model(safeValue(model.getModelCode()))
                .maxTokens(DEFAULT_MAX_TOKENS);
        for (Message message : messages) {
            if (Message.ROLE_ASSISTANT.equals(message.getRole())) {
                builder.addAssistantMessage(safeValue(message.getContent()));
            } else {
                builder.addUserMessage(safeValue(message.getContent()));
            }
        }
        return builder.build();
    }

    private List<Content> buildGeminiContents(List<Message> messages) {
        ArrayList<Content> contents = new ArrayList<>(messages.size());
        for (Message message : messages) {
            contents.add(Content.builder()
                    .role(Message.ROLE_ASSISTANT.equals(message.getRole()) ? "model" : "user")
                    .parts(Part.builder().text(safeValue(message.getContent())).build())
                    .build());
        }
        return contents;
    }

    private void validateModel(AiModelConfig model) {
        if (TextUtils.isEmpty(safeValue(model.getApiUrl()))) {
            throw new IllegalArgumentException("API 地址不能为空。");
        }
        if (TextUtils.isEmpty(safeValue(model.getApiKey()))) {
            throw new IllegalArgumentException("API Key 不能为空。");
        }
        if (TextUtils.isEmpty(safeValue(model.getModelCode()))) {
            throw new IllegalArgumentException("模型代号不能为空。");
        }
    }

    private String extractOpenAiText(ChatCompletion completion) {
        if (completion == null || completion.choices() == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (ChatCompletion.Choice choice : completion.choices()) {
            if (choice == null || choice.message() == null) {
                continue;
            }
            String text = choice.message().content().orElse(choice.message().refusal().orElse(""));
            if (!TextUtils.isEmpty(text)) {
                builder.append(text);
            }
        }
        return builder.toString();
    }

    private String extractOpenAiChunkText(ChatCompletionChunk chunk) {
        if (chunk == null || chunk.choices() == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (ChatCompletionChunk.Choice choice : chunk.choices()) {
            if (choice == null || choice.delta() == null) {
                continue;
            }
            String text = choice.delta().content().orElse(choice.delta().refusal().orElse(""));
            if (!TextUtils.isEmpty(text)) {
                builder.append(text);
            }
        }
        return builder.toString();
    }

    private String extractClaudeText(com.anthropic.models.messages.Message message) {
        if (message == null || message.content() == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (ContentBlock block : message.content()) {
            if (block != null && block.isText()) {
                builder.append(block.asText().text());
            }
        }
        return builder.toString();
    }

    private String extractClaudeDelta(RawMessageStreamEvent event) {
        if (event == null || !event.isContentBlockDelta()) {
            return "";
        }
        RawContentBlockDelta delta = event.asContentBlockDelta().delta();
        if (delta == null || !delta.isText()) {
            return "";
        }
        return delta.asText().text();
    }

    private List<Message> sanitizeMessages(List<Message> messages) {
        ArrayList<Message> sanitized = new ArrayList<>();
        for (Message message : messages) {
            String content = message.getContent() == null ? "" : message.getContent().trim();
            if (TextUtils.isEmpty(content)) {
                continue;
            }
            sanitized.add(new Message(
                    message.getId(),
                    message.getConversationId(),
                    message.getRole(),
                    content,
                    message.getCreatedAt()
            ));
        }
        return sanitized;
    }

    private String safeValue(String value) {
        return value == null ? "" : value.trim();
    }

    private String buildErrorMessage(Exception exception) {
        String message = exception.getMessage();
        if (TextUtils.isEmpty(message)) {
            return "AI 请求失败，请检查模型配置和网络连接。";
        }
        return message;
    }

    private void postPartial(AiResponseCallback callback, String partial) {
        mainHandler.post(() -> callback.onPartial(partial));
    }

    private void postComplete(AiResponseCallback callback, String content) {
        mainHandler.post(() -> callback.onComplete(TextUtils.isEmpty(content) ? "模型没有返回内容。" : content));
    }

    private void postError(AiResponseCallback callback, String error) {
        mainHandler.post(() -> callback.onError(TextUtils.isEmpty(error) ? "AI 请求失败，请稍后重试。" : error));
    }
}
