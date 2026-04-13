package com.qinghe.liteai.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MarkdownMessageFormatter {
    private static final Pattern INLINE_PAREN_LATEX = Pattern.compile("\\\\\\((.+?)\\\\\\)");

    private MarkdownMessageFormatter() {
    }

    public static String normalize(String markdown) {
        if (isBlank(markdown)) {
            return "";
        }

        String source = markdown.replace("\r\n", "\n");
        String[] lines = source.split("\n", -1);
        StringBuilder builder = new StringBuilder(source.length() + 32);
        boolean inFence = false;
        String fenceToken = "";

        for (int index = 0; index < lines.length; index++) {
            String line = lines[index];
            String trimmed = line.trim();
            String candidateFence = resolveFenceToken(trimmed);

            if (!isBlank(candidateFence)) {
                if (!inFence) {
                    inFence = true;
                    fenceToken = candidateFence;
                } else if (trimmed.startsWith(fenceToken)) {
                    inFence = false;
                    fenceToken = "";
                }
                builder.append(line);
            } else if (inFence) {
                builder.append(line);
            } else {
                builder.append(normalizeMarkdownLine(line, trimmed));
            }

            if (index < lines.length - 1) {
                builder.append('\n');
            }
        }

        return builder.toString();
    }

    static String normalizeMarkdownLine(String line, String trimmedLine) {
        if (isStandaloneDoubleDollar(trimmedLine)) {
            String content = trimmedLine.substring(2, trimmedLine.length() - 2).trim();
            return "$$\n" + content + "\n$$";
        }
        if (isStandaloneBracketBlock(trimmedLine)) {
            String content = trimmedLine.substring(2, trimmedLine.length() - 2).trim();
            return "$$\n" + content + "\n$$";
        }
        return normalizeSingleDollarInlineMath(normalizeInlineParenMath(line));
    }

    private static String normalizeInlineParenMath(String line) {
        Matcher matcher = INLINE_PAREN_LATEX.matcher(line);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, Matcher.quoteReplacement("$$" + matcher.group(1) + "$$"));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String normalizeSingleDollarInlineMath(String line) {
        StringBuilder builder = new StringBuilder(line.length() + 8);
        boolean inCodeSpan = false;

        for (int index = 0; index < line.length(); index++) {
            char current = line.charAt(index);
            if (current == '`') {
                inCodeSpan = !inCodeSpan;
                builder.append(current);
                continue;
            }
            if (!inCodeSpan && current == '$' && isSingleDollar(line, index) && !isEscaped(line, index)) {
                int closing = findClosingSingleDollar(line, index + 1);
                if (closing > index + 1) {
                    String expression = line.substring(index + 1, closing);
                    if (containsVisibleText(expression)) {
                        builder.append("$$").append(expression).append("$$");
                        index = closing;
                        continue;
                    }
                }
            }
            builder.append(current);
        }
        return builder.toString();
    }

    private static int findClosingSingleDollar(String line, int start) {
        boolean inCodeSpan = false;
        for (int index = start; index < line.length(); index++) {
            char current = line.charAt(index);
            if (current == '`') {
                inCodeSpan = !inCodeSpan;
                continue;
            }
            if (!inCodeSpan && current == '$' && isSingleDollar(line, index) && !isEscaped(line, index)) {
                return index;
            }
        }
        return -1;
    }

    private static boolean isSingleDollar(String line, int index) {
        char previous = index > 0 ? line.charAt(index - 1) : '\0';
        char next = index + 1 < line.length() ? line.charAt(index + 1) : '\0';
        return previous != '$' && next != '$';
    }

    private static boolean isEscaped(String line, int index) {
        return index > 0 && line.charAt(index - 1) == '\\';
    }

    private static boolean containsVisibleText(String text) {
        return !isBlank(text) && !isBlank(text.trim());
    }

    private static boolean isStandaloneDoubleDollar(String trimmedLine) {
        return trimmedLine.startsWith("$$")
                && trimmedLine.endsWith("$$")
                && trimmedLine.length() > 4
                && trimmedLine.indexOf("$$", 2) == trimmedLine.length() - 2;
    }

    private static boolean isStandaloneBracketBlock(String trimmedLine) {
        return trimmedLine.startsWith("\\[")
                && trimmedLine.endsWith("\\]")
                && trimmedLine.length() > 4;
    }

    private static String resolveFenceToken(String trimmedLine) {
        if (trimmedLine.startsWith("```")) {
            return "```";
        }
        if (trimmedLine.startsWith("~~~")) {
            return "~~~";
        }
        return "";
    }

    private static boolean isBlank(String value) {
        return value == null || value.isEmpty();
    }
}
