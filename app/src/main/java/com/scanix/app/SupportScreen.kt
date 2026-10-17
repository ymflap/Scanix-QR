package com.scanix.app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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

private const val SUPPORT_EMAIL = "dhmlpy@gmail.com"
private const val PROJECT_URL =
    "https://github.com/ymflap/Scanix-QR"

@Composable
fun SupportScreen() {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "الدعم والمساعدة",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "إذا واجهت مشكلة أو أردت إرسال اقتراح، " +
                "يمكنك التواصل عبر البريد الإلكتروني أو " +
                "زيارة مستودع المشروع.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "البريد الإلكتروني",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = SUPPORT_EMAIL,
                    style = MaterialTheme.typography.bodyLarge
                )

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_SENDTO,
                            Uri.parse("mailto:$SUPPORT_EMAIL")
                        ).apply {
                            putExtra(
                                Intent.EXTRA_SUBJECT,
                                "طلب دعم - Scanix QR"
                            )
                        }

                        try {
                            context.startActivity(intent)
                        } catch (_: ActivityNotFoundException) {
                            Toast.makeText(
                                context,
                                "لا يوجد تطبيق بريد إلكتروني مثبت.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                ) {
                    Text("إرسال بريد إلكتروني")
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "مستودع المشروع",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = PROJECT_URL,
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(PROJECT_URL)
                        )

                        try {
                            context.startActivity(intent)
                        } catch (_: ActivityNotFoundException) {
                            Toast.makeText(
                                context,
                                "تعذّر فتح الرابط على هذا الجهاز.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                ) {
                    Text("فتح GitHub")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
