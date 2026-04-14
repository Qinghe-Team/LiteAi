package com.qinghe.liteai.ui.markdown;

import android.content.Context;
import android.util.TypedValue;
import android.widget.TextView;

import com.qinghe.liteai.util.MarkdownMessageFormatter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.noties.markwon.Markwon;
import io.noties.markwon.ext.latex.JLatexMathPlugin;
import io.noties.markwon.ext.tables.TablePlugin;
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin;

public final class MarkdownRenderer {
    private static final Map<String, Markwon> CACHE = new ConcurrentHashMap<>();
    private static final ExecutorService LATEX_EXECUTOR = Executors.newSingleThreadExecutor();

    private MarkdownRenderer() {
    }

    public static void render(TextView textView, String markdown) {
        String normalized = MarkdownMessageFormatter.normalize(markdown);
        Object rendered = textView.getTag();
        if (normalized.equals(rendered)) {
            return;
        }
        textView.setTag(normalized);
        Markwon markwon = getOrCreate(textView);
        markwon.setMarkdown(textView, normalized);
    }

    public static void renderPlainText(TextView textView, String text) {
        String safeText = text == null ? "" : text;
        if (safeText.contentEquals(textView.getText())) {
            return;
        }
        textView.setTag(null);
        textView.setText(safeText);
    }

    private static Markwon getOrCreate(TextView textView) {
        Context context = textView.getContext().getApplicationContext();
        int textColor = textView.getCurrentTextColor();
        int textSizePx = Math.round(textView.getTextSize());
        String cacheKey = textColor + ":" + textSizePx;
        return CACHE.computeIfAbsent(cacheKey, ignored -> build(context, textColor, textSizePx));
    }

    private static Markwon build(Context context, int textColor, int textSizePx) {
        float latexTextSize = textSizePx > 0
                ? textSizePx
                : TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14, context.getResources().getDisplayMetrics());
        return Markwon.builder(context)
                .usePlugin(MarkwonInlineParserPlugin.create())
                .usePlugin(TablePlugin.create(context))
                .usePlugin(JLatexMathPlugin.create(latexTextSize, builder -> {
                    builder.inlinesEnabled(true);
                    builder.executorService(LATEX_EXECUTOR);
                    builder.theme().textColor(textColor);
                }))
                .build();
    }
}
