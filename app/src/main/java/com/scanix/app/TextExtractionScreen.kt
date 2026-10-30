package com.scanix.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun TextExtractionScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedLanguage by remember { mutableStateOf("ara") }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var extractedText by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var downloadPercent by remember { mutableIntStateOf(-1) }
    var downloadLanguage by remember { mutableStateOf("") }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    var cameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            cameraPermission = granted
            if (!granted) {
                statusMessage =
                    context.getString(R.string.ocr_camera_permission_denied)
            }
        }

    val imagePicker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) {
                scope.launch {
                    try {
                        val bitmap = withContext(Dispatchers.IO) {
                            context.contentResolver
                                .openInputStream(uri)
                                ?.use { input ->
                                    BitmapFactory.decodeStream(input)
                                }
                        }

                        if (bitmap != null) {
                            selectedBitmap = bitmap
                            extractedText = ""
                            statusMessage =
                                context.getString(R.string.ocr_image_selected)
                        } else {
                            statusMessage =
                                context.getString(R.string.ocr_image_read_failed)
                        }
                    } catch (_: Exception) {
                        statusMessage =
                            context.getString(R.string.ocr_image_open_failed)
                    }
                }
            }
        }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.ocr_title),
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = stringResource(R.string.ocr_choose_language),
            style = MaterialTheme.typography.titleMedium
        )

        LanguageOption(
            label = stringResource(R.string.ocr_language_arabic),
            selected = selectedLanguage == "ara",
            enabled = !isProcessing
        ) {
            selectedLanguage = "ara"
        }

        LanguageOption(
            label = stringResource(R.string.ocr_language_english),
            selected = selectedLanguage == "eng",
            enabled = !isProcessing
        ) {
            selectedLanguage = "eng"
        }

        LanguageOption(
            label = stringResource(R.string.ocr_language_both),
            selected = selectedLanguage == "ara+eng",
            enabled = !isProcessing
        ) {
            selectedLanguage = "ara+eng"
        }

        Text(
            text = stringResource(R.string.ocr_take_photo),
            style = MaterialTheme.typography.titleMedium
        )

        if (cameraPermission) {
            CameraDocumentPreview(
                onImageCaptureReady = { imageCapture = it },
                onCameraError = { message ->
                    statusMessage = message
                }
            )

            Button(
                onClick = {
                    val capture = imageCapture
                    if (capture == null) {
                        statusMessage =
                            context.getString(R.string.ocr_camera_not_ready)
                    } else {
                        takeDocumentPhoto(
                            context = context,
                            capture = capture,
                            onSuccess = { bitmap ->
                                selectedBitmap = bitmap
                                extractedText = ""
                                statusMessage =
                                    context.getString(R.string.ocr_photo_captured)
                            },
                            onError = { message ->
                                statusMessage = message
                            }
                        )
                    }
                },
                enabled = !isProcessing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.ocr_capture_document))
            }
        } else {
            Button(
                onClick = {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.ocr_allow_camera))
            }
        }

        OutlinedButton(
            onClick = { imagePicker.launch("image/*") },
            enabled = !isProcessing,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.ocr_choose_image))
        }

        if (selectedBitmap != null) {
            Text(
                text = stringResource(R.string.ocr_image_ready),
                color = MaterialTheme.colorScheme.primary
            )
        }

        Button(
            onClick = {
                val bitmap = selectedBitmap
                if (bitmap == null) {
                    statusMessage =
                        context.getString(R.string.ocr_capture_or_choose)
                } else {
                    scope.launch {
                        isProcessing = true
                        statusMessage = ""
                        downloadPercent = -1
                        downloadLanguage = ""

                        try {
                            OcrLanguageManager.ensureLanguageData(
                                context = context,
                                languageCode = selectedLanguage
                            ) { progress ->
                                downloadLanguage = progress.languageCode
                                downloadPercent = progress.percent ?: -1
                            }

                            statusMessage =
                                context.getString(R.string.ocr_recognizing)

                            extractedText = withContext(Dispatchers.IO) {
                                OcrLanguageManager.recognizeText(
                                    context = context,
                                    bitmap = bitmap,
                                    languageCode = selectedLanguage
                                )
                            }

                            statusMessage = if (extractedText.isBlank()) {
                                context.getString(R.string.ocr_no_text)
                            } else {
                                context.getString(R.string.ocr_completed)
                            }
                        } catch (e: Exception) {
                            val detail = e.localizedMessage
                                ?: context.getString(R.string.ocr_unknown_error)

                            statusMessage = context.getString(
                                R.string.ocr_failed,
                                detail
                            )
                        } finally {
                            isProcessing = false
                            downloadPercent = -1
                            downloadLanguage = ""
                        }
                    }
                }
            },
            enabled = !isProcessing,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.ocr_extract))
        }

        if (isProcessing) {
            if (downloadPercent >= 0) {
                Text(
                    text = stringResource(
                        R.string.ocr_downloading_progress,
                        downloadPercent
                    )
                )

                LinearProgressIndicator(
                    progress = { downloadPercent / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Text(
                    text = if (downloadLanguage.isNotBlank()) {
                        stringResource(R.string.ocr_downloading_language)
                    } else {
                        stringResource(R.string.ocr_preparing)
                    }
                )
            }
        }

        if (statusMessage.isNotBlank()) {
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Text(
            text = stringResource(R.string.ocr_extracted_text),
            style = MaterialTheme.typography.titleMedium
        )

        OutlinedTextField(
            value = extractedText,
            onValueChange = { extractedText = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            placeholder = {
                Text(stringResource(R.string.ocr_text_placeholder))
            },
            enabled = !isProcessing
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val clipboard =
                        context.getSystemService(
                            Context.CLIPBOARD_SERVICE
                        ) as android.content.ClipboardManager

                    clipboard.setPrimaryClip(
                        android.content.ClipData.newPlainText(
                            "Extracted text",
                            extractedText
                        )
                    )

                    statusMessage =
                        context.getString(R.string.ocr_text_copied)
                },
                enabled = extractedText.isNotBlank() && !isProcessing,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.ocr_copy))
            }
            
            OutlinedButton(
                onClick = {
                    if (extractedText.isNotBlank()) {
                        val sendIntent =
                            android.content.Intent(
                                android.content.Intent.ACTION_SEND
                            ).apply {
                                type = "text/plain"
                                putExtra(
                                    android.content.Intent.EXTRA_TEXT,
                                    extractedText
                                )
                            }

                        context.startActivity(
                            android.content.Intent.createChooser(
                                sendIntent,
                                context.getString(R.string.ocr_share_title)
                            )
                        )
                    }
                },
                enabled = extractedText.isNotBlank() && !isProcessing,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.ocr_share))
            }
        }

        OutlinedButton(
            onClick = {
                extractedText = ""
                statusMessage = ""
            },
            enabled = extractedText.isNotBlank() && !isProcessing,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.ocr_clear_text))
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun LanguageOption(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            enabled = enabled
        )

        Text(
            text = label,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun CameraDocumentPreview(
    onImageCaptureReady: (ImageCapture?) -> Unit,
    onCameraError: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var previewView by remember {
        mutableStateOf<PreviewView?>(null)
    }

    AndroidViewContainer(
        factory = {
            PreviewView(context).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                previewView = this
            }
        }
    )

    val currentView = previewView

    LaunchedEffect(currentView, lifecycleOwner) {
        if (currentView == null) return@LaunchedEffect

        try {
            val provider = withContext(Dispatchers.IO) {
                ProcessCameraProvider.getInstance(context).get()
            }

            val preview = Preview.Builder().build().apply {
                surfaceProvider = currentView.surfaceProvider
            }

            val capture = ImageCapture.Builder()
                .setCaptureMode(
                    ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
                )
                .build()

            withContext(Dispatchers.Main) {
                try {
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        capture
                    )
                    onImageCaptureReady(capture)
                } catch (e: Exception) {
                    onImageCaptureReady(null)
                    onCameraError(
                        e.localizedMessage
                            ?: context.getString(R.string.ocr_camera_start_failed)
                    )
                }
            }
        } catch (e: Exception) {
            onImageCaptureReady(null)
            onCameraError(
                e.localizedMessage
                    ?: context.getString(R.string.ocr_camera_init_failed)
            )
        }
    }
}

