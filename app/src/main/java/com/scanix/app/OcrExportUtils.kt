package com.scanix.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.core.content.ContextCompat
import java.io.IOException

/**
 * Utilities for exporting OCR text to TXT and PDF files.
 * File creation uses a URI supplied by Android's document picker.
 */
object OcrExportUtils {

    @Throws(IOException::class)
    fun saveTextFile(
        context: Context,
        uri: Uri,
        text: String
    ) {
        val output = context.contentResolver.openOutputStream(uri)
            ?: throw IOException("Could not open the selected file.")

        output.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write(text)
        }
    }

    @Throws(IOException::class)
    fun savePdfFile(
        context: Context,
        uri: Uri,
        text: String
    ) {
        val document = PdfDocument()

        try {
            val pageWidth = 595
            val pageHeight = 842
            val margin = 40f
            val contentWidth = pageWidth - (margin * 2).toInt()
            val contentHeight = pageHeight - (margin * 2).toInt()

            val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.BLACK
                textSize = 12f
            }

            val printableText = text.ifEmpty { " " }

            val layout = StaticLayout.Builder
                .obtain(
                    printableText,
                    0,
                    printableText.length,
                    textPaint,
                    contentWidth
                )
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_RTL)
                .setIncludePad(true)
                .build()

            val lineHeight = textPaint.fontMetrics.let {
                it.descent - it.ascent
            }

            val linesPerPage = (
                contentHeight / lineHeight
            ).toInt().coerceAtLeast(1)

            val totalLines = layout.lineCount
            var firstLine = 0
            var pageNumber = 1

            while (firstLine < totalLines) {
                val lastLine = minOf(
                    firstLine + linesPerPage,
                    totalLines
                )

                val startY = layout.getLineTop(firstLine)
                val endY = layout.getLineBottom(lastLine - 1)
                val sectionHeight = endY - startY

                val pageInfo = PdfDocument.PageInfo.Builder(
                    pageWidth,
                    pageHeight,
                    pageNumber
                ).create()

                val page = document.startPage(pageInfo)
                val canvas = page.canvas

                canvas.save()
                canvas.translate(margin, margin - startY)
                canvas.clipRect(
                    0f,
                    startY.toFloat(),
                    contentWidth.toFloat(),
                    (startY + sectionHeight).toFloat()
                )

                layout.draw(canvas)
                canvas.restore()

                document.finishPage(page)

                firstLine = lastLine
                pageNumber++
            }

            val output = context.contentResolver.openOutputStream(uri)
                ?: throw IOException("Could not open the selected PDF file.")

            output.use {
                document.writeTo(it)
            }
        } finally {
            document.close()
        }
    }

    fun copyText(
        context: Context,
        text: String
    ) {
        val clipboard = context.getSystemService(
            Context.CLIPBOARD_SERVICE
        ) as ClipboardManager

        clipboard.setPrimaryClip(
            ClipData.newPlainText("Extracted text", text)
        )
    }

    fun shareText(
        context: Context,
        text: String
    ) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }

        val chooser = Intent.createChooser(
            intent,
            "Share extracted text"
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        context.startActivity(chooser)
    }
}
