package com.photomigrate.app.util

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import java.io.File
import java.io.InputStream
import java.io.OutputStream

class VaultManager(private val context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val vaultDir = File(context.filesDir, "secure_vault").apply { 
        if (!exists()) mkdirs() 
    }

    /**
     * Encrypts and saves a file into the vault.
     */
    fun encryptAndSave(input: InputStream, filename: String): File? {
        val destination = File(vaultDir, "enc_${System.currentTimeMillis()}_$filename")
        if (destination.exists()) destination.delete()

        val encryptedFile = EncryptedFile.Builder(
            context,
            destination,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        return try {
            val output: OutputStream = encryptedFile.openFileOutput()
            input.use { it.copyTo(output) }
            output.close()
            destination
        } catch (e: Exception) {
            if (destination.exists()) destination.delete()
            null
        }
    }

    /**
     * Decrypts a file from the vault and returns an InputStream.
     */
    fun decrypt(file: File): InputStream? {
        if (!file.exists()) return null

        val encryptedFile = EncryptedFile.Builder(
            context,
            file,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        return try {
            encryptedFile.openFileInput()
        } catch (e: Exception) {
            null
        }
    }

    fun deleteFile(path: String) {
        val file = File(path)
        if (file.exists()) file.delete()
    }
}
