package com.pushkar.RGM

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    
    // In a real E2E app, keys should be unique per chat and exchanged securely (Diffie-Hellman)
    // For this demonstration, we'll use a derived key based on the Chat ID to ensure 
    // messages only make sense within that specific "Neural Link".
    
    private fun generateKey(chatId: String): SecretKeySpec {
        val bytes = chatId.padEnd(32, '0').substring(0, 32).toByteArray()
        return SecretKeySpec(bytes, "AES")
    }

    private fun generateIv(chatId: String): IvParameterSpec {
        val bytes = chatId.padEnd(16, '0').substring(0, 16).toByteArray()
        return IvParameterSpec(bytes)
    }

    fun encrypt(chatId: String, plainText: String): String {
        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, generateKey(chatId), generateIv(chatId))
            val encryptedBytes = cipher.doFinal(plainText.toByteArray())
            Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
        } catch (e: Exception) {
            plainText
        }
    }

    fun decrypt(chatId: String, encryptedText: String): String {
        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, generateKey(chatId), generateIv(chatId))
            val decodedBytes = Base64.decode(encryptedText, Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            String(decryptedBytes)
        } catch (e: Exception) {
            // If decryption fails, it might be a legacy unencrypted message
            encryptedText
        }
    }
}
