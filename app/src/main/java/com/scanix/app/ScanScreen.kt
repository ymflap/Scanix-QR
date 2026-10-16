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
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.CameraSelector
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

@Composable
fun ScanScreen(
    onImageSelected: (Uri) -> Unit = {},
    onPermissionGranted: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

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
    var decodedResult by remember { mutableStateOf<DecodedBarcode?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lastSavedValue by remember { mutableStateOf("") }

    val scanner = remember { BarcodeScanning.getClient() }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
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
                errorMessage = "إذن الكاميرا مطلوب للمسح المباشر."
            }
        }

    fun handleDecodedResult(result: DecodedBarcode) {
        decodedResult = result
        errorMessage = null

        if (lastSavedValue != result.value) {
            lastSavedValue = result.value

            if (ScanixSettingsStore.isSaveHistoryEnabled(context)) {
                LocalHistoryStore.save(
                    context = context,
                    value = result.value,
                    format = result.format
                )
            }

            if (ScanixSettingsStore.isVibrationEnabled(context)) {
                try {
                    val vibrator =
                        context.getSystemService(Vibrator::class.java)

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
        }
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
                            val result = barcodes.firstNotNullOfOrNull { barcode ->
                                val value = barcode.rawValue
                                    ?: barcode.displayValue

                                if (value.isNullOrBlank()) {
                                    null
                                } else {
                                    DecodedBarcode(
                                        value = value,
                                        format = barcode.format
                                    )
                                }
                            }

                            if (result != null) {
                                handleDecodedResult(result)
                            } else {
                                errorMessage =
                                    "لم يتم العثور على QR أو باركود في الصورة."
                            }
                        }
                        .addOnFailureListener {
                            errorMessage = "تعذرت قراءة الصورة. جرّب صورة أخرى."
                        }
                } catch (_: Exception) {
                    errorMessage = "تعذر فتح الصورة المختارة."
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
                                it.surfaceProvider = previewView.surfaceProvider
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
                                        val result =
                                            barcodes.firstNotNullOfOrNull { barcode ->
                                                val value = barcode.rawValue
                                                    ?: barcode.displayValue

                                                if (value.isNullOrBlank()) {
                                                    null
                                                } else {
                                                    DecodedBarcode(
                                                        value = value,
                                                        format = barcode.format
                                                    )
                                                }
                                            }

                                        if (result != null) {
                                            handleDecodedResult(result)
                                        }
                                    }
                                    .addOnFailureListener {
                                        errorMessage =
                                            "حدث خطأ أثناء قراءة الرمز."
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
                        errorMessage =
                            "تعذر تشغيل الكاميرا. أغلق التطبيقات التي تستخدمها ثم حاول مجددًا."
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
                errorMessage = "الفلاش غير متاح على هذه الكاميرا."
                flashEnabled = false
            }
        }
    }

    DisposableEffect(scanner) {
        onDispose {
            scanner.close()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Scanix",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "مسح QR والباركود",
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
                            text = "اسمح باستخدام الكاميرا لبدء المسح",
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
                            Text("السماح بالكاميرا")
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
                Text("اختيار صورة")
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
                Text(if (flashEnabled) "إيقاف الفلاش" else "الفلاش")
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

        decodedResult?.let { result ->
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "نتيجة المسح",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = result.formatName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                contentDescription = "نسخ النتيجة"
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
                                        "مشاركة النتيجة"
                                    )
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = "مشاركة النتيجة"
                            )
                        }

                        val isWebLink =
                            result.value.startsWith("https://", true) ||
                            result.value.startsWith("http://", true) ||
                            result.value.startsWith("www.", true)

                        if (isWebLink) {
                            IconButton(
                                onClick = {
                                    val url = if (
                                        result.value.startsWith("www.", true)
                                    ) {
                                        "https://${result.value}"
                                    } else {
                                        result.value
                                    }

                                    try {
                                        context.startActivity(
                                            Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse(url)
                                            )
                                        )
                                    } catch (_: Exception) {
                                        errorMessage =
                                            "تعذر فتح الرابط على هذا الجهاز."
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.OpenInBrowser,
                                    contentDescription = "فتح الرابط"
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "تتم قراءة الرموز على الجهاز، وتُحفظ النتائج محليًا.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
