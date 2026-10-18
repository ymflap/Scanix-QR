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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PrivacyPolicyScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.privacy_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = stringResource(R.string.privacy_last_updated),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        PrivacySection(
            title = stringResource(R.string.privacy_section_1_title),
            body = stringResource(R.string.privacy_section_1_body)
        )

        PrivacySection(
            title = stringResource(R.string.privacy_section_2_title),
            body = stringResource(R.string.privacy_section_2_body)
        )

        PrivacySection(
            title = stringResource(R.string.privacy_section_3_title),
            body = stringResource(R.string.privacy_section_3_body)
        )

        PrivacySection(
            title = stringResource(R.string.privacy_section_4_title),
            body = stringResource(R.string.privacy_section_4_body)
        )

        PrivacySection(
            title = stringResource(R.string.privacy_section_5_title),
            body = stringResource(R.string.privacy_section_5_body)
        )

        PrivacySection(
            title = stringResource(R.string.privacy_section_6_title),
            body = stringResource(R.string.privacy_section_6_body)
        )

        PrivacySection(
            title = stringResource(R.string.privacy_section_7_title),
            body = stringResource(R.string.privacy_section_7_body)
        )

        PrivacySection(
            title = stringResource(R.string.privacy_section_8_title),
            body = stringResource(R.string.privacy_section_8_body)
        )

        PrivacySection(
            title = stringResource(R.string.privacy_section_9_title),
            body = stringResource(R.string.privacy_section_9_body)
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun PrivacySection(
    title: String,
    body: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
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
