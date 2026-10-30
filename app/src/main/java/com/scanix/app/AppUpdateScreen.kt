package com.scanix.app

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

private const val SCANIX_UPDATE_INFO_URL =
    "https://raw.githubusercontent.com/ymflap/Scanix-QR/main/updates/latest.json"

private const val SCANIX_GITHUB_LATEST_RELEASE_URL =
    "https://api.github.com/repos/ymflap/Scanix-QR/releases/latest"

private data class ScanixUpdateInfo(
    val versionCode: Long?,
    val versionName: String,
    val updateUrl: String,
    val releaseNotes: String
)

@Composable
fun AppUpdateScreen(
    visible: Boolean = true,
    downloadRequestId: Int = 0,
    onUpdateAvailable: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    val currentVersion = remember(context) {
        getInstalledVersion(
            context.packageManager.getPackageInfo(
                context.packageName,
                0
            )
        )
    }

    val updateChannel = BuildConfig.UPDATE_CHANNEL

    var isChecking by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(-1) }
    var updateInfo by remember {
        mutableStateOf<ScanixUpdateInfo?>(null)
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lastHandledDownloadRequest by remember { mutableStateOf(0) }
    var lastNotifiedVersion by remember { mutableStateOf<String?>(null) }

    fun checkForUpdates() {
        if (isChecking || isDownloading) return

        isChecking = true
        errorMessage = null
        updateInfo = null

        Thread {
            var connection: HttpURLConnection? = null

            try {
                val result = if (updateChannel == "github") {
                    connection = URL(SCANIX_GITHUB_LATEST_RELEASE_URL)
                        .openConnection() as HttpURLConnection

                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000
                    connection.requestMethod = "GET"
                    connection.setRequestProperty(
                        "Accept",
                        "application/vnd.github+json"
                    )
                    connection.setRequestProperty(
                        "User-Agent",
                        "Scanix-QR-Android"
                    )
                    connection.setRequestProperty(
                        "Cache-Control",
                        "no-cache"
                    )

                    val responseCode = connection.responseCode

                    if (responseCode !in 200..299) {
                        throw IllegalStateException(
                            "Published GitHub release unavailable"
                        )
                    }

                    val response = connection.inputStream
                        .bufferedReader(Charsets.UTF_8)
                        .use { it.readText() }

                    val release = JSONObject(response)

                    if (
                        release.optBoolean("draft", true) ||
                        release.optBoolean("prerelease", true) ||
                        release.optString("published_at").isBlank()
                    ) {
                        throw IllegalStateException(
                            "No published stable release"
                        )
                    }

                    val versionName = release
                        .optString("tag_name", "")
                        .trim()

                    if (!isSupportedReleaseVersion(versionName)) {
                        throw IllegalStateException(
                            "Invalid published release version"
                        )
                    }

                    val assets = release.optJSONArray("assets")
                        ?: throw IllegalStateException(
                            "Release APK unavailable"
                        )

                    var apkUrl = ""

                    for (index in 0 until assets.length()) {
                        val asset = assets.optJSONObject(index)
                            ?: continue

                        if (
                            asset.optString("name") == "app-release.apk" &&
                            asset.optString("state") == "uploaded"
                        ) {
                            apkUrl = asset.optString(
                                "browser_download_url",
                                ""
                            )
                            break
                        }
                    }

                    if (apkUrl.isBlank()) {
                        throw IllegalStateException(
                            "Published release APK unavailable"
                        )
                    }
                                        val parsedUrl = Uri.parse(apkUrl)

                    if (
                        parsedUrl.scheme != "https" ||
                        parsedUrl.host != "github.com"
                    ) {
                        throw IllegalStateException(
                            "Invalid published APK URL"
                        )
                    }

                    ScanixUpdateInfo(
                        versionCode = null,
                        versionName = versionName,
                        updateUrl = apkUrl,
                        releaseNotes = ""
                    )
                } else {
                    connection = URL(SCANIX_UPDATE_INFO_URL)
                        .openConnection() as HttpURLConnection

                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000
                    connection.requestMethod = "GET"
                    connection.setRequestProperty(
                        "Accept",
                        "application/json"
                    )
                    connection.setRequestProperty(
                        "Cache-Control",
                        "no-cache"
                    )

                    val responseCode = connection.responseCode

                    if (responseCode !in 200..299) {
                        throw IllegalStateException(
                            "Update information unavailable"
                        )
                    }

                    val response = connection.inputStream
                        .bufferedReader(Charsets.UTF_8)
                        .use { it.readText() }

                    val root = JSONObject(response)
                    val channels = root.optJSONObject("channels")
                        ?: throw IllegalStateException(
                            "Missing update information"
                        )

                    val channel = channels.optJSONObject(updateChannel)
                        ?: throw IllegalStateException(
                            "Update information unavailable"
                        )

                    val versionCode = channel.optLong(
                        "versionCode",
                        0L
                    )
                    val versionName = channel.optString(
                        "versionName",
                        ""
                    )
                    val updateUrl = channel.optString(
                        "updateUrl",
                        ""
                    )
                    val releaseNotes = channel.optString(
                        "releaseNotes",
                        ""
                    )

                    if (versionCode <= 0L || versionName.isBlank()) {
                        throw IllegalStateException(
                            "Invalid update information"
                        )
                    }

                    if (updateUrl.isNotBlank()) {
                        val parsedUrl = Uri.parse(updateUrl)

                        if (
                            parsedUrl.scheme != "https" ||
                            parsedUrl.host.isNullOrBlank()
                        ) {
                            throw IllegalStateException(
                                "Invalid update URL"
                            )
                        }
                    }

                    ScanixUpdateInfo(
                        versionCode = versionCode,
                        versionName = versionName,
                        updateUrl = updateUrl,
                        releaseNotes = releaseNotes
                    )
                }

                mainHandler.post {
                    updateInfo = result
                    isChecking = false
                }
            } catch (_: Exception) {
                mainHandler.post {
                    errorMessage = localizedUpdateMessage(
                        context = context,
                        arabic = "تعذر التحقق من التحديث. تحقق من اتصال الإنترنت وحاول مرة أخرى.",
                        english = "Unable to check for updates. Check your internet connection and try again."
                    )
                    isChecking = false
                }
            } finally {
                connection?.disconnect()
            }
        }.start()
    }

    fun downloadAndInstallUpdate() {
        val info = updateInfo ?: return

        if (isDownloading || info.updateUrl.isBlank()) return

        val parsedUrl = Uri.parse(info.updateUrl)

        if (
            parsedUrl.scheme != "https" ||
            parsedUrl.host.isNullOrBlank()
        ) {
            errorMessage = localizedUpdateMessage(
                context = context,
                arabic = "رابط تنزيل التحديث غير صالح.",
                english = "The update download URL is invalid."
            )
            return
        }

        isDownloading = true
        downloadProgress = -1
        errorMessage = null

        Thread {
            var connection: HttpURLConnection? = null
            var apkFile: File? = null

            try {
                val updatesDirectory = File(
                    context.cacheDir,
                    "updates"
                )

                if (
                    !updatesDirectory.exists() &&
                    !updatesDirectory.mkdirs()
                ) {
                    throw IllegalStateException(
                        "Cannot create update directory"
                    )
                }

                apkFile = File(
                    updatesDirectory,
                    "scanix-update.apk"
                )

                connection = URL(info.updateUrl)
                    .openConnection() as HttpURLConnection

                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.requestMethod = "GET"
                connection.instanceFollowRedirects = true
                connection.setRequestProperty(
                    "Accept",
                    "application/vnd.android.package-archive"
                )

                val responseCode = connection.responseCode

                if (responseCode !in 200..299) {
                    throw IllegalStateException(
                        "APK download failed"
                    )
                }

                val contentLength = connection.contentLengthLong
                var downloadedBytes = 0L

                connection.inputStream.use { input ->
                    apkFile.outputStream().buffered().use { output ->
                        val buffer = ByteArray(16 * 1024)
                        var bytesRead: Int

                        while (
                            input.read(buffer).also {
                                bytesRead = it
                            } != -1
                        ) {
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead

                            if (contentLength > 0L) {
                                val progress = (
                                    downloadedBytes * 100L /
                                        contentLength
                                    ).toInt().coerceIn(0, 100)

                                mainHandler.post {
                                    downloadProgress = progress
                                }
                            }
                        }

                        output.flush()
                    }
                }

                if (!apkFile.isFile || apkFile.length() == 0L) {
                    throw IllegalStateException(
                        "Downloaded APK is empty"
                    )
                }

                val archiveInfo = context.packageManager
                    .getPackageArchiveInfo(
                        apkFile.absolutePath,
                        0
                    ) ?: throw IllegalStateException(
                    "Downloaded file is not a valid APK"
                )

                val downloadedVersion = getInstalledVersion(archiveInfo)

                if (archiveInfo.packageName != context.packageName) {
                    throw IllegalStateException(
                        "Downloaded APK package does not match"
                    )
                }

                if (downloadedVersion.first <= currentVersion.first) {
                    throw IllegalStateException(
                        "Downloaded APK is not newer than installed version"
                    )
                }
                                if (
                    info.versionCode == null &&
                    normalizeVersion(downloadedVersion.second) !=
                    normalizeVersion(info.versionName)
                ) {
                    throw IllegalStateException(
                        "Downloaded APK version does not match published release"
                    )
                }

                if (
                    info.versionCode != null &&
                    downloadedVersion.first != info.versionCode
                ) {
                    throw IllegalStateException(
                        "Downloaded APK version does not match update information"
                    )
                }

                mainHandler.post {
                    try {
                        val apkUri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            apkFile
                        )

                        val installIntent = Intent(
                            Intent.ACTION_VIEW
                        ).apply {
                            setDataAndType(
                                apkUri,
                                "application/vnd.android.package-archive"
                            )
                            addFlags(
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }

                        context.startActivity(installIntent)
                        isDownloading = false
                        downloadProgress = 100
                    } catch (_: Exception) {
                        errorMessage = localizedUpdateMessage(
                            context = context,
                            arabic = "تم تنزيل الملف، لكن تعذر بدء تثبيت التحديث. تحقق من السماح بتثبيت التطبيقات من هذا المصدر.",
                            english = "The file was downloaded, but installation could not be started. Check whether installing apps from this source is allowed."
                        )
                        isDownloading = false
                    }
                }
            } catch (_: Exception) {
                apkFile?.delete()

                mainHandler.post {
                    errorMessage = localizedUpdateMessage(
                        context = context,
                        arabic = "تعذر تنزيل ملف التحديث أو التحقق منه. تحقق من اتصال الإنترنت وحاول مرة أخرى.",
                        english = "Unable to download or verify the update. Check your internet connection and try again."
                    )
                    isDownloading = false
                    downloadProgress = -1
                }
            } finally {
                connection?.disconnect()
            }
        }.start()
    }

    LaunchedEffect(updateChannel) {
        checkForUpdates()
    }

    val updateAvailable = updateInfo?.let { info ->
        if (updateChannel == "github") {
            compareVersions(
                info.versionName,
                currentVersion.second
            ) > 0
        } else {
            (info.versionCode ?: 0L) > currentVersion.first
        }
    } == true

    LaunchedEffect(updateAvailable, updateInfo?.versionName) {
        val info = updateInfo
        if (
            updateAvailable &&
            info != null &&
            lastNotifiedVersion != info.versionName
        ) {
            lastNotifiedVersion = info.versionName
            onUpdateAvailable(info.versionName)
        }
    }

    LaunchedEffect(downloadRequestId, updateAvailable, updateInfo?.versionName) {
        val info = updateInfo
        if (
            downloadRequestId > 0 &&
            downloadRequestId != lastHandledDownloadRequest &&
            updateAvailable &&
            info != null
        ) {
            lastHandledDownloadRequest = downloadRequestId
            downloadAndInstallUpdate()
        }
    }

    if (visible) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.update_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.update_current_version),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = displayVersion(currentVersion.second),
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(4.dp))

                when {
                    isDownloading -> {
                        CircularProgressIndicator(
                            progress = {
                                if (downloadProgress >= 0) {
                                    downloadProgress / 100f
                                } else {
                                    0f
                                }
                            }
                        )

                        Text(
                            text = if (downloadProgress >= 0) {
                                "${localizedUpdateMessage(
                                    context = context,
                                    arabic = "جارٍ تنزيل التحديث",
                                    english = "Downloading update"
                                )} $downloadProgress%"
                            } else {
                                localizedUpdateMessage(
                                    context = context,
                                    arabic = "جارٍ تنزيل التحديث...",
                                    english = "Downloading update..."
                                )
                            }
                        )
                    }

                    isChecking -> {
                        CircularProgressIndicator()

                        Text(
                            text = stringResource(R.string.update_checking)
                        )
                    }

                    errorMessage != null -> {
                        Text(
                            text = errorMessage.orEmpty(),
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    updateInfo != null && updateAvailable -> {
                        Text(
                            text = stringResource(
                                R.string.update_available
                            ),
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = displayVersion(updateInfo!!.versionName),
                            style = MaterialTheme.typography.titleMedium
                        )

                        if (updateInfo!!.updateUrl.isNotBlank()) {
                            Button(
                                onClick = {
                                    downloadAndInstallUpdate()
                                },
                                enabled = !isDownloading,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    stringResource(R.string.update_download)
                                )
                            }
                        }
                    }

                    updateInfo != null -> {
                        Text(
                            text = stringResource(
                                R.string.update_not_available
                            ),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
                OutlinedButton(
            onClick = { checkForUpdates() },
            enabled = !isChecking && !isDownloading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                stringResource(R.string.update_check)
            )
        }
    }
    }
}

