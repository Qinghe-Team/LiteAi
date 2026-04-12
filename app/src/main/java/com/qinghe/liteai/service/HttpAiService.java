package com.qinghe.liteai.service;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.qinghe.liteai.model.AiModelConfig;
import com.qinghe.liteai.model.Message;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpAiService implements AiService {
    private static final String MODE_OPENAI = "OpenAI";
    private static final String MODE_CLAUDE = "Claude";
    private static final String MODE_GEMINI = "Gemini";

    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    public void requestReply(AiModelConfig model, List<Message> messages, boolean streamOutput, AiResponseCallback callback) {
        executorService.execute(() -> {
            if (model == null) {
                postError(callback, "请先在“模型管理”中添加并启用一个模型。");
                return;
            }

            List<Message> requestMessages = sanitizeMessages(messages);
            if (requestMessages.isEmpty()) {
                postError(callback, "没有可发送的对话内容。");
                return;
            }

            try {
                String mode = model.getApiMode() == null ? "" : model.getApiMode();
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
        HttpURLConnection connection = null;
        try {
            connection = openConnection(resolveOpenAiUrl(model.getApiUrl()));
            connection.setRequestProperty("Authorization", "Bearer " + safeValue(model.getApiKey()));
            writeJson(connection, buildOpenAiRequest(model, messages, streamOutput));
            if (!streamOutput) {
                postComplete(callback, extractOpenAiText(readResponseObject(connection)));
                return;
            }

            StringBuilder builder = new StringBuilder();
            consumeSse(connection, (event, data) -> {
                String payload = data == null ? "" : data.trim();
                if (TextUtils.isEmpty(payload) || "[DONE]".equals(payload)) {
                    return;
                }
                JSONObject json = new JSONObject(payload);
                JSONArray choices = json.optJSONArray("choices");
                if (choices == null || choices.length() == 0) {
                    return;
                }
                JSONObject choice = choices.optJSONObject(0);
                if (choice == null) {
                    return;
                }
                JSONObject delta = choice.optJSONObject("delta");
                String chunk = extractOpenAiDelta(delta);
                if (!TextUtils.isEmpty(chunk)) {
                    builder.append(chunk);
                    postPartial(callback, builder.toString());
                }
            });
            postComplete(callback, builder.toString());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void requestClaude(AiModelConfig model, List<Message> messages, boolean streamOutput, AiResponseCallback callback) throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = openConnection(resolveClaudeUrl(model.getApiUrl()));
            connection.setRequestProperty("x-api-key", safeValue(model.getApiKey()));
            connection.setRequestProperty("anthropic-version", "2023-06-01");
            writeJson(connection, buildClaudeRequest(model, messages, streamOutput));
            if (!streamOutput) {
                postComplete(callback, extractClaudeText(readResponseObject(connection)));
                return;
            }

            StringBuilder builder = new StringBuilder();
            consumeSse(connection, (event, data) -> {
                if (TextUtils.isEmpty(data)) {
                    return;
                }
                JSONObject json = new JSONObject(data);
                String chunk = "";
                JSONObject delta = json.optJSONObject("delta");
                if (delta != null) {
                    chunk = delta.optString("text");
                }
                if (TextUtils.isEmpty(chunk)) {
                    JSONObject contentBlock = json.optJSONObject("content_block");
                    if (contentBlock != null) {
                        chunk = contentBlock.optString("text");
                    }
                }
                if (!TextUtils.isEmpty(chunk)) {
                    builder.append(chunk);
                    postPartial(callback, builder.toString());
                }
            });
            postComplete(callback, builder.toString());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void requestGemini(AiModelConfig model, List<Message> messages, boolean streamOutput, AiResponseCallback callback) throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = openConnection(resolveGeminiUrl(model, streamOutput));
            writeJson(connection, buildGeminiRequest(messages));
            if (!streamOutput) {
                postComplete(callback, extractGeminiText(readResponseObject(connection)));
                return;
            }

            StringBuilder builder = new StringBuilder();
            consumeSse(connection, (event, data) -> {
                if (TextUtils.isEmpty(data)) {
                    return;
                }
                JSONObject json = new JSONObject(data);
                String chunk = extractGeminiText(json);
                if (!TextUtils.isEmpty(chunk)) {
                    builder.append(chunk);
                    postPartial(callback, builder.toString());
                }
            });
            postComplete(callback, builder.toString());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private JSONObject buildOpenAiRequest(AiModelConfig model, List<Message> messages, boolean streamOutput) throws JSONException {
        JSONObject payload = new JSONObject();
        payload.put("model", safeValue(model.getModelCode()));
        payload.put("stream", streamOutput);
        payload.put("messages", buildSharedMessages(messages));
        return payload;
    }

    private JSONObject buildClaudeRequest(AiModelConfig model, List<Message> messages, boolean streamOutput) throws JSONException {
        JSONObject payload = new JSONObject();
        payload.put("model", safeValue(model.getModelCode()));
        payload.put("max_tokens", 2048);
        payload.put("stream", streamOutput);
        payload.put("messages", buildSharedMessages(messages));
        return payload;
    }

    private JSONObject buildGeminiRequest(List<Message> messages) throws JSONException {
        JSONObject payload = new JSONObject();
        JSONArray contents = new JSONArray();
        for (Message message : messages) {
            JSONObject item = new JSONObject();
            item.put("role", Message.ROLE_ASSISTANT.equals(message.getRole()) ? "model" : "user");
            JSONArray parts = new JSONArray();
            parts.put(new JSONObject().put("text", safeValue(message.getContent())));
            item.put("parts", parts);
            contents.put(item);
        }
        payload.put("contents", contents);
        return payload;
    }

    private JSONArray buildSharedMessages(List<Message> messages) throws JSONException {
        JSONArray array = new JSONArray();
        for (Message message : messages) {
            JSONObject item = new JSONObject();
            item.put("role", Message.ROLE_ASSISTANT.equals(message.getRole()) ? Message.ROLE_ASSISTANT : Message.ROLE_USER);
            item.put("content", safeValue(message.getContent()));
            array.put(item);
        }
        return array;
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

    private HttpURLConnection openConnection(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(60000);
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("Accept", "application/json, text/event-stream");
        return connection;
    }

    private void writeJson(HttpURLConnection connection, JSONObject payload) throws IOException {
        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(payload.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    private JSONObject readResponseObject(HttpURLConnection connection) throws Exception {
        String body = readResponseText(connection);
        if (TextUtils.isEmpty(body)) {
            return new JSONObject();
        }
        return new JSONObject(body);
    }

    private String readResponseText(HttpURLConnection connection) throws IOException {
        int code = connection.getResponseCode();
        InputStream stream = code >= 400 ? connection.getErrorStream() : connection.getInputStream();
        String body = readStream(stream);
        if (code >= 400) {
            throw new IOException(readApiErrorMessage(body, code));
        }
        return body;
    }

    private String readApiErrorMessage(String body, int code) {
        if (!TextUtils.isEmpty(body)) {
            try {
                JSONObject json = new JSONObject(body);
                JSONObject error = json.optJSONObject("error");
                if (error != null && !TextUtils.isEmpty(error.optString("message"))) {
                    return "AI 请求失败（" + code + "）： " + error.optString("message");
                }
                if (!TextUtils.isEmpty(json.optString("message"))) {
                    return "AI 请求失败（" + code + "）： " + json.optString("message");
                }
            } catch (JSONException ignored) {
            }
            return "AI 请求失败（" + code + "）： " + body;
        }
        return "AI 请求失败，HTTP " + code;
    }

    private void consumeSse(HttpURLConnection connection, SseListener listener) throws Exception {
        int code = connection.getResponseCode();
        if (code >= 400) {
            throw new IOException(readApiErrorMessage(readStream(connection.getErrorStream()), code));
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            String event = "";
            StringBuilder dataBuilder = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    if (dataBuilder.length() > 0) {
                        listener.onEvent(event, dataBuilder.toString().trim());
                        dataBuilder.setLength(0);
                        event = "";
                    }
                    continue;
                }
                if (line.startsWith("event:")) {
                    event = line.substring(6).trim();
                } else if (line.startsWith("data:")) {
                    if (dataBuilder.length() > 0) {
                        dataBuilder.append('\n');
                    }
                    dataBuilder.append(line.substring(5).trim());
                }
            }
            if (dataBuilder.length() > 0) {
                listener.onEvent(event, dataBuilder.toString().trim());
            }
        }
    }

    private String extractOpenAiText(JSONObject response) {
        JSONArray choices = response.optJSONArray("choices");
        if (choices == null || choices.length() == 0) {
            return "";
        }
        JSONObject choice = choices.optJSONObject(0);
        if (choice == null) {
            return "";
        }
        JSONObject message = choice.optJSONObject("message");
        return extractTextValue(message == null ? null : message.opt("content"));
    }

    private String extractOpenAiDelta(JSONObject delta) {
        if (delta == null) {
            return "";
        }
        return extractTextValue(delta.opt("content"));
    }

    private String extractClaudeText(JSONObject response) {
        JSONArray content = response.optJSONArray("content");
        if (content == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < content.length(); index++) {
            JSONObject block = content.optJSONObject(index);
            if (block == null) {
                continue;
            }
            if (!TextUtils.isEmpty(block.optString("text"))) {
                builder.append(block.optString("text"));
            }
        }
        return builder.toString();
    }

    private String extractGeminiText(JSONObject response) {
        JSONArray candidates = response.optJSONArray("candidates");
        if (candidates == null || candidates.length() == 0) {
            return "";
        }
        JSONObject candidate = candidates.optJSONObject(0);
        if (candidate == null) {
            return "";
        }
        JSONObject content = candidate.optJSONObject("content");
        if (content == null) {
            return "";
        }
        JSONArray parts = content.optJSONArray("parts");
        if (parts == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < parts.length(); index++) {
            JSONObject part = parts.optJSONObject(index);
            if (part != null && !TextUtils.isEmpty(part.optString("text"))) {
                builder.append(part.optString("text"));
            }
        }
        return builder.toString();
    }

    private String extractTextValue(Object content) {
        if (content instanceof String) {
            return (String) content;
        }
        if (content instanceof JSONArray) {
            JSONArray array = (JSONArray) content;
            StringBuilder builder = new StringBuilder();
            for (int index = 0; index < array.length(); index++) {
                Object part = array.opt(index);
                if (part instanceof JSONObject) {
                    JSONObject object = (JSONObject) part;
                    if (!TextUtils.isEmpty(object.optString("text"))) {
                        builder.append(object.optString("text"));
                    }
                } else if (part instanceof String) {
                    builder.append((String) part);
                }
            }
            return builder.toString();
        }
        return "";
    }

    private String resolveOpenAiUrl(String apiUrl) throws Exception {
        return appendPath(apiUrl, "/chat/completions");
    }

    private String resolveClaudeUrl(String apiUrl) throws Exception {
        return appendPath(apiUrl, "/messages");
    }

    private String resolveGeminiUrl(AiModelConfig model, boolean streamOutput) throws Exception {
        String apiUrl = safeValue(model.getApiUrl());
        String action = streamOutput ? ":streamGenerateContent" : ":generateContent";
        String resolved;
        if (apiUrl.contains(":generateContent") || apiUrl.contains(":streamGenerateContent")) {
            resolved = apiUrl
                    .replace(":streamGenerateContent", action)
                    .replace(":generateContent", action);
        } else {
            resolved = appendPath(apiUrl, "/models/" + safeValue(model.getModelCode()) + action);
        }
        if (streamOutput && !resolved.contains("alt=sse")) {
            resolved = appendQueryParameter(resolved, "alt", "sse");
        }
        return appendQueryParameter(resolved, "key", safeValue(model.getApiKey()));
    }

    private String appendPath(String rawUrl, String suffix) throws Exception {
        URI uri = new URI(safeValue(rawUrl));
        String path = uri.getPath() == null ? "" : uri.getPath();
        if (!path.endsWith(suffix)) {
            if (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
            path = path + suffix;
        }
        return new URI(uri.getScheme(), uri.getAuthority(), path, uri.getQuery(), uri.getFragment()).toString();
    }

    private String appendQueryParameter(String rawUrl, String name, String value) throws Exception {
        URI uri = new URI(rawUrl);
        String query = uri.getQuery();
        if (!TextUtils.isEmpty(query)) {
            String[] parts = query.split("&");
            for (String part : parts) {
                if (part.startsWith(name + "=")) {
                    return rawUrl;
                }
            }
        }
        String encoded = URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        String newQuery = TextUtils.isEmpty(query) ? name + "=" + encoded : query + "&" + name + "=" + encoded;
        return new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), newQuery, uri.getFragment()).toString();
    }

    private String safeValue(String value) {
        return value == null ? "" : value.trim();
    }

    private String readStream(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        try (InputStream inputStream = stream; ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }
            return outputStream.toString(StandardCharsets.UTF_8.name());
        }
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

    private interface SseListener {
        void onEvent(String event, String data) throws Exception;
    }
}
