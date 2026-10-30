package com.scanix.app

import android.content.Intent
import android.content.pm.PackageInfo
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

private data class ScanixUpdateInfo(
    val versionCode: Long,
    val versionName: String,
    val updateUrl: String,
    val releaseNotes: String
)

@Composable
fun AppUpdateScreen() {
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

    fun checkForUpdates() {
        if (isChecking || isDownloading) return

        isChecking = true
        errorMessage = null
        updateInfo = null

        Thread {
            var connection: HttpURLConnection? = null

            try {
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

                val versionCode = channel.optLong("versionCode", 0L)
                val versionName = channel.optString("versionName", "")
                val updateUrl = channel.optString("updateUrl", "")
                val releaseNotes = channel.optString("releaseNotes", "")

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

                val result = ScanixUpdateInfo(
                    versionCode = versionCode,
                    versionName = versionName,
                    updateUrl = updateUrl,
                    releaseNotes = releaseNotes
                )

                mainHandler.post {
                    updateInfo = result
                    isChecking = false
                }
            } catch (_: Exception) {
                mainHandler.post {
                    errorMessage = context.getString(
                        R.string.update_error
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
            errorMessage = context.getString(R.string.update_error)
            return
        }

        isDownloading = true
        downloadProgress = -1
        errorMessage = null

        Thread {
            var connection: HttpURLConnection? = null

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

                val apkFile = File(
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
                        errorMessage = context.getString(
                            R.string.update_error
                        )
                        isDownloading = false
                    }
                }
            } catch (_: Exception) {
                mainHandler.post {
                    errorMessage = context.getString(
                        R.string.update_error
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

    val updateAvailable = updateInfo?.let {
        it.versionCode > currentVersion.first
    } == true

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
                                "${stringResource(R.string.update_checking)} $downloadProgress%"
                            } else {
                                stringResource(R.string.update_checking)
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
                            text = errorMessage!!,
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

                        if (updateInfo!!.releaseNotes.isNotBlank()) {
                            Text(updateInfo!!.releaseNotes)
                        }

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

private fun displayVersion(versionName: String): String {
    val cleanVersion = versionName.removePrefix("v").removePrefix("V")
    return "v$cleanVersion"
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
