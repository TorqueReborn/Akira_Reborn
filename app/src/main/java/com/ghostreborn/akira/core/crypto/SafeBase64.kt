package com.ghostreborn.akira.core.crypto

/**
 * JVM-safe, pure Kotlin / standard library Base64 utility that runs identically
 * on Android runtime and in JVM host unit tests without relying on android.util.Base64 stubs.
 */
object SafeBase64 {
    fun decode(src: String): ByteArray {
        val clean = src.trim().replace("\n", "").replace("\r", "")
        return java.util.Base64.getDecoder().decode(clean)
    }

    fun encodeToString(src: ByteArray, noWrap: Boolean = true): String {
        val encoded = java.util.Base64.getEncoder().encodeToString(src)
        return if (noWrap) encoded.replace("\n", "").replace("\r", "") else encoded
    }
}
