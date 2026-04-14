package com.qinghe.liteai.util;

public final class StreamingMarkdownSegmenter {
    private StreamingMarkdownSegmenter() {
    }

    public static Segment segment(String markdown) {
        String normalized = MarkdownMessageFormatter.normalize(markdown == null ? "" : markdown);
        int stableEnd = findStableMarkdownEnd(normalized);
        if (stableEnd <= 0) {
            return new Segment(normalized, "", normalized);
        }
        return new Segment(
                normalized,
                normalized.substring(0, stableEnd),
                normalized.substring(stableEnd)
        );
    }

    static int findStableMarkdownEnd(String normalized) {
        if (normalized == null || normalized.isEmpty()) {
            return 0;
        }

        int stableEnd = 0;
        boolean inFence = false;
        String fenceToken = "";
        boolean inCodeSpan = false;
        int mathStart = -1;

        for (int index = 0; index < normalized.length(); index++) {
            if (isLineStart(normalized, index)) {
                String candidateFence = resolveFenceTokenAt(normalized, index);
                if (!candidateFence.isEmpty()) {
                    int lineEnd = findLineEnd(normalized, index);
                    if (!inFence) {
                        inFence = true;
                        fenceToken = candidateFence;
                        index = lineEnd;
                        continue;
                    }
                    if (candidateFence.equals(fenceToken)) {
                        inFence = false;
                        fenceToken = "";
                        stableEnd = lineEnd + 1;
                        index = lineEnd;
                        continue;
                    }
                }
            }

            if (inFence) {
                continue;
            }

            char current = normalized.charAt(index);
            if (current == '`') {
                inCodeSpan = !inCodeSpan;
                continue;
            }
            if (inCodeSpan) {
                continue;
            }
            if (isDoubleDollar(normalized, index)) {
                if (mathStart < 0) {
                    mathStart = index;
                } else {
                    stableEnd = index + 2;
                    mathStart = -1;
                }
                index++;
            }
        }

        return Math.min(stableEnd, normalized.length());
    }

    private static boolean isLineStart(String value, int index) {
        return index == 0 || value.charAt(index - 1) == '\n';
    }

    private static String resolveFenceTokenAt(String value, int index) {
        if (value.startsWith("```", index)) {
            return "```";
        }
        if (value.startsWith("~~~", index)) {
            return "~~~";
        }
        return "";
    }

    private static int findLineEnd(String value, int start) {
        int lineEnd = value.indexOf('\n', start);
        return lineEnd >= 0 ? lineEnd : value.length() - 1;
    }

    private static boolean isDoubleDollar(String value, int index) {
        return index + 1 < value.length()
                && value.charAt(index) == '$'
                && value.charAt(index + 1) == '$'
                && !isEscaped(value, index);
    }

    private static boolean isEscaped(String value, int index) {
        int backslashCount = 0;
        for (int cursor = index - 1; cursor >= 0 && value.charAt(cursor) == '\\'; cursor--) {
            backslashCount++;
        }
        return (backslashCount % 2) == 1;
    }

    public static final class Segment {
        private final String normalized;
        private final String markdownPrefix;
        private final String plainSuffix;

        Segment(String normalized, String markdownPrefix, String plainSuffix) {
            this.normalized = normalized;
            this.markdownPrefix = markdownPrefix;
            this.plainSuffix = plainSuffix;
        }

        public String getNormalized() {
            return normalized;
        }

        public String getMarkdownPrefix() {
            return markdownPrefix;
        }

        public String getPlainSuffix() {
            return plainSuffix;
        }

        public boolean hasMarkdownPrefix() {
            return !markdownPrefix.isEmpty();
        }
    }
}
