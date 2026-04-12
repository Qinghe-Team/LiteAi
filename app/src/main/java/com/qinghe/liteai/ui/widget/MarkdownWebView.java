package com.qinghe.liteai.ui.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Base64;
import android.util.TypedValue;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.nio.charset.StandardCharsets;

public class MarkdownWebView extends WebView {
    public MarkdownWebView(Context context) {
        super(context);
        init();
    }

    public MarkdownWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MarkdownWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    private void init() {
        setBackgroundColor(Color.TRANSPARENT);
        WebSettings settings = getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.setSafeBrowsingEnabled(true);
        }
        addJavascriptInterface(new HeightBridge(), "LiteAiBridge");
        setWebViewClient(new WebViewClient());
    }

    public void render(String markdown, boolean darkMode) {
        String textColor = darkMode ? "#E4E7EE" : "#102341";
        String encodedMarkdown = Base64.encodeToString(
                (markdown == null ? "" : markdown).getBytes(StandardCharsets.UTF_8),
                Base64.NO_WRAP
        );
        String html = "<!doctype html><html><head>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0' />" +
                "<style>body{margin:0;font-size:14px;line-height:1.45;color:" + textColor + ";background:transparent;word-break:break-word;}p{margin:0 0 8px;}pre{white-space:pre-wrap;background:rgba(0,0,0,0.06);padding:8px;border-radius:8px;}code{font-family:monospace;}blockquote{margin:8px 0;padding-left:8px;border-left:3px solid rgba(42,92,170,0.45);}ul,ol{padding-left:18px;}a{color:#5B8CFF;}</style>" +
                "<script src='https://cdn.jsdelivr.net/npm/marked/marked.min.js'></script>" +
                "<script>window.MathJax={tex:{inlineMath:[['$','$'],['\\(','\\)']]},svg:{fontCache:'global'}};</script>" +
                "<script async src='https://cdn.jsdelivr.net/npm/mathjax@3/es5/tex-svg.js'></script>" +
                "</head><body><div id='content'></div><script>" +
                "function decodeUtf8Base64(value){try{return new TextDecoder('utf-8').decode(Uint8Array.from(atob(value),function(char){return char.charCodeAt(0);}));}catch(error){return atob(value);}}" +
                "const rawMarkdown=decodeUtf8Base64('" + encodedMarkdown + "');" +
                "function escapeHtml(value){return value.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');}" +
                "function paint(){var safe=escapeHtml(rawMarkdown);if(window.marked){document.getElementById('content').innerHTML=marked.parse(safe,{breaks:true});}else{document.getElementById('content').innerText=rawMarkdown;}if(window.MathJax&&MathJax.typesetPromise){MathJax.typesetPromise().then(reportHeight).catch(reportHeight);}else{reportHeight();}}" +
                "function reportHeight(){setTimeout(function(){LiteAiBridge.onHeight(document.body.scrollHeight);},60);}" +
                "window.addEventListener('load',function(){setTimeout(paint,120);});" +
                "</script></body></html>";
        loadDataWithBaseURL("https://localhost/", html, "text/html", "utf-8", null);
    }

    @Override
    public void onResume() {
        super.onResume();
        resumeTimers();
    }

    @Override
    public void onPause() {
        pauseTimers();
        super.onPause();
    }

    private class HeightBridge {
        @JavascriptInterface
        public void onHeight(float height) {
            post(() -> {
                int targetHeight = (int) Math.ceil(height * getResources().getDisplayMetrics().density);
                if (targetHeight <= 0) {
                    targetHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, getResources().getDisplayMetrics());
                }
                getLayoutParams().height = targetHeight;
                requestLayout();
            });
        }
    }
}
