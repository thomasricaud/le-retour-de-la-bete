package fr.leretourdelabete.data

import fr.leretourdelabete.BuildConfig
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class AppUpdate(
    val currentVersion: String,
    val latestVersion: String,
    val launchUrl: String,
)

class GitHubReleaseUpdateChecker(
    private val currentVersion: String = BuildConfig.VERSION_NAME,
) {
    suspend fun findAvailableUpdate(): AppUpdate? = withContext(Dispatchers.IO) {
        runCatching { fetchLatestRelease() }.getOrNull()
    }

    private fun fetchLatestRelease(): AppUpdate? {
        val connection = (URL(LATEST_RELEASE_API).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MILLIS
            readTimeout = READ_TIMEOUT_MILLIS
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            setRequestProperty("User-Agent", "Le-Retour-de-la-Bete-Android/$currentVersion")
        }

        return try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null

            val payload = connection.inputStream.bufferedReader().use { it.readText() }
            val release = JSONObject(payload)
            val latestVersion = release.optString("tag_name")
            if (!isVersionNewer(latestVersion, currentVersion)) return null

            val assets = release.optJSONArray("assets")
            val apkUrl = (0 until (assets?.length() ?: 0))
                .asSequence()
                .mapNotNull { assets?.optJSONObject(it) }
                .firstOrNull { it.optString("name") == APK_ASSET_NAME }
                ?.optString("browser_download_url")
            val releaseUrl = release.optString("html_url")
            val launchUrl = sequenceOf(apkUrl, releaseUrl)
                .filterNotNull()
                .firstOrNull(::isTrustedReleaseUrl)
                ?: return null

            AppUpdate(
                currentVersion = currentVersion,
                latestVersion = latestVersion.removePrefix("v").removePrefix("V"),
                launchUrl = launchUrl,
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun isTrustedReleaseUrl(rawUrl: String): Boolean {
        val url = runCatching { URL(rawUrl) }.getOrNull() ?: return false
        return url.protocol == "https" &&
            url.host.equals("github.com", ignoreCase = true) &&
            url.path.startsWith(RELEASE_PATH_PREFIX)
    }

    private companion object {
        const val LATEST_RELEASE_API =
            "https://api.github.com/repos/thomasricaud/le-retour-de-la-bete/releases/latest"
        const val RELEASE_PATH_PREFIX = "/thomasricaud/le-retour-de-la-bete/releases/"
        const val APK_ASSET_NAME = "le-retour-de-la-bete.apk"
        const val CONNECT_TIMEOUT_MILLIS = 2_500
        const val READ_TIMEOUT_MILLIS = 4_000
    }
}

internal fun isVersionNewer(latest: String, current: String): Boolean {
    val latestParts = versionParts(latest) ?: return false
    val currentParts = versionParts(current) ?: return false
    val partCount = maxOf(latestParts.size, currentParts.size)

    repeat(partCount) { index ->
        val latestPart = latestParts.getOrElse(index) { 0 }
        val currentPart = currentParts.getOrElse(index) { 0 }
        if (latestPart != currentPart) return latestPart > currentPart
    }
    return false
}

private fun versionParts(rawVersion: String): List<Int>? {
    val numericVersion = rawVersion
        .trim()
        .removePrefix("v")
        .removePrefix("V")
        .substringBefore('-')
        .substringBefore('+')
    if (numericVersion.isBlank()) return null

    return numericVersion.split('.')
        .map { it.toIntOrNull() ?: return null }
}