@Composable
private fun AndroidViewContainer(
    factory: () -> PreviewView
) {
    androidx.compose.ui.viewinterop.AndroidView(
        factory = { factory() },
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    )
}

private fun takeDocumentPhoto(
    context: Context,
    capture: ImageCapture,
    onSuccess: (Bitmap) -> Unit,
    onError: (String) -> Unit
) {
    val photoFile = File(
        context.cacheDir,
        "ocr_document_${System.currentTimeMillis()}.jpg"
    )

    val outputOptions =
        ImageCapture.OutputFileOptions.Builder(photoFile).build()

    capture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(
                output: ImageCapture.OutputFileResults
            ) {
                try {
                    val bitmap = BitmapFactory.decodeFile(
                        photoFile.absolutePath
                    )

                    if (bitmap != null) {
                        onSuccess(bitmap)
                    } else {
                        onError(
                            context.getString(R.string.ocr_photo_decode_failed)
                        )
                    }
                } catch (e: Exception) {
                    onError(
                        e.localizedMessage
                            ?: context.getString(R.string.ocr_photo_read_failed)
                    )
                } finally {
                    photoFile.delete()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                photoFile.delete()
                onError(
                    exception.localizedMessage
                        ?: context.getString(R.string.ocr_photo_capture_failed)
                )
            }
        }
    )
}
