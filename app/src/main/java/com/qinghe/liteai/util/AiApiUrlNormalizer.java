package com.qinghe.liteai.util;

import java.net.URI;

public final class AiApiUrlNormalizer {
    private AiApiUrlNormalizer() {
    }

    public static String normalizeOpenAiBaseUrl(String rawUrl) throws Exception {
        URI uri = requireUri(rawUrl);
        String path = trimTrailingSlash(uri.getPath());
        path = removeSuffix(path, "/chat/completions");
        if (isBlank(path) || "/".equals(path)) {
            path = "/v1";
        }
        return rebuild(uri, path);
    }

    public static String normalizeClaudeBaseUrl(String rawUrl) throws Exception {
        URI uri = requireUri(rawUrl);
        String path = trimTrailingSlash(uri.getPath());
        path = removeSuffix(path, "/v1/messages");
        path = removeSuffix(path, "/messages");
        path = removeSuffix(path, "/v1");
        return rebuild(uri, isBlank(path) ? "" : path);
    }

    public static String normalizeGeminiBaseUrl(String rawUrl) throws Exception {
        URI uri = requireUri(rawUrl);
        String path = trimTrailingSlash(uri.getPath());
        int modelIndex = path.indexOf("/models/");
        if (modelIndex >= 0) {
            path = path.substring(0, modelIndex);
        }
        path = trimTrailingSlash(path);
        return rebuild(uri, isBlank(path) ? "" : path);
    }

    private static URI requireUri(String rawUrl) throws Exception {
        String safeUrl = rawUrl == null ? "" : rawUrl.trim();
        if (isBlank(safeUrl)) {
            throw new IllegalArgumentException("API 地址不能为空。");
        }
        URI uri = new URI(safeUrl);
        if (isBlank(uri.getScheme()) || isBlank(uri.getHost())) {
            throw new IllegalArgumentException("API 地址格式无效，请输入完整的 http(s) 地址。");
        }
        return uri;
    }

    private static String rebuild(URI uri, String path) throws Exception {
        String safePath = isBlank(path) ? "" : path;
        return new URI(uri.getScheme(), uri.getAuthority(), safePath, null, null).toString();
    }

    private static String trimTrailingSlash(String path) {
        if (isBlank(path) || "/".equals(path)) {
            return path == null ? "" : path;
        }
        String normalized = path;
        while (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static String removeSuffix(String value, String suffix) {
        if (isBlank(value) || isBlank(suffix) || !value.endsWith(suffix)) {
            return value;
        }
        String normalized = value.substring(0, value.length() - suffix.length());
        return isBlank(normalized) ? "" : normalized;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
