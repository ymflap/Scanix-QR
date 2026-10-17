package com.scanix.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AboutScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "حول التطبيق",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Scanix QR",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "الإصدار 1.0.3.6",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "أداة لمسح وقراءة رموز QR والباركود " +
                        "باستخدام كاميرا جهازك أو الصور التي تختارها.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        AboutSection(
            title = "مميزات التطبيق",
            body = "• مسح رموز QR والباركود بالكاميرا.\n" +
                "• قراءة الرموز من الصور المختارة.\n" +
                "• مراجعة نتائج المسح في السجل.\n" +
                "• نسخ النتائج ومشاركتها عند الحاجة.\n" +
                "• تخصيص المظهر وإعدادات المسح."
        )

        AboutSection(
            title = "الخصوصية",
            body = "يمكنك التحكم في حفظ نتائج المسح من الإعدادات. " +
                "راجع سياسة الخصوصية لمعرفة تفاصيل التعامل مع البيانات."
        )

        AboutSection(
            title = "المطور",
            body = "Mohammed Nasser"
        )

        AboutSection(
            title = "البريد الإلكتروني",
            body = "dhmlpy@gmail.com"
        )

        AboutSection(
            title = "المصدر على GitHub",
            body = "https://github.com/ymflap/Scanix-QR"
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun AboutSection(
    title: String,
    body: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
