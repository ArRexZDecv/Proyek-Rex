package com.rex.launcher

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewAssetLoader.AssetsPathHandler
import androidx.webkit.WebViewAssetLoader.InternalStoragePathHandler
import androidx.webkit.WebViewClientCompat
import java.io.File

class LauncherActivity : ComponentActivity() {

    private lateinit var bridge: LauncherBridge

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", AssetsPathHandler(this))
            .addPathHandler(
                "/${LauncherBridge.COVER_PATH}/",
                InternalStoragePathHandler(this, File(filesDir, LauncherBridge.COVER_PATH)),
            )
            .build()

        val webView = WebView(this)
        bridge = LauncherBridge(this, webView, LauncherSettings(this))

        webView.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = object : WebViewClientCompat() {
                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                    assetLoader.shouldInterceptRequest(request.url)

                override fun onPageFinished(view: WebView?, url: String?) {
                    bridge.publishState()
                }
            }
            addJavascriptInterface(bridge, "Android")
            loadUrl("${LauncherBridge.ASSET_ORIGIN}/assets/launcher/index.html")
        }
        setContentView(webView)
    }
}
