package com.rex.launcher

import android.content.Context
import org.json.JSONObject

class LauncherSettings(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun read(): JSONObject = JSONObject(prefs.getString(KEY_JSON, null) ?: "{}")

    fun save(rawJson: String) {
        val json = JSONObject(rawJson)
        json.put(KEY_RENDERER, sanitizeRenderer(json.optString(KEY_RENDERER)))
        json.put(KEY_TARGET_FPS, sanitizeFps(json.optInt(KEY_TARGET_FPS, DEFAULT_FPS)))
        json.put(KEY_OVERLAY, json.optBoolean(KEY_OVERLAY, DEFAULT_OVERLAY))
        prefs.edit().putString(KEY_JSON, json.toString()).apply()
    }

    private fun sanitizeRenderer(value: String): String =
        value.takeIf { it in RENDERERS } ?: DEFAULT_RENDERER

    private fun sanitizeFps(value: Int): Int =
        if (value in TARGET_FPS_OPTIONS) value else DEFAULT_FPS

    companion object {
        const val PREFS = "rex-launcher"
        private const val KEY_JSON = "settings"

        private const val KEY_RENDERER = "renderer"
        private const val KEY_TARGET_FPS = "targetFps"
        private const val KEY_OVERLAY = "overlayVisible"

        private const val DEFAULT_RENDERER = "vulkan"
        private const val DEFAULT_FPS = 60
        private const val DEFAULT_OVERLAY = true

        private val RENDERERS = setOf("vulkan", "opengl", "software")
        private val TARGET_FPS_OPTIONS = setOf(30, 60, 120)
    }
}
