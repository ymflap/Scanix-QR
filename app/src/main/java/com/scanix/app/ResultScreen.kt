package com.scanix.app

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.content.ClipData
import androidx.core.content.ContextCompat
import android.content.ClipboardManager
import android.widget.Toast

@Composable
fun ResultScreen(
    content: String,
    type: String,
    onSave: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val safeUri = validateWebUrl(content)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "نتيجة المسح",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "نوع الرمز",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = type,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "المحتوى",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        if (safeUri != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "رابط ويب",
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "تحقق من عنوان الموقع قبل فتحه. التحقق من صيغة الرابط لا يضمن أن الموقع آمن.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        text = "النطاق: ${safeUri.host.orEmpty()}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Button(
                        onClick = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                safeUri
                            )
                            if (intent.resolveActivity(
                                    context.packageManager
                                ) != null
                            ) {
                                context.startActivity(intent)
                            } else {
                                Toast.makeText(
                                    context,
                                    "لا يوجد تطبيق لفتح الرابط",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor =
                                MaterialTheme.colorScheme.onSurface,
                            contentColor =
                                MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Text("فتح الرابط")
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val clipboard = context.getSystemService(
                        ClipboardManager::class.java
                    )
                    clipboard?.setPrimaryClip(
                        ClipData.newPlainText("Scanix result", content)
                    )
                    Toast.makeText(
                        context,
                        "تم نسخ النتيجة",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("نسخ")
            }

            OutlinedButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, content)
                    }
                    context.startActivity(
                        Intent.createChooser(intent, "مشاركة النتيجة")
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("مشاركة")
            }
        }

        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onSurface,
                contentColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Text("حفظ في السجل")
        }

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("رجوع")
        }
    }
}

private fun validateWebUrl(value: String): Uri? {
    val candidate = value.trim()
    if (candidate.isEmpty()) return null

    val uri = Uri.parse(candidate)
    val scheme = uri.scheme?.lowercase()
    val host = uri.host

    if (scheme !in setOf("http", "https")) return null
    if (host.isNullOrBlank()) return null
    if (host.any { it.isWhitespace() }) return null

    return uri
}
