package com.example.photoroulette.data.update

import android.content.Context
import com.example.photoroulette.model.AppReleaseInfo
import com.example.photoroulette.utils.VersionNameUtils
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class AppUpdateRepository(
    context: Context,
    private val repoOwner: String = DEFAULT_REPO_OWNER,
    private val repoName: String = DEFAULT_REPO_NAME,
) {
    private val appContext = context.applicationContext
    private val updateCacheDir = File(appContext.cacheDir, UPDATE_CACHE_DIRECTORY)

    suspend fun fetchLatestRelease(): AppReleaseInfo? = withContext(Dispatchers.IO) {
        val apiUrl = URL("https://api.github.com/repos/$repoOwner/$repoName/releases/latest")
        val connection = (apiUrl.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = NETWORK_TIMEOUT_MS
            readTimeout = NETWORK_TIMEOUT_MS
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "PhotoRoulette-Android")
        }

        try {
            val responseCode = connection.responseCode
            when (responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    val payload = connection.inputStream.bufferedReader().use { reader ->
                        reader.readText()
                    }
                    parseReleasePayload(payload)
                }

                HttpURLConnection.HTTP_NOT_FOUND -> null

                else -> {
                    val errorBody = connection.errorStream?.bufferedReader()?.use { reader ->
                        reader.readText()
                    }
                    throw IOException("GitHub release request failed: HTTP $responseCode ${errorBody.orEmpty()}")
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    suspend fun downloadReleaseApk(release: AppReleaseInfo): File = withContext(Dispatchers.IO) {
        val apkDownloadUrl = release.apkDownloadUrl
            ?: throw IOException("No APK asset available for release ${release.tagName}.")

        ensureUpdateCacheDirectory()
        cleanupDownloadedApkPackages()

        val targetFile = File(updateCacheDir, "update-${release.normalizedVersion}.apk")
        val tempFile = File(updateCacheDir, "update-${release.normalizedVersion}.download")

        if (tempFile.exists()) {
            tempFile.delete()
        }

        val connection = (URL(apkDownloadUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = NETWORK_TIMEOUT_MS
            readTimeout = NETWORK_TIMEOUT_MS
            setRequestProperty("Accept", "application/octet-stream")
            setRequestProperty("User-Agent", "PhotoRoulette-Android")
            instanceFollowRedirects = true
        }

        try {
            if (connection.responseCode !in 200..299) {
                throw IOException("Failed to download release APK: HTTP ${connection.responseCode}.")
            }

            connection.inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            if (targetFile.exists()) {
                targetFile.delete()
            }

            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }

            targetFile
        } catch (throwable: Throwable) {
            tempFile.delete()
            throw throwable
        } finally {
            connection.disconnect()
        }
    }

    fun cleanupDownloadedApkPackages() {
        if (!updateCacheDir.exists() || !updateCacheDir.isDirectory) {
            return
        }

        updateCacheDir.listFiles()
            ?.filter { file ->
                file.isFile &&
                    file.name.startsWith("update-") &&
                    (file.name.endsWith(".apk") || file.name.endsWith(".download"))
            }
            ?.forEach { file ->
                file.delete()
            }
    }

    private fun ensureUpdateCacheDirectory() {
        if (!updateCacheDir.exists()) {
            updateCacheDir.mkdirs()
        }
    }

    private fun parseReleasePayload(payload: String): AppReleaseInfo {
        val releaseJson = JSONObject(payload)
        val tagName = releaseJson.optString("tag_name").takeIf { it.isNotBlank() }
            ?: releaseJson.optString("name").ifBlank { "0" }
        val normalizedVersion = VersionNameUtils.normalize(tagName)
        val assets = releaseJson.optJSONArray("assets")
        var apkDownloadUrl: String? = null
        if (assets != null) {
            for (index in 0 until assets.length()) {
                val asset = assets.optJSONObject(index) ?: continue
                val name = asset.optString("name")
                if (name.endsWith(".apk", ignoreCase = true)) {
                    apkDownloadUrl = asset.optString("browser_download_url").takeIf { it.isNotBlank() }
                    if (apkDownloadUrl != null) {
                        break
                    }
                }
            }
        }

        return AppReleaseInfo(
            tagName = tagName,
            normalizedVersion = normalizedVersion,
            title = releaseJson.optString("name").takeIf { it.isNotBlank() },
            notes = releaseJson.optString("body").takeIf { it.isNotBlank() },
            apkDownloadUrl = apkDownloadUrl,
            releasePageUrl = releaseJson.optString("html_url").takeIf { it.isNotBlank() },
        )
    }

    companion object {
        private const val DEFAULT_REPO_OWNER = "JaneJane123654"
        private const val DEFAULT_REPO_NAME = "photoRoulette"
        private const val UPDATE_CACHE_DIRECTORY = "updates"
        private const val NETWORK_TIMEOUT_MS = 15_000
    }
}
