package com.rex.launcher

import android.content.Context
import org.json.JSONObject

class LauncherSettings(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun read(): JSONObject {
        val rawJson = prefs.getString(KEY_JSON, null)
        val json = if (!rawJson.isNullOrEmpty()) {
            runCatching { JSONObject(rawJson) }.getOrDefault(JSONObject())
        } else {
            JSONObject()
        }

        // Baca preferensi individual dari SharedPreferences (prioritas) atau dari raw JSON
        val renderer = sanitizeRenderer(
            if (prefs.contains(KEY_RENDERER)) {
                prefs.getString(KEY_RENDERER, DEFAULT_RENDERER) ?: DEFAULT_RENDERER
            } else {
                json.optString(KEY_RENDERER, DEFAULT_RENDERER)
            }
        )
        val targetFps = sanitizeFps(
            if (prefs.contains(KEY_TARGET_FPS)) {
                prefs.getInt(KEY_TARGET_FPS, DEFAULT_FPS)
            } else {
                json.optInt(KEY_TARGET_FPS, DEFAULT_FPS)
            }
        )
        val overlayVisible = if (prefs.contains(KEY_OVERLAY)) {
            prefs.getBoolean(KEY_OVERLAY, DEFAULT_OVERLAY)
        } else {
            json.optBoolean(KEY_OVERLAY, DEFAULT_OVERLAY)
        }

        json.put(KEY_RENDERER, renderer)
        json.put(KEY_TARGET_FPS, targetFps)
        json.put(KEY_OVERLAY, overlayVisible)

        return json
    }

    fun save(rawJson: String) {
        val json = JSONObject(rawJson)
        val renderer = sanitizeRenderer(json.optString(KEY_RENDERER, DEFAULT_RENDERER))
        val targetFps = sanitizeFps(json.optInt(KEY_TARGET_FPS, DEFAULT_FPS))
        val overlayVisible = json.optBoolean(KEY_OVERLAY, DEFAULT_OVERLAY)

        json.put(KEY_RENDERER, renderer)
        json.put(KEY_TARGET_FPS, targetFps)
        json.put(KEY_OVERLAY, overlayVisible)

        prefs.edit()
            .putString(KEY_JSON, json.toString())
            .putString(KEY_RENDERER, renderer)
            .putInt(KEY_TARGET_FPS, targetFps)
            .putBoolean(KEY_OVERLAY, overlayVisible)
            .apply()
    }

    fun getRenderer(): String =
        sanitizeRenderer(prefs.getString(KEY_RENDERER, DEFAULT_RENDERER) ?: DEFAULT_RENDERER)

    fun getTargetFps(): Int =
        sanitizeFps(prefs.getInt(KEY_TARGET_FPS, DEFAULT_FPS))

    fun isOverlayVisible(): Boolean =
        prefs.getBoolean(KEY_OVERLAY, DEFAULT_OVERLAY)

    private fun sanitizeRenderer(value: String): String =
        value.lowercase().takeIf { it in RENDERERS } ?: DEFAULT_RENDERER

    private fun sanitizeFps(value: Int): Int =
        if (value in TARGET_FPS_OPTIONS) value else DEFAULT_FPS

    companion object {
        const val PREFS = "rex-launcher"
        private const val KEY_JSON = "settings"

        const val KEY_RENDERER = "renderer"
        const val KEY_TARGET_FPS = "targetFps"
        const val KEY_OVERLAY = "overlayVisible"

        const val DEFAULT_RENDERER = "vulkan"
        const val DEFAULT_FPS = 60
        const val DEFAULT_OVERLAY = true

        val RENDERERS = setOf("vulkan", "opengl", "software")
        val TARGET_FPS_OPTIONS = setOf(30, 60, 120)
    }
}
