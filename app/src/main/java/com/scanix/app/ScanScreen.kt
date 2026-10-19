package com.scanix.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

@Composable
fun ScanScreen(
    onImageSelected: (Uri) -> Unit = {},
    onPermissionGranted: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val lifecycleOwner =
        androidx.lifecycle.compose.LocalLifecycleOwner.current

    var cameraPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var flashEnabled by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // حالة نافذة تحذير الرابط.
    var linkSafetyReport by remember {
        mutableStateOf<LinkSafetyReport?>(null)
    }

    val decodedResults = remember {
        mutableStateListOf<DecodedBarcode>()
    }
    val knownResults = remember { mutableSetOf<String>() }
    val savedHistoryKeys = remember { mutableSetOf<String>() }
    val scanner = remember { BarcodeScanning.getClient() }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    fun openCheckedUrl(url: String) {
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
            )
        } catch (_: Exception) {
            errorMessage = context.getString(
                R.string.scan_open_link_failed
            )
        }
    }

    fun inspectAndOpenLink(rawUrl: String) {
        val report = LinkSafetyChecker.inspect(rawUrl)

        when {
            report.riskLevel == LinkRiskLevel.BLOCKED -> {
                linkSafetyReport = report
            }

            report.requiresConfirmation -> {
                linkSafetyReport = report
            }

            !report.normalizedUrl.isNullOrBlank() -> {
                openCheckedUrl(report.normalizedUrl)
            }

            else -> {
                linkSafetyReport = report
            }
        }
    }

    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
            cameraPermissionGranted = granted

            if (granted) {
                errorMessage = null
                onPermissionGranted()
            } else {
                errorMessage = context.getString(
                    R.string.scan_camera_permission_required
                )
            }
        }

    fun saveResultToHistory(result: DecodedBarcode) {
        val key = "${result.format}:${result.value}"
        if (!savedHistoryKeys.add(key)) return

        if (ScanixSettingsStore.isSaveHistoryEnabled(context)) {
            LocalHistoryStore.save(
                context = context,
                value = result.value,
                format = result.format
            )
        }
    }

    fun vibrateIfEnabled() {
        if (!ScanixSettingsStore.isVibrationEnabled(context)) return

        try {
            val vibrator = context.getSystemService(Vibrator::class.java)

            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        VibrationEffect.createOneShot(
                            100,
                            VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(100)
                }
            }
        } catch (_: Exception) {
            // تجاهل أخطاء الاهتزاز.
        }
    }

    fun convertBarcodes(
        barcodes: List<Barcode>
    ): List<DecodedBarcode> {
        return barcodes.mapNotNull { barcode ->
            val value = barcode.rawValue
                ?: barcode.displayValue
                ?: return@mapNotNull null

            if (value.isBlank()) {
                null
            } else {
                DecodedBarcode(
                    value = value,
                    format = barcode.format
                )
            }
        }.distinctBy { "${it.format}:${it.value}" }
    }

    fun handleDecodedResults(results: List<DecodedBarcode>) {
        if (results.isEmpty()) return

        errorMessage = null
        var foundNewResult = false

        results.forEach { result ->
            val key = "${result.format}:${result.value}"

            if (knownResults.add(key)) {
                decodedResults.add(result)
                foundNewResult = true
            }

            saveResultToHistory(result)
        }

        if (foundNewResult) vibrateIfEnabled()
    }

    val imagePickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->
            if (uri != null) {
                onImageSelected(uri)

                try {
                    val image = InputImage.fromFilePath(context, uri)

                    scanner.process(image)
                        .addOnSuccessListener { barcodes ->
                            val results = convertBarcodes(barcodes)

                            if (results.isNotEmpty()) {
                                handleDecodedResults(results)
                            } else {
                                errorMessage = context.getString(
                                    R.string.scan_image_no_code
                                )
                            }
                        }
                        .addOnFailureListener {
                            errorMessage = context.getString(
                                R.string.scan_image_failed
                            )
                        }
                } catch (_: Exception) {
                    errorMessage = context.getString(
                        R.string.scan_image_open_failed
                    )
                }
            }
        }

    DisposableEffect(
        cameraPermissionGranted,
        lifecycleOwner,
        previewView
    ) {
        var provider: ProcessCameraProvider? = null

        if (cameraPermissionGranted) {
            val providerFuture =
                ProcessCameraProvider.getInstance(context)

            providerFuture.addListener(
                {
                    try {
                        val cameraProvider = providerFuture.get()
                        provider = cameraProvider

                        val preview = Preview.Builder()
                            .build()
                            .also {
                                it.surfaceProvider =
                                    previewView.surfaceProvider
                            }

                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(
                                ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                            )
                            .build()

                        analysis.setAnalyzer(
                            ContextCompat.getMainExecutor(context)
                        ) { imageProxy ->
                            val mediaImage = imageProxy.image

                            if (mediaImage == null) {
                                imageProxy.close()
                            } else {
                                val image = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )

                                scanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        val results =
                                            convertBarcodes(barcodes)

                                        if (results.isNotEmpty()) {
                                            handleDecodedResults(results)
                                        }
                                    }
                                    .addOnFailureListener {
                                        errorMessage = context.getString(
                                            R.string.scan_decode_error
                                        )
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            }
                        }

                        cameraProvider.unbindAll()
                        camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analysis
                        )
                    } catch (_: Exception) {
                        errorMessage = context.getString(
                            R.string.scan_camera_start_failed
                        )
                    }
                },
                ContextCompat.getMainExecutor(context)
            )
        } else {
            camera = null
        }

        onDispose {
            camera = null
            provider?.unbindAll()
        }
    }

    LaunchedEffect(camera, flashEnabled) {
        try {
            camera?.cameraControl?.enableTorch(flashEnabled)
        } catch (_: Exception) {
            if (flashEnabled) {
                errorMessage = context.getString(
                    R.string.scan_flash_unavailable
                )
                flashEnabled = false
            }
        }
    }

    DisposableEffect(scanner) {
        onDispose { scanner.close() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(R.string.scan_title),
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline
            ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (cameraPermissionGranted) {
                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = stringResource(
                                R.string.scan_permission_prompt
                            ),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                cameraPermissionLauncher.launch(
                                    Manifest.permission.CAMERA
                                )
                            }
                        ) {
                            Text(
                                stringResource(R.string.scan_allow_camera)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = {
                    imagePickerLauncher.launch("image/*")
                },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Image,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(stringResource(R.string.scan_image))
            }

            OutlinedButton(
                onClick = {
                    if (!cameraPermissionGranted) {
                        cameraPermissionLauncher.launch(
                            Manifest.permission.CAMERA
                        )
                    } else {
                        flashEnabled = !flashEnabled
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.FlashlightOn,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    stringResource(
                        if (flashEnabled) {
                            R.string.scan_flash_off
                        } else {
                            R.string.scan_flash_on
                        }
                    )
                )
            }
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = errorMessage.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }

        if (decodedResults.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "${decodedResults.size} نتيجة",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            decodedResults.forEach { result ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = result.formatName,
                            style = MaterialTheme.typography.labelMedium,
                            color =
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = result.value,
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(
                                onClick = {
                                    clipboard.setText(
                                        AnnotatedString(result.value)
                                    )
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = stringResource(
                                        R.string.scan_result_copy
                                    )
                                )
                            }

                            IconButton(
                                onClick = {
                                    val sendIntent = Intent(
                                        Intent.ACTION_SEND
                                    ).apply {
                                        type = "text/plain"
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            result.value
                                        )
                                    }

                                    context.startActivity(
                                        Intent.createChooser(
                                            sendIntent,
                                            context.getString(
                                                R.string.scan_result_share
                                            )
                                        )
                                    )
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Share,
                                    contentDescription = stringResource(
                                        R.string.scan_result_share
                                    )
                                )
                            }

                            val isWebLink =
                                result.value.startsWith(
                                    "https://", true
                                ) ||
                                result.value.startsWith(
                                    "http://", true
                                ) ||
                                result.value.startsWith("www.", true)

                            if (isWebLink) {
                                IconButton(
                                    onClick = {
                                        val url =
                                            if (result.value.startsWith(
                                                    "www.", true
                                                )
                                            ) {
                                                "https://${result.value}"
                                            } else {
                                                result.value
                                            }

                                        // لا يُفتح الرابط مباشرة.
                                        // يُفحص أولًا باستخدام LinkSafetyChecker.
                                        inspectAndOpenLink(url)
                                    }
                                ) {
                                    Icon(
                                        imageVector =
                                            Icons.Outlined.OpenInBrowser,
                                        contentDescription = stringResource(
                                            R.string.result_open_link
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.scan_privacy_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }

    // نافذة تأكيد أمان الرابط.
    val report = linkSafetyReport

    if (report != null) {
        val isBlocked = report.riskLevel == LinkRiskLevel.BLOCKED

        AlertDialog(
            onDismissRequest = {
                linkSafetyReport = null
            },
            title = {
                Text(
                    text = stringResource(
                        if (isBlocked) {
                            R.string.link_warning_title
                        } else {
                            R.string.link_warning_title
                        }
                    )
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(
                            if (isBlocked) {
                                R.string.link_warning_message
                            } else {
                                R.string.link_warning_message
                            }
                        )
                    )

                    report.warnings.forEach { warningRes ->
                        Text(
                            text = context.getString(warningRes),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                if (!isBlocked && !report.normalizedUrl.isNullOrBlank()) {
                    TextButton(
                        onClick = {
                            val url = report.normalizedUrl
                            linkSafetyReport = null
                            openCheckedUrl(url)
                        }
                    ) {
                        Text(stringResource(R.string.link_open_anyway))
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        linkSafetyReport = null
                    }
                ) {
                    Text(stringResource(R.string.link_cancel))
                }
            }
        )
    }
}
