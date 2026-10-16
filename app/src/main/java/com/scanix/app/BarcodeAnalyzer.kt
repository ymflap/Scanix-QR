package com.scanix.app

import android.graphics.Bitmap
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

/**
 * Reads QR codes and supported barcodes from a bitmap.
 * Processing happens on the device using ML Kit.
 */
object BarcodeAnalyzer {

    private val scanner by lazy {
        BarcodeScanning.getClient()
    }

    fun analyze(
        bitmap: Bitmap,
        onSuccess: (List<DecodedBarcode>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val image = InputImage.fromBitmap(bitmap, 0)

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                val results = barcodes.mapNotNull { barcode ->
                    val value = barcode.rawValue
                        ?: barcode.displayValue
                        ?: return@mapNotNull null

                    DecodedBarcode(
                        value = value,
                        format = barcode.format
                    )
                }

                onSuccess(results)
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }
}

data class DecodedBarcode(
    val value: String,
    val format: Int
) {
    val formatName: String
        get() = when (format) {
            com.google.mlkit.vision.barcode.common
                .Barcode.FORMAT_QR_CODE -> "QR Code"

            com.google.mlkit.vision.barcode.common
                .Barcode.FORMAT_EAN_13 -> "EAN-13"

            com.google.mlkit.vision.barcode.common
                .Barcode.FORMAT_EAN_8 -> "EAN-8"

            com.google.mlkit.vision.barcode.common
                .Barcode.FORMAT_UPC_A -> "UPC-A"

            com.google.mlkit.vision.barcode.common
                .Barcode.FORMAT_UPC_E -> "UPC-E"

            com.google.mlkit.vision.barcode.common
                .Barcode.FORMAT_CODE_128 -> "Code 128"

            com.google.mlkit.vision.barcode.common
                .Barcode.FORMAT_CODE_39 -> "Code 39"

            com.google.mlkit.vision.barcode.common
                .Barcode.FORMAT_CODE_93 -> "Code 93"

            com.google.mlkit.vision.barcode.common
                .Barcode.FORMAT_CODABAR -> "Codabar"

            com.google.mlkit.vision.barcode.common
                .Barcode.FORMAT_ITF -> "ITF"

            com.google.mlkit.vision.barcode.common
                .Barcode.FORMAT_PDF417 -> "PDF417"

            com.google.mlkit.vision.barcode.common
                .Barcode.FORMAT_AZTEC -> "Aztec"

            com.google.mlkit.vision.barcode.common
                .Barcode.FORMAT_DATA_MATRIX -> "Data Matrix"

            else -> "Barcode"
        }
}
