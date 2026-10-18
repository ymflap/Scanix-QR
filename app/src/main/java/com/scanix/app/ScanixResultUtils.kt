package com.scanix.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Utilities for copying, sharing, and opening decoded scan results.
 */
object ScanixResultUtils {

    fun copyText(
        context: Context,
        text: String
    ) {
        if (text.isBlank()) {
            showMessage(
                context,
                context.getString(R.string.error_generic)
            )
            return
        }

        val clipboard = context.getSystemService(
            Context.CLIPBOARD_SERVICE
        ) as ClipboardManager

        clipboard.setPrimaryClip(
            ClipData.newPlainText("Scanix result", text)
        )

        showMessage(
            context,
            context.getString(R.string.result_copied)
        )
    }

    fun shareText(
        context: Context,
        text: String
    ) {
        if (text.isBlank()) {
            showMessage(
                context,
                context.getString(R.string.error_generic)
            )
            return
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }

        val chooser = Intent.createChooser(
            sendIntent,
            context.getString(R.string.result_share)
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        try {
            context.startActivity(chooser)
        } catch (_: Exception) {
            showMessage(
                context,
                context.getString(R.string.error_generic)
            )
        }
    }

    fun openWebUrl(
        context: Context,
        value: String
    ) {
        val uri = try {
            Uri.parse(value.trim())
        } catch (_: Exception) {
            showMessage(
                context,
                context.getString(R.string.result_no_app_to_open_link)
            )
            return
        }

        if (
            uri.scheme !in listOf("http", "https") ||
            uri.host.isNullOrBlank()
        ) {
            showMessage(
                context,
                context.getString(R.string.result_no_app_to_open_link)
            )
            return
        }

        val intent = Intent(
            Intent.ACTION_VIEW,
            uri
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            showMessage(
                context,
                context.getString(R.string.result_no_app_to_open_link)
            )
        }
    }

    private fun showMessage(
        context: Context,
        message: String
    ) {
        Toast.makeText(
            context,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }
}
