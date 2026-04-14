package com.qinghe.liteai.ui.markdown;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.util.TypedValue;
import android.widget.TextView;

import com.qinghe.liteai.util.MarkdownMessageFormatter;
import com.qinghe.liteai.util.StreamingMarkdownSegmenter;

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
        RenderState state = RenderState.full(normalized);
        if (state.equals(textView.getTag())) {
            return;
        }
        textView.setTag(state);
        Markwon markwon = getOrCreate(textView);
        markwon.setParsedMarkdown(textView, markwon.toMarkdown(normalized));
    }

    public static void renderStreaming(TextView textView, String markdown) {
        StreamingMarkdownSegmenter.Segment segment = StreamingMarkdownSegmenter.segment(markdown);
        RenderState previous = textView.getTag() instanceof RenderState ? (RenderState) textView.getTag() : null;
        RenderState state = RenderState.streaming(segment.getNormalized(), segment.getMarkdownPrefix());
        if (state.equals(textView.getTag())) {
            return;
        }
        if (!segment.hasMarkdownPrefix()) {
            textView.setTag(state);
            textView.setText(segment.getNormalized());
            return;
        }
        Markwon markwon = getOrCreate(textView);
        CharSequence renderedPrefix = null;
        if (previous != null && previous.canReuseStreamingPrefix(segment.getMarkdownPrefix())) {
            renderedPrefix = previous.renderedPrefix;
        }
        if (renderedPrefix == null) {
            renderedPrefix = markwon.toMarkdown(segment.getMarkdownPrefix());
        }
        textView.setTag(state.withRenderedPrefix(renderedPrefix));
        SpannableStringBuilder builder = new SpannableStringBuilder(renderedPrefix);
        builder.append(segment.getPlainSuffix());
        markwon.setParsedMarkdown(textView, builder);
    }

    public static void renderPlainText(TextView textView, String text) {
        String safeText = text == null ? "" : text;
        RenderState state = RenderState.plain(safeText);
        if (state.equals(textView.getTag())) {
            return;
        }
        textView.setTag(state);
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

    private static final class RenderState {
        private final String text;
        private final String markdownPrefix;
        private final int mode;
        private final CharSequence renderedPrefix;

        private RenderState(String text, String markdownPrefix, int mode, CharSequence renderedPrefix) {
            this.text = text;
            this.markdownPrefix = markdownPrefix;
            this.mode = mode;
            this.renderedPrefix = renderedPrefix;
        }

        static RenderState full(String text) {
            return new RenderState(text, text, 1, null);
        }

        static RenderState streaming(String text, String markdownPrefix) {
            return new RenderState(text, markdownPrefix, 2, null);
        }

        static RenderState plain(String text) {
            return new RenderState(text, "", 0, null);
        }

        RenderState withRenderedPrefix(CharSequence renderedPrefix) {
            return new RenderState(text, markdownPrefix, mode, renderedPrefix);
        }

        boolean canReuseStreamingPrefix(String prefix) {
            return mode == 2
                    && renderedPrefix != null
                    && markdownPrefix.equals(prefix);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof RenderState)) {
                return false;
            }
            RenderState that = (RenderState) other;
            return mode == that.mode
                    && text.equals(that.text)
                    && markdownPrefix.equals(that.markdownPrefix);
        }

        @Override
        public int hashCode() {
            int result = text.hashCode();
            result = 31 * result + markdownPrefix.hashCode();
            result = 31 * result + mode;
            return result;
        }
    }
}
