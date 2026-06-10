package com.arenaai.app.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.arenaai.app.showAppInfoDialog

class WebAppInterface {
    @JavascriptInterface
    fun showAppInfo() {
        showAppInfoDialog.value = true
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(url: String) {
    var webView: WebView? by remember { mutableStateOf(null) }
    var isLoading by remember { mutableStateOf(true) }

    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        builtInZoomControls = true
                        displayZoomControls = false
                        allowFileAccess = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        javaScriptCanOpenWindowsAutomatically = true
                        
                        // Fix for Google Login (Bypass "disallowed_useragent" error)
                        userAgentString = userAgentString.replace("; wv", "")
                    }

                    // Crucial for Google Login to maintain session state
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                    addJavascriptInterface(WebAppInterface(), "Android")

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            isLoading = true
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false
                            
                            // Aggressive JavaScript to clone an existing menu item and force it into the sidebar
                            val js = """
                                (function() {
                                    function forceInject() {
                                        if (document.getElementById('android-app-info-btn')) return;
                                        
                                        var elements = document.querySelectorAll('*');
                                        var targetSibling = null;
                                        
                                        // Find a known sidebar item (Leaderboard, Search, Battle Mode, New Chat)
                                        for (var i = 0; i < elements.length; i++) {
                                            var el = elements[i];
                                            if (el.children.length === 0) {
                                                var text = el.textContent.trim();
                                                if (text === 'Leaderboard' || text === 'Search' || text === 'Battle Mode' || text === 'New Chat') {
                                                    // Get the clickable container
                                                    var container = el.closest('a') || el.closest('button') || el.closest('[role="button"]') || el.parentElement;
                                                    if (container && container.parentElement) {
                                                        targetSibling = container;
                                                        break;
                                                    }
                                                }
                                            }
                                        }
                                        
                                        if (targetSibling && targetSibling.parentElement) {
                                            // Clone the exact element to keep all CSS/Tailwind classes perfectly matching
                                            var btn = targetSibling.cloneNode(true);
                                            btn.id = 'android-app-info-btn';
                                            
                                            if (btn.tagName === 'A') {
                                                btn.removeAttribute('href');
                                                btn.removeAttribute('target');
                                            }
                                            
                                            // Replace text dynamically
                                            function replaceText(node) {
                                                if (node.nodeType === 3) { // Text node
                                                    var t = node.nodeValue.trim();
                                                    if (t === 'Leaderboard' || t === 'Search' || t === 'Battle Mode' || t === 'New Chat') {
                                                        node.nodeValue = 'App Info';
                                                    }
                                                } else {
                                                    for (var j = 0; j < node.childNodes.length; j++) {
                                                        replaceText(node.childNodes[j]);
                                                    }
                                                }
                                            }
                                            replaceText(btn);
                                            
                                            // Replace SVG icon with an Info icon
                                            var svg = btn.querySelector('svg');
                                            if (svg) {
                                                svg.innerHTML = '<circle cx="12" cy="12" r="10"></circle><line x1="12" y1="16" x2="12" y2="12"></line><line x1="12" y1="8" x2="12.01" y2="8"></line>';
                                            }
                                            
                                            btn.style.cursor = 'pointer';
                                            
                                            // Add click listener (using capture phase to intercept React events)
                                            btn.addEventListener('click', function(e) {
                                                e.preventDefault();
                                                e.stopPropagation();
                                                if (window.Android) {
                                                    window.Android.showAppInfo();
                                                }
                                            }, true);
                                            
                                            // Append to the sidebar list
                                            targetSibling.parentElement.appendChild(btn);
                                            return true;
                                        }
                                        return false;
                                    }
                                    
                                    // Use MutationObserver to watch for DOM changes (e.g. after login or navigation)
                                    var observer = new MutationObserver(function() {
                                        forceInject();
                                    });
                                    observer.observe(document.body, { childList: true, subtree: true });
                                    
                                    // Also run periodically just to be safe
                                    setInterval(forceInject, 1000);
                                })();
                            """.trimIndent()
                            
                            view?.evaluateJavascript(js, null)
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val targetUrl = request?.url?.toString() ?: return false
                            
                            // Let WebView load normal web pages
                            if (targetUrl.startsWith("http://") || targetUrl.startsWith("https://")) {
                                return false
                            }
                            
                            // Handle special intents (like opening external apps for auth if needed)
                            try {
                                val intent = Intent.parseUri(targetUrl, Intent.URI_INTENT_SCHEME)
                                context.startActivity(intent)
                                return true
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            
                            return false
                        }
                    }
                    
                    webChromeClient = WebChromeClient()
                    
                    loadUrl(url)
                    webView = this
                }
            },
            update = {
                webView = it
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
