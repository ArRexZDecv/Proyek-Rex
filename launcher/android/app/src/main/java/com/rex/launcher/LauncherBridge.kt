// Dipanggil dari JS sebagai window.Android.<method>(...). Lihat native.call di js/app.js.
package com.rex.launcher

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.MimeTypeMap
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.EnumSet
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class LauncherBridge(
    private val activity: ComponentActivity,
    private val webView: WebView,
    private val settings: LauncherSettings,
) {
    private val prefs = activity.getSharedPreferences(LauncherSettings.PREFS, 0)
    private val io: ExecutorService = Executors.newSingleThreadExecutor()
    private val romIndex = ConcurrentHashMap<String, Uri>()
    private val pressedButtons: MutableSet<PadButton> = EnumSet.noneOf(PadButton::class.java)

    @Volatile
    private var romFolderPath = ""

    @Volatile
    private var pendingCoverGameId: String? = null

    private val folderPicker: ActivityResultLauncher<Uri?> =
        activity.registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { treeUri ->
            if (treeUri != null) onFolderPicked(treeUri)
        }

    private val coverPicker: ActivityResultLauncher<Array<String>> =
        activity.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            val gameId = pendingCoverGameId
            pendingCoverGameId = null
            if (uri != null && gameId != null) importCover(gameId, uri)
        }

    init {
        prefs.getString(KEY_ROM_TREE, null)?.let { stored ->
            val treeUri = Uri.parse(stored)
            romFolderPath = displayPath(treeUri)
            scanAsync(treeUri)
        }
    }

    @JavascriptInterface
    fun pickRomFolder() {
        activity.runOnUiThread { folderPicker.launch(null) }
    }

    @JavascriptInterface
    fun launchGame(romFileName: String): Boolean {
        val entry = findRomEntry(romFileName) ?: run {
            Log.w(TAG, "File ROM tidak ditemukan di indeks: $romFileName")
            return false
        }
        val matchedFileName = entry.key
        val romUri = entry.value
        val watermelon = installedWatermelonPackage() ?: run {
            Log.w(TAG, "WatermelonDS tidak ditemukan pada perangkat")
            return false
        }

        // Susun path file ROM lengkap agar WatermelonDS dapat menentukan direktori penyimpanan save file (.dsv/.sav)
        // dan mencegah save file tersimpan di folder root aplikasi.
        val fullRomPath = if (romFolderPath.isNotEmpty()) {
            "$romFolderPath/$matchedFileName"
        } else {
            matchedFileName
        }

        val intent = Intent(WATERMELON_ACTION_LAUNCH_ROM).apply {
            setClassName(watermelon, WATERMELON_EMULATOR_ACTIVITY)
            setData(romUri)
            putExtra("PATH", fullRomPath)
            putExtra("uri", romUri.toString())
            // Sertakan preferensi konfigurasi emulator
            putExtra("renderer", settings.getRenderer())
            putExtra("targetFps", settings.getTargetFps())
            putExtra("overlayVisible", settings.isOverlayVisible())
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }

        activity.runOnUiThread {
            runCatching {
                activity.grantUriPermission(
                    watermelon,
                    romUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                activity.startActivity(intent)
            }.onFailure { error ->
                Log.w(TAG, "Gagal membuka emulator WatermelonDS", error)
                Toast.makeText(
                    activity,
                    "Gagal membuka emulator: ${error.localizedMessage ?: "Aplikasi tidak ditemukan"}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        return true
    }

    @JavascriptInterface
    fun pickCoverArt(gameId: String) {
        pendingCoverGameId = gameId
        activity.runOnUiThread { coverPicker.launch(arrayOf("image/*")) }
    }

    @JavascriptInterface
    fun loadSettings(): String = settings.read().toString()

    @JavascriptInterface
    fun saveSettings(json: String) {
        runCatching { settings.save(json) }
            .onFailure { Log.w(TAG, "Pengaturan tidak valid", it) }
    }

    @JavascriptInterface
    fun requestInitialState() {
        activity.runOnUiThread {
            publishState()
        }
    }

    @JavascriptInterface
    fun padInput(button: String, pressed: Boolean) {
        val padButton = PadButton.entries.find { it.name == button } ?: return
        if (pressed) {
            pressedButtons.add(padButton)
        } else {
            pressedButtons.remove(padButton)
        }
    }

    fun publishState() {
        if (romFolderPath.isNotEmpty()) {
            push("window.launcher && window.launcher.setRomFolder(${JSONObject.quote(romFolderPath)}, ${romIndex.size})")
            push("window.launcher && window.launcher.setRomFiles(${JSONArray(romIndex.keys.toList())})")
        }
        prefs.all.forEach { (key, value) ->
            if (key.startsWith(COVER_KEY_PREFIX) && value is String) {
                val file = File(coverDir(), value)
                if (file.exists()) publishCover(key.removePrefix(COVER_KEY_PREFIX), file)
            }
        }
    }

    fun pushFps(fps: Int) = push("window.launcher && window.launcher.setFps($fps)")

    private fun findRomEntry(romFileName: String): Map.Entry<String, Uri>? {
        // 1. Pencocokan langsung (exact match)
        romIndex.entries.firstOrNull { it.key == romFileName }?.let { return it }

        // 2. Pencocokan case-insensitive
        romIndex.entries.firstOrNull { it.key.equals(romFileName, ignoreCase = true) }?.let { return it }

        // 3. Pencocokan nama dasar tanpa ekstensi .nds
        val baseName = romFileName.removeSuffix(".nds")
        romIndex.entries.firstOrNull {
            it.key.removeSuffix(".nds").equals(baseName, ignoreCase = true)
        }?.let { return it }

        // 4. Pencocokan substring toleran
        return romIndex.entries.firstOrNull {
            it.key.contains(baseName, ignoreCase = true) || baseName.contains(it.key.removeSuffix(".nds"), ignoreCase = true)
        }
    }

    private fun onFolderPicked(treeUri: Uri) {
        activity.contentResolver.takePersistableUriPermission(
            treeUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        prefs.edit().putString(KEY_ROM_TREE, treeUri.toString()).apply()
        romFolderPath = displayPath(treeUri)
        scanAsync(treeUri)
    }

    private fun scanAsync(treeUri: Uri) {
        io.execute {
            runCatching {
                val root = DocumentFile.fromTreeUri(activity, treeUri) ?: error("Folder ROM tidak valid")
                romIndex.clear()
                collectRoms(root)
            }.onFailure { Log.w(TAG, "Gagal memindai folder ROM", it) }
            publishState()
        }
    }

    private fun collectRoms(dir: DocumentFile) {
        for (child in dir.listFiles()) {
            if (child.isDirectory) {
                collectRoms(child)
                continue
            }
            val name = child.name ?: continue
            if (name.endsWith(".nds", ignoreCase = true)) romIndex[name] = child.uri
        }
    }

    private fun importCover(gameId: String, source: Uri) {
        io.execute {
            runCatching {
                val ext = MimeTypeMap.getSingleton()
                    .getExtensionFromMimeType(activity.contentResolver.getType(source)) ?: "jpg"
                val dir = coverDir().apply { mkdirs() }
                dir.listFiles()?.filter { it.nameWithoutExtension == gameId }?.forEach { it.delete() }

                val target = File(dir, "$gameId.$ext")
                activity.contentResolver.openInputStream(source)?.use { input ->
                    target.outputStream().use { input.copyTo(it) }
                } ?: error("Gambar tidak bisa dibuka")

                prefs.edit().putString(COVER_KEY_PREFIX + gameId, target.name).apply()
                publishCover(gameId, target)
            }.onFailure { Log.w(TAG, "Gagal menyimpan cover $gameId", it) }
        }
    }

    private fun publishCover(gameId: String, file: File) {
        val url = "$ASSET_ORIGIN/$COVER_PATH/${file.name}?v=${file.lastModified()}"
        push("window.launcher && window.launcher.setCover(${JSONObject.quote(gameId)}, ${JSONObject.quote(url)})")
    }

    private fun installedWatermelonPackage(): String? =
        WATERMELON_PACKAGES.firstOrNull { pkg ->
            try {
                activity.packageManager.getPackageInfo(pkg, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            } catch (e: Throwable) {
                false
            }
        }

    private fun displayPath(treeUri: Uri): String {
        val docId = DocumentsContract.getTreeDocumentId(treeUri)
        val volume = docId.substringBefore(':')
        val relative = docId.substringAfter(':', "")
        val base = if (volume == "primary") "/storage/emulated/0" else "/storage/$volume"
        return if (relative.isEmpty()) base else "$base/$relative"
    }

    private fun coverDir(): File = File(activity.filesDir, COVER_PATH)

    private fun push(script: String) {
        activity.runOnUiThread { webView.evaluateJavascript(script, null) }
    }

    companion object {
        const val ASSET_ORIGIN = "https://appassets.androidplatform.net"
        const val COVER_PATH = "covers"

        private const val TAG = "LauncherBridge"
        private const val KEY_ROM_TREE = "romTreeUri"
        private const val COVER_KEY_PREFIX = "cover:"

        // Action resmi dan target activity WatermelonDS sesuai dokumentasi manifest
        private const val WATERMELON_ACTION_LAUNCH_ROM = "me.magnum.melondualds.LAUNCH_ROM"
        private const val WATERMELON_EMULATOR_ACTIVITY = "me.magnum.melonds.ui.emulator.EmulatorActivity"
        private val WATERMELON_PACKAGES = listOf("me.magnum.melondualds", "me.magnum.melondualds.nightly")
    }
}

enum class PadButton { UP, DOWN, LEFT, RIGHT, A, B, X, Y }