private fun localizedUpdateMessage(
    context: Context,
    arabic: String,
    english: String
): String {
    val configuration: Configuration =
        context.resources.configuration

    val language = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        configuration.locales[0]?.language.orEmpty()
    } else {
        @Suppress("DEPRECATION")
        configuration.locale?.language.orEmpty()
    }

    return if (language.startsWith("ar", ignoreCase = true)) {
        arabic
    } else {
        english
    }
}

private fun displayVersion(versionName: String): String {
    val cleanVersion = versionName.removePrefix("v").removePrefix("V")
    return "v$cleanVersion"
}

private fun normalizeVersion(versionName: String): String {
    return versionName.trim()
        .removePrefix("v")
        .removePrefix("V")
}

private fun isSupportedReleaseVersion(versionName: String): Boolean {
    return Regex("^[vV]?\\d+(\\.\\d+){3}$")
        .matches(versionName)
}

private fun compareVersions(
    firstVersion: String,
    secondVersion: String
): Int {
    val first = Regex("\\d+")
        .findAll(normalizeVersion(firstVersion))
        .mapNotNull { it.value.toIntOrNull() }
        .toList()

    val second = Regex("\\d+")
        .findAll(normalizeVersion(secondVersion))
        .mapNotNull { it.value.toIntOrNull() }
        .toList()

    val maxParts = maxOf(first.size, second.size)

    for (index in 0 until maxParts) {
        val firstPart = first.getOrElse(index) { 0 }
        val secondPart = second.getOrElse(index) { 0 }

        if (firstPart != secondPart) {
            return firstPart.compareTo(secondPart)
        }
    }

    return 0
}

@Suppress("DEPRECATION")
private fun getInstalledVersion(
    packageInfo: PackageInfo
): Pair<Long, String> {
    val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo.longVersionCode
    } else {
        packageInfo.versionCode.toLong()
    }

    return Pair(
        versionCode,
        packageInfo.versionName ?: versionCode.toString()
    )
}
