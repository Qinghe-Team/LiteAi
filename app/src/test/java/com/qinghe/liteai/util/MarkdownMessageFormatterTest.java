package com.qinghe.liteai.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MarkdownMessageFormatterTest {

    @Test
    public void normalize_convertsInlineSingleDollarMath() {
        assertEquals("公式 $$a^2+b^2=c^2$$ 示例", MarkdownMessageFormatter.normalize("公式 $a^2+b^2=c^2$ 示例"));
    }

    @Test
    public void normalize_convertsInlineParenLatex() {
        assertEquals("这里是 $$e^x$$ 示例", MarkdownMessageFormatter.normalize("这里是 \\(e^x\\) 示例"));
    }

    @Test
    public void normalize_convertsStandaloneDisplayMathLine() {
        assertEquals("$$\nx = y + z\n$$", MarkdownMessageFormatter.normalize("$$x = y + z$$"));
    }

    @Test
    public void normalize_keepsFencedCodeUntouched() {
        String markdown = "```python\nprice = \"$5\"\n```";
        assertTrue(MarkdownMessageFormatter.normalize(markdown).contains("price = \"$5\""));
    }
}
