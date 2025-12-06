package com.example.shade.utils

enum class ThreatLevel {
    SAFE,
    MEDIUM,
    HIGH
}

data class ThreatItem(
    val title: String,
    val description: String,
    val dangers: List<String> = emptyList(),
    val riskScore: Int = 0,
    val threatLevel: ThreatLevel = ThreatLevel.SAFE,
    val source: String = ""
)



// Data for each scanned app row
data class HistoryItem(
    val name: String,
    val dateTime: String,
    val threatLevel: ThreatLevel,
    val description: String,
    val dangerList: List<String>,
    val source: String,             // Quick Scan / Full Scan / Firebase etc.
    val riskScore: Int
)

data class MalwareScanResponse(
    val malware_confidence: Double,
    val verdict: String,
    val decision_threshold: Int,
    val summary: String,
    val analysis_log: List<String>,
    val indicators: Indicators,
    val key_risks: List<String>,
    val full_report_text: String
)

data class Indicators(
    val ips: List<String>,
    val urls: List<String>
)


