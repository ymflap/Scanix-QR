package com.scanix.app

import android.net.Uri
import java.net.IDN
import java.util.Locale

enum class LinkRiskLevel {
    NORMAL,
    SUSPICIOUS,
    BLOCKED
}

data class LinkSafetyReport(
    val normalizedUrl: String?,
    val riskLevel: LinkRiskLevel,
    val warnings: List<Int>,
    val requiresConfirmation: Boolean
)

object LinkSafetyChecker {

    private val suspiciousWords = setOf(
        "login",
        "verify",
        "verification",
        "password",
        "wallet",
        "banking",
        "security",
        "account",
        "claim",
        "reward",
        "prize",
        "freegift",
        "update"
    )

    fun inspect(rawUrl: String): LinkSafetyReport {
        val input = rawUrl.trim()

        if (input.isBlank()) {
            return blocked(R.string.link_blocked_empty)
        }

        if (
            input.any { it.isWhitespace() || it.isISOControl() } ||
            '\\' in input
        ) {
            return blocked(R.string.link_blocked_invalid_chars)
        }

        val candidate = when {
            input.startsWith("https://", true) -> input
            input.startsWith("http://", true) -> input
            "://" in input -> return blocked(
                R.string.link_blocked_unsupported_type
            )
            else -> "https://$input"
        }

        val uri = try {
            Uri.parse(candidate)
        } catch (_: Exception) {
            return blocked(R.string.link_blocked_invalid_destination)
        }

        val scheme = uri.scheme?.lowercase(Locale.ROOT)
        val host = uri.host

        if (scheme !in listOf("http", "https")) {
            return blocked(R.string.link_blocked_unsupported_type)
        }

        if (host.isNullOrBlank() || uri.userInfo != null) {
            return blocked(R.string.link_blocked_invalid_destination)
        }

        val asciiHost = try {
            IDN.toASCII(host).lowercase(Locale.ROOT)
        } catch (_: Exception) {
            return blocked(R.string.link_blocked_invalid_domain)
        }

        val warnings = mutableListOf<Int>()

        if (isIpAddress(asciiHost)) {
            warnings.add(R.string.link_warning_ip)
        }

        if (asciiHost.contains("xn--")) {
            warnings.add(R.string.link_warning_encoded_domain)
        }

        val hostLabels = asciiHost.split(".")
        val suspiciousHostLabels = hostLabels.filter { label ->
            suspiciousWords.any { word -> label.contains(word) }
        }

        if (
            suspiciousHostLabels.isNotEmpty() &&
            hostLabels.size >= 3
        ) {
            warnings.add(R.string.link_warning_suspicious_terms)
        }

        val fullUrl = uri.toString().lowercase(Locale.ROOT)

        if (
            fullUrl.length > 250 ||
            listOf(
                "redirect=",
                "url=",
                "return=",
                "continue=",
                "target="
            ).count { fullUrl.contains(it) } >= 2
        ) {
            warnings.add(R.string.link_warning_redirect)
        }

        return if (warnings.isEmpty()) {
            LinkSafetyReport(
                normalizedUrl = uri.toString(),
                riskLevel = LinkRiskLevel.NORMAL,
                warnings = emptyList(),
                requiresConfirmation = false
            )
        } else {
            LinkSafetyReport(
                normalizedUrl = uri.toString(),
                riskLevel = LinkRiskLevel.SUSPICIOUS,
                warnings = warnings,
                requiresConfirmation = true
            )
        }
    }

    private fun isIpAddress(host: String): Boolean {
        val parts = host.split(".")

        val ipv4 = parts.size == 4 && parts.all { part ->
            part.isNotEmpty() &&
                part.all(Char::isDigit) &&
                (part.toIntOrNull() in 0..255)
        }

        val ipv6 = host.startsWith("[") && host.endsWith("]")

        return ipv4 || ipv6
    }

    private fun blocked(messageRes: Int) = LinkSafetyReport(
        normalizedUrl = null,
        riskLevel = LinkRiskLevel.BLOCKED,
        warnings = listOf(messageRes),
        requiresConfirmation = true
    )
}
