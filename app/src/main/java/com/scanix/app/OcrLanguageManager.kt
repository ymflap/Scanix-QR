package com.scanix.app

import android.content.Context
import android.graphics.Bitmap
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages on-demand OCR language downloads and local recognition.
 *
 * Network access is used only when a requested language model is missing
 * or invalid. Recognition itself runs locally on the device.
 */
object OcrLanguageManager {

    private const val BASE_URL =
        "https://raw.githubusercontent.com/tesseract-ocr/tessdata_fast/main/"

    private const val CONNECT_TIMEOUT_MS = 15000
    private const val READ_TIMEOUT_MS = 30000

    private const val MIN_MODEL_SIZE_BYTES = 100 * 1024L

    private val supportedLanguages = setOf("ara", "eng")

    data class DownloadProgress(
        val languageCode: String,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) {
        /**
         * Returns null when the server does not provide a usable total size.
         * The UI must not invent a percentage in that situation.
         */
        val percent: Int?
            get() {
                if (totalBytes <= 0L) return null

                return (
                    downloadedBytes.coerceIn(0L, totalBytes) *
                        100L / totalBytes
                    ).toInt()
            }
    }

    /**
     * Returns the application's private Tesseract data directory.
     * Tesseract expects a tessdata subdirectory inside this directory.
     */
    private fun getDataDirectory(context: Context): File {
        return File(context.filesDir, "tesseract")
    }

    private fun getTessdataDirectory(context: Context): File {
        return File(getDataDirectory(context), "tessdata")
    }

    private fun getModelFile(
        context: Context,
        languageCode: String
    ): File {
        require(languageCode in supportedLanguages) {
            "Unsupported OCR language: $languageCode"
        }

        return File(
            getTessdataDirectory(context),
            "$languageCode.traineddata"
        )
    }

    /**
     * Downloads all requested models if necessary.
     *
     * Supported values: "ara", "eng", and "ara+eng".
     * Progress callbacks report actual downloaded bytes for the current file.
     *
     * Throws IOException when a download or validation fails.
     */
    suspend fun ensureLanguageData(
        context: Context,
        languageCode: String,
        onProgress: (DownloadProgress) -> Unit = {}
    ): Unit = withContext(Dispatchers.IO) {
        val languages = languageCode
            .split("+")
            .map { it.trim() }
            .distinct()

        require(
            languages.isNotEmpty() &&
                languages.all { it in supportedLanguages }
        ) {
            "Unsupported OCR language: $languageCode"
        }

        val appContext = context.applicationContext
        val tessdataDirectory = getTessdataDirectory(appContext)

        if (!tessdataDirectory.exists() &&
            !tessdataDirectory.mkdirs()
        ) {
            throw IOException("Could not create OCR data directory.")
        }

        for (language in languages) {
            val modelFile = getModelFile(appContext, language)

            if (isModelValid(appContext, language, modelFile)) {
                continue
            }

            downloadModel(
                languageCode = language,
                destination = modelFile,
                onProgress = onProgress
            )

            if (!isModelValid(appContext, language, modelFile)) {
                modelFile.delete()
                throw IOException(
                    "The downloaded OCR language file is invalid."
                )
            }
        }
    }

