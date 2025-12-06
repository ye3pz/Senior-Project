package com.example.shade.utils


object TrustedApps {

    // Exact full package names
    val exact = setOf(
        "com.example.shade",
        "com.spotify.music",
        "com.discord",
        "com.vitastudio.mahjong",
        "com.google.android.apps.youtube.music",
        "com.samsung.android.notes"
    )

    // Trusted vendor prefixes
    val prefixes = listOf(
        "com.google.",
        "com.samsung.",
        "com.android.",
        "com.sec.android.",
        "com.qualcomm.",
        "com.oakever.tiletrip",
        "com.pinterest",
        "com.soulcompany.bubbleshooter.relaxing"
    )
    fun isTrusted(packageName: String): Boolean {
        // Exact match first
        if (exact.contains(packageName)) return true

        // Prefix match
        for (prefix in prefixes) {
            if (packageName.startsWith(prefix)) return true
        }

        return false
    }

    val trustedInstallers = setOf(
        "com.android.vending",                 // Google Play Store
        "com.sec.android.app.samsungapps",     // Samsung Galaxy Store
        "com.amazon.venezia"                   // Amazon Appstore
    )

    fun isTrustedInstaller(installerPackage: String?): Boolean {
        if (installerPackage == null) return false
        return installerPackage in trustedInstallers
    }

}

