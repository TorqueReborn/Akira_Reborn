package com.ghostreborn.akira.core.crypto

/**
 * Universal Base64 utility safe for both Android API 24+ and JVM host unit tests.
 * Falls back to Android's android.util.Base64 when java.util.Base64 is unavailable on API < 26.
 */
object SafeBase64 {
    fun decode(src: String): ByteArray {
        val clean = src.trim().replace("\n", "").replace("\r", "")
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            java.util.Base64.getDecoder().decode(clean)
        } else {
            android.util.Base64.decode(clean, android.util.Base64.DEFAULT)
        }
    }

    fun encodeToString(src: ByteArray, noWrap: Boolean = true): String {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val encoded = java.util.Base64.getEncoder().encodeToString(src)
            if (noWrap) encoded.replace("\n", "").replace("\r", "") else encoded
        } else {
            val flags = if (noWrap) android.util.Base64.NO_WRAP else android.util.Base64.DEFAULT
            android.util.Base64.encodeToString(src, flags)
        }
    }
}