    /**
     * Downloads to a temporary file first, so incomplete downloads
     * are never treated as completed language models.
     */
    private fun downloadModel(
        languageCode: String,
        destination: File,
        onProgress: (DownloadProgress) -> Unit
    ) {
        val temporaryFile = File(
            destination.parentFile,
            "${destination.name}.part"
        )

        var connection: HttpURLConnection? = null

        try {
            temporaryFile.delete()

            connection = (URL(
                "$BASE_URL$languageCode.traineddata"
            ).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                instanceFollowRedirects = true
                setRequestProperty(
                    "User-Agent",
                    "Scanix-QR-Android"
                )
            }

            connection.connect()

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw IOException(
                    "OCR model download failed: HTTP $responseCode"
                )
            }

            val totalBytes = connection.contentLengthLong

            if (totalBytes == 0L) {
                throw IOException("The OCR model file is empty.")
            }

            var downloadedBytes = 0L

            connection.inputStream.use { input ->
                FileOutputStream(temporaryFile).use { output ->
                    val buffer = ByteArray(16 * 1024)

                    while (true) {
                        val count = input.read(buffer)
                        if (count == -1) break

                        output.write(buffer, 0, count)
                        downloadedBytes += count

                        onProgress(
                            DownloadProgress(
                                languageCode = languageCode,
                                downloadedBytes = downloadedBytes,
                                totalBytes = totalBytes
                            )
                        )
                    }

                    output.flush()
                }
            }

            if (downloadedBytes < MIN_MODEL_SIZE_BYTES) {
                throw IOException(
                    "The downloaded OCR model is incomplete."
                )
            }

            if (totalBytes > 0L &&
                downloadedBytes != totalBytes
            ) {
                throw IOException(
                    "The downloaded OCR model size does not match."
                )
            }

            if (destination.exists() && !destination.delete()) {
                throw IOException(
                    "Could not replace the existing OCR model."
                )
            }

            if (!temporaryFile.renameTo(destination)) {
                throw IOException(
                    "Could not save the downloaded OCR model."
                )
            }
        } catch (exception: Exception) {
            temporaryFile.delete()

            if (exception is IOException) {
                throw exception
            }

            throw IOException(
                "Could not download the OCR language model.",
                exception
            )
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Checks file presence and asks Tesseract to initialize the model.
     * This catches files that exist but cannot actually be loaded.
     */
    private fun isModelValid(
        context: Context,
        languageCode: String,
        modelFile: File
    ): Boolean {
        if (!modelFile.isFile ||
            modelFile.length() < MIN_MODEL_SIZE_BYTES
        ) {
            return false
        }

        val engine = try {
            TessBaseAPI()
        } catch (_: Exception) {
            return false
        }

        return try {
            engine.init(
                getDataDirectory(context).absolutePath,
                languageCode
            )
        } catch (_: Exception) {
            false
        } finally {
            engine.recycle()
        }
    }

    /**
     * Recognizes text from a bitmap using already-downloaded models.
     * This method never downloads files or accesses the network.
     */
    suspend fun recognizeText(
        context: Context,
        bitmap: Bitmap,
        languageCode: String
    ): String = withContext(Dispatchers.IO) {
        val languages = languageCode
            .split("+")
            .map { it.trim() }
            .distinct()

        require(
            languages.isNotEmpty() &&
                languages.all { it in supportedLanguages }
        ) {
            "Unsupported OCR language: $languageCode"
        }

        val appContext = context.applicationContext

        for (language in languages) {
            val modelFile = getModelFile(appContext, language)

            if (!modelFile.isFile ||
                modelFile.length() < MIN_MODEL_SIZE_BYTES
            ) {
                throw IOException(
                    "The OCR language files have not been downloaded."
                )
            }
        }

        val engine = TessBaseAPI()

        try {
            val initialized = engine.init(
                getDataDirectory(appContext).absolutePath,
                languages.joinToString("+")
            )

            if (!initialized) {
                throw IOException(
                    "Could not initialize the OCR engine."
                )
            }

            engine.setImage(bitmap)

            engine.getUTF8Text()?.trim().orEmpty()
        } finally {
            engine.recycle()
        }
    }

    /**
     * Reports whether every requested language file is present.
     * A later OCR initialization still validates that Tesseract can load it.
     */
    fun hasLanguageData(
        context: Context,
        languageCode: String
    ): Boolean {
        val languages = languageCode
            .split("+")
            .map { it.trim() }
            .distinct()

        if (
            languages.isEmpty() ||
            languages.any { it !in supportedLanguages }
        ) {
            return false
        }

        return languages.all { language ->
            val file = getModelFile(
                context.applicationContext,
                language
            )

            file.isFile &&
                file.length() >= MIN_MODEL_SIZE_BYTES
        }
    }
}
