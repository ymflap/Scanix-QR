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
    val warnings: List<String>,
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
            return blocked("الرابط فارغ أو غير صالح.")
        }

        if (
            input.any { it.isWhitespace() || it.isISOControl() } ||
            '\\' in input
        ) {
            return blocked("يحتوي الرابط على أحرف غير صالحة.")
        }

        val candidate = when {
            input.startsWith("https://", true) -> input
            input.startsWith("http://", true) -> input
            "://" in input -> return blocked(
                "نوع الرابط غير مدعوم."
            )
            else -> "https://$input"
        }

        val uri = try {
            Uri.parse(candidate)
        } catch (_: Exception) {
            return blocked("تعذر تحليل الرابط.")
        }

        val scheme = uri.scheme?.lowercase(Locale.ROOT)
        val host = uri.host

        if (scheme !in listOf("http", "https")) {
            return blocked("نوع الرابط غير مسموح.")
        }

        if (host.isNullOrBlank() || uri.userInfo != null) {
            return blocked(
                "تعذر التحقق من وجهة الرابط بوضوح."
            )
        }

        val asciiHost = try {
            IDN.toASCII(host).lowercase(Locale.ROOT)
        } catch (_: Exception) {
            return blocked("اسم النطاق غير صالح.")
        }

        val warnings = mutableListOf<String>()

        // Direct IP addresses deserve additional scrutiny.
        if (isIpAddress(asciiHost)) {
            warnings.add(
                "الرابط يستخدم عنوان IP مباشرًا بدل اسم نطاق معتاد."
            )
        }

        // Internationalized domain names can sometimes imitate other domains.
        if (asciiHost.contains("xn--")) {
            warnings.add(
                "اسم النطاق يحتوي على أحرف دولية مشفّرة؛ تحقق من تهجئته."
            )
        }

        // Suspicious-looking subdomains, e.g. brand-login.example.com.
        val hostLabels = asciiHost.split(".")
        val suspiciousHostLabels = hostLabels.filter { label ->
            suspiciousWords.any { word ->
                label.contains(word)
            }
        }

        if (
            suspiciousHostLabels.isNotEmpty() &&
            hostLabels.size >= 3
        ) {
            warnings.add(
                "اسم النطاق يحتوي على كلمات قد تُستخدم في صفحات تسجيل دخول أو تحقق مزيفة."
            )
        }

        // A long URL with many redirect-related parameters may hide its destination.
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
            warnings.add(
                "الرابط طويل أو يحتوي على مؤشرات تحويل متعددة؛ راجع وجهته قبل المتابعة."
            )
        }

        // HTTP alone is not enough to label a URL suspicious.
        // Ordinary HTTP links are not warned about by this checker.

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

    private fun blocked(message: String) = LinkSafetyReport(
        normalizedUrl = null,
        riskLevel = LinkRiskLevel.BLOCKED,
        warnings = listOf(message),
        requiresConfirmation = true
    )
}
