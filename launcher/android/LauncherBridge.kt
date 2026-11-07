// Host WebView untuk launcher. Dipanggil dari JS sebagai window.Android.<method>(...).
// Di UI, setiap method dipanggil lewat native.call(name, ...args) di js/app.js.
package com.rex.launcher

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView

class LauncherBridge(private val activity: Activity, private val webView: WebView) {

    @JavascriptInterface
    fun launchGame(gameId: String) {
        activity.runOnUiThread { /* TODO: buka emulator dengan ROM milik gameId */ }
    }

    @JavascriptInterface
    fun pickRomFolder() {
        activity.runOnUiThread { /* TODO: buka SAF (ACTION_OPEN_DOCUMENT_TREE) lalu panggil setRomFolder */ }
    }

    @JavascriptInterface
    fun pickCoverArt(gameId: String) {
        activity.runOnUiThread { /* TODO: pilih gambar cover untuk gameId */ }
    }

    @JavascriptInterface
    fun saveSettings(json: String) {
        activity.getSharedPreferences("rex-launcher", 0).edit().putString("settings", json).apply()
    }

    @JavascriptInterface
    fun padInput(button: String, pressed: Boolean) {
        // TODO: teruskan ke core emulator sebagai input tombol
    }

    fun pushFps(fps: Int) = push("window.launcher.setFps($fps)")

    fun pushRomFolder(path: String, gameCount: Int) =
        push("window.launcher.setRomFolder('${path.replace("'", "\\'")}', $gameCount)")

    private fun push(script: String) {
        activity.runOnUiThread { webView.evaluateJavascript(script, null) }
    }
}

class LauncherActivity : Activity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            addJavascriptInterface(LauncherBridge(this@LauncherActivity, this), "Android")
            loadUrl("file:///android_asset/launcher/index.html")
        }
        setContentView(webView)
    }
}
