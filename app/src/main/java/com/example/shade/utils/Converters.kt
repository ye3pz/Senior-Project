package com.example.shade.utils

fun MalwareScanResponse.toThreatItem(pkgName: String): ThreatItem {
    val score = (malware_confidence * 100).toInt()

    val level = when {
        score >= 50 -> ThreatLevel.HIGH
        score >= 20 -> ThreatLevel.MEDIUM
        else -> ThreatLevel.SAFE
    }

    val dangerList = mutableListOf<String>()
    if (key_risks.isNotEmpty()) dangerList.addAll(key_risks)
    if (indicators.ips.isNotEmpty()) dangerList.add("Suspicious IPs: ${indicators.ips.joinToString()}")
    if (indicators.urls.isNotEmpty()) dangerList.add("Suspicious URLs: ${indicators.urls.joinToString()}")

    if (dangerList.isEmpty()) {
        dangerList.add(summary)
    }

    return ThreatItem(
        title = pkgName,
        description = "Malware Confidence: $malware_confidence%\nVerdict: $verdict",
        dangers = dangerList,
        riskScore = score,
        threatLevel = level,
        source = "APK Upload Scan"
    )
}