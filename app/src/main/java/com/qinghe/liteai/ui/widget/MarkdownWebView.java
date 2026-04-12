package com.qinghe.liteai.ui.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Base64;
import android.util.TypedValue;
import android.webkit.WebResourceRequest;
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
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(false);
        settings.setBlockNetworkLoads(true);
        settings.setBlockNetworkImage(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            settings.setAllowFileAccessFromFileURLs(false);
            settings.setAllowUniversalAccessFromFileURLs(false);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.setSafeBrowsingEnabled(true);
        }
        addJavascriptInterface(new HeightBridge(), "LiteAiBridge");
        setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return shouldBlock(request == null ? null : request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return shouldBlock(url == null ? null : Uri.parse(url));
            }
        });
    }

    public void render(String markdown, boolean darkMode) {
        String textColor = darkMode ? "#E4E7EE" : "#102341";
        String mutedColor = darkMode ? "rgba(226,232,244,0.72)" : "rgba(16,35,65,0.68)";
        String codeBackground = darkMode ? "rgba(255,255,255,0.08)" : "rgba(0,0,0,0.06)";
        String borderColor = darkMode ? "rgba(180,197,255,0.32)" : "rgba(42,92,170,0.28)";
        String encodedMarkdown = Base64.encodeToString(
                (markdown == null ? "" : markdown).getBytes(StandardCharsets.UTF_8),
                Base64.NO_WRAP
        );
        String html = "<!doctype html><html><head>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0' />" +
                "<link rel='stylesheet' href='markdown/katex.min.css' />" +
                "<style>" +
                "body{margin:0;font-size:14px;line-height:1.5;color:" + textColor + ";background:transparent;word-break:break-word;overflow-wrap:anywhere;}" +
                "h1,h2,h3,h4,h5,h6{margin:0 0 10px;line-height:1.35;}h1{font-size:1.3em;}h2{font-size:1.18em;}h3{font-size:1.08em;}" +
                "p{margin:0 0 8px;}ul,ol{margin:0 0 8px;padding-left:18px;}li+li{margin-top:4px;}" +
                "blockquote{margin:8px 0;padding:0 0 0 8px;border-left:3px solid " + borderColor + ";color:" + mutedColor + ";}" +
                "pre{margin:8px 0;padding:8px 10px;background:" + codeBackground + ";border-radius:8px;white-space:pre-wrap;word-break:break-word;overflow-wrap:anywhere;}" +
                "code{font-family:monospace;background:" + codeBackground + ";border-radius:6px;padding:1px 4px;}" +
                "pre code{background:transparent;padding:0;border-radius:0;}" +
                "hr{border:none;border-top:1px solid " + borderColor + ";margin:12px 0;}" +
                "table{border-collapse:collapse;display:block;max-width:100%;overflow:auto;margin:8px 0;}th,td{border:1px solid " + borderColor + ";padding:4px 6px;}" +
                "a{color:#5B8CFF;text-decoration:none;}img{max-width:100%;height:auto;}" +
                ".katex-display{margin:8px 0;overflow-x:auto;overflow-y:hidden;padding-bottom:2px;}" +
                "</style>" +
                "<script src='markdown/marked.umd.js'></script>" +
                "<script src='markdown/katex.min.js'></script>" +
                "<script src='markdown/auto-render.min.js'></script>" +
                "</head><body><div id='content'></div><script>" +
                "function decodeUtf8Base64(value){try{return new TextDecoder('utf-8').decode(Uint8Array.from(atob(value),function(char){return char.charCodeAt(0);}));}catch(error){return atob(value);}}" +
                "const rawMarkdown=decodeUtf8Base64('" + encodedMarkdown + "');" +
                "function escapeHtml(value){return value.replace(/</g,'&lt;').replace(/>/g,'&gt;');}" +
                "function reportHeight(){setTimeout(function(){LiteAiBridge.onHeight(Math.max(document.body.scrollHeight,document.documentElement.scrollHeight));},80);}" +
                "function renderMarkdown(){var safe=escapeHtml(rawMarkdown);if(window.marked&&window.marked.parse){marked.setOptions({breaks:true,gfm:true});document.getElementById('content').innerHTML=marked.parse(safe);}else{document.getElementById('content').innerHTML='<pre>'+safe+'</pre>';}}" +
                "function renderMath(){if(window.renderMathInElement){renderMathInElement(document.getElementById('content'),{throwOnError:false,strict:'ignore',delimiters:[{left:'$$',right:'$$',display:true},{left:'\\\\[',right:'\\\\]',display:true},{left:'$',right:'$',display:false},{left:'\\\\(',right:'\\\\)',display:false}],ignoredTags:['script','noscript','style','textarea','pre','code']});}}" +
                "function paint(){renderMarkdown();renderMath();reportHeight();setTimeout(reportHeight,200);setTimeout(reportHeight,600);}" +
                "window.addEventListener('load',paint);" +
                "if(window.ResizeObserver){new ResizeObserver(reportHeight).observe(document.body);}" +
                "</script></body></html>";
        loadDataWithBaseURL("file:///android_asset/", html, "text/html", "utf-8", null);
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
                if (getLayoutParams() == null) {
                    return;
                }
                int targetHeight = (int) Math.ceil(height * getResources().getDisplayMetrics().density);
                if (targetHeight <= 0) {
                    targetHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, getResources().getDisplayMetrics());
                }
                getLayoutParams().height = targetHeight;
                requestLayout();
            });
        }
    }

    private boolean shouldBlock(Uri uri) {
        if (uri == null) {
            return true;
        }
        String uriString = uri.toString();
        return !uriString.startsWith("file:///android_asset/");
    }
}
