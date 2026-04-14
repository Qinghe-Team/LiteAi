package com.qinghe.liteai.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StreamingMarkdownSegmenterTest {

    @Test
    public void segment_keepsIncompleteLatexAsPlainText() {
        StreamingMarkdownSegmenter.Segment segment = StreamingMarkdownSegmenter.segment("公式 $a^2+b");

        assertEquals("", segment.getMarkdownPrefix());
        assertEquals("公式 $a^2+b", segment.getPlainSuffix());
    }

    @Test
    public void segment_rendersCompletedLatexOnlyOnce() {
        StreamingMarkdownSegmenter.Segment segment = StreamingMarkdownSegmenter.segment("公式 $a^2+b^2=c^2$ 继续输出");

        assertEquals("公式 $$a^2+b^2=c^2$$", segment.getMarkdownPrefix());
        assertEquals(" 继续输出", segment.getPlainSuffix());
    }

    @Test
    public void segment_rendersCompletedFenceAndLeavesTrailingSuffixPlain() {
        StreamingMarkdownSegmenter.Segment segment = StreamingMarkdownSegmenter.segment("```java\nint a = 1;\n```\n后续");

        assertEquals("```java\nint a = 1;\n```\n", segment.getMarkdownPrefix());
        assertEquals("后续", segment.getPlainSuffix());
    }

    @Test
    public void segment_keepsUnclosedFenceAsPlainText() {
        StreamingMarkdownSegmenter.Segment segment = StreamingMarkdownSegmenter.segment("```java\nint a = 1;");

        assertEquals("", segment.getMarkdownPrefix());
        assertEquals("```java\nint a = 1;", segment.getPlainSuffix());
    }
}
