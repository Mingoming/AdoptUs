package com.example.adoptus.util

import android.net.Uri

/**
 * Utility to format whatsapp phone numbers to wa.me URLs.
 */
fun String.formatToWaUrl(petName: String? = null): String {
    val cleanedNum = this.replace(Regex("[^0-9+]"), "")
    val formattedNum = when {
        cleanedNum.startsWith("0") -> "62" + cleanedNum.substring(1)
        cleanedNum.startsWith("+") -> cleanedNum.substring(1)
        else -> cleanedNum
    }
    return if (petName != null) {
        "https://wa.me/$formattedNum?text=Halo, saya tertarik mengadopsi $petName"
    } else {
        "https://wa.me/$formattedNum"
    }
}
