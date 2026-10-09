package io.github.neronguyen.chat.core.security

interface CryptoManager {
    fun encrypt(bytes: ByteArray): ByteArray
    fun decrypt(bytes: ByteArray): ByteArray
}
