package com.emanuel5014.trainable.data.repository

import android.content.Context
import com.emanuel5014.trainable.data.local.dao.PhysicalCheckDao
import com.emanuel5014.trainable.data.local.entity.PhysicalCheckEntity
import com.emanuel5014.trainable.util.ImageStorageUtils
import com.emanuel5014.trainable.util.PhysicalCheckCryptoManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhysicalCheckRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val physicalCheckDao: PhysicalCheckDao,
    private val cryptoManager: PhysicalCheckCryptoManager,
    private val preferencesRepository: UserPreferencesRepository
) {

    private val folderName = "physical_checks"
    private val validationString = "TRAINABLE_SECURE_VAL"

    // Chiave in memoria per la sessione corrente dell'app
    private var activeSecretKey: SecretKey? = null

    private val filesDirectory: File
        get() = File(context.filesDir, folderName).apply { if (!exists()) mkdirs() }

    fun getAllPhysicalChecks(): Flow<List<PhysicalCheckEntity>> = physicalCheckDao.getAllPhysicalChecks()

    suspend fun getPhysicalCheckById(id: Int): PhysicalCheckEntity? = physicalCheckDao.getPhysicalCheckById(id)

    /**
     * Tenta l'auto-sblocco usando Keystore per decifrare la password salvata.
     * Ritorna true se la chiave viene caricata correttamente, false altrimenti.
     */
    suspend fun tryAutoUnlock(): Boolean = withContext(Dispatchers.IO) {
        if (activeSecretKey != null) return@withContext true
        val isEncryptionEnabled = preferencesRepository.physicalCheckEncryptionEnabled.first()
        if (!isEncryptionEnabled) return@withContext true // Se non attiva, sbloccato per default

        val saltHex = preferencesRepository.physicalCheckEncryptionSalt.first() ?: return@withContext false
        val wrappedPassHex = preferencesRepository.physicalCheckWrappedKeyKeystore.first() ?: return@withContext false
        val ivHex = preferencesRepository.physicalCheckWrappedKeyIv.first() ?: return@withContext false

        try {
            val salt = decodeHex(saltHex)
            val encryptedPass = decodeHex(wrappedPassHex)
            val iv = decodeHex(ivHex)

            val password = cryptoManager.decryptPasswordWithKeystore(encryptedPass, iv)
            if (password != null) {
                activeSecretKey = cryptoManager.deriveKey(password, salt)
                return@withContext true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        false
    }

    /**
     * Sblocca la cassaforte con la password inserita dall'utente.
     */
    suspend fun unlock(password: String): Boolean = withContext(Dispatchers.IO) {
        val saltHex = preferencesRepository.physicalCheckEncryptionSalt.first() ?: return@withContext false
        val validationHex = preferencesRepository.physicalCheckValidationBlock.first() ?: return@withContext false
        val validationIvHex = preferencesRepository.physicalCheckValidationIv.first() ?: return@withContext false

        try {
            val salt = decodeHex(saltHex)
            val validation = decodeHex(validationHex)
            val valIv = decodeHex(validationIvHex)

            val derivedKey = cryptoManager.deriveKey(password, salt)
            
            // Verifica la password provando a decifrare il blocco di validazione
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, valIv)
            cipher.init(Cipher.DECRYPT_MODE, derivedKey, spec)
            val decryptedBytes = cipher.doFinal(validation)
            val decryptedStr = String(decryptedBytes, StandardCharsets.UTF_8)

            if (decryptedStr == validationString) {
                activeSecretKey = derivedKey
                
                // Salva nuovamente la password tramite Keystore sul dispositivo per auto-sblocco futuro
                val (wrapped, newIv) = cryptoManager.encryptPasswordWithKeystore(password)
                preferencesRepository.setPhysicalCheckWrappedKeyKeystore(toHex(wrapped))
                preferencesRepository.setPhysicalCheckWrappedKeyIv(toHex(newIv))
                return@withContext true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        false
    }

    /**
     * Abilita la crittografia. Prende tutti i file in chiaro esistenti e li cifra.
     */
    suspend fun enableEncryption(password: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val salt = cryptoManager.generateSalt()
            val derivedKey = cryptoManager.deriveKey(password, salt)

            // Crea blocco di validazione
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, derivedKey)
            val valBytes = cipher.doFinal(validationString.toByteArray(StandardCharsets.UTF_8))
            val valIv = cipher.iv

            // Salva credenziali e salt
            preferencesRepository.setPhysicalCheckEncryptionSalt(toHex(salt))
            preferencesRepository.setPhysicalCheckValidationBlock(toHex(valBytes))
            preferencesRepository.setPhysicalCheckValidationIv(toHex(valIv))

            // Salva password cifrata via Keystore per uso locale
            val (wrapped, iv) = cryptoManager.encryptPasswordWithKeystore(password)
            preferencesRepository.setPhysicalCheckWrappedKeyKeystore(toHex(wrapped))
            preferencesRepository.setPhysicalCheckWrappedKeyIv(toHex(iv))

            activeSecretKey = derivedKey

            // Cifra i file esistenti
            val directory = filesDirectory
            directory.listFiles()?.forEach { file ->
                if (file.isFile && !file.name.endsWith(".enc")) {
                    val plainBytes = file.readBytes()
                    val encryptedBytes = cryptoManager.encryptData(plainBytes, derivedKey)
                    val encFile = File(directory, file.name + ".enc")
                    encFile.writeBytes(encryptedBytes)
                    file.delete()
                }
            }

            preferencesRepository.setPhysicalCheckEncryptionEnabled(true)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Disabilita la crittografia decifrando tutti i file sul disco.
     */
    suspend fun disableEncryption(password: String): Boolean = withContext(Dispatchers.IO) {
        val unlocked = unlock(password)
        if (!unlocked) return@withContext false

        val key = activeSecretKey ?: return@withContext false

        try {
            val directory = filesDirectory
            directory.listFiles()?.forEach { file ->
                if (file.isFile && file.name.endsWith(".enc")) {
                    val encBytes = file.readBytes()
                    val plainBytes = cryptoManager.decryptData(encBytes, key)
                    val decFile = File(directory, file.name.removeSuffix(".enc"))
                    decFile.writeBytes(plainBytes)
                    file.delete()
                }
            }

            // Pulisci preferenze
            preferencesRepository.setPhysicalCheckEncryptionEnabled(false)
            preferencesRepository.setPhysicalCheckEncryptionSalt(null)
            preferencesRepository.setPhysicalCheckValidationBlock(null)
            preferencesRepository.setPhysicalCheckValidationIv(null)
            preferencesRepository.setPhysicalCheckWrappedKeyKeystore(null)
            preferencesRepository.setPhysicalCheckWrappedKeyIv(null)
            activeSecretKey = null
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Salva una nuova foto per un check fisico.
     */
    suspend fun savePhoto(imageBytes: ByteArray): String = withContext(Dispatchers.IO) {
        val filename = "check_${System.currentTimeMillis()}_${(100..999).random()}.jpg"
        val compressedBytes = ImageStorageUtils.compressImageBytes(imageBytes)
        val isEncryptionEnabled = preferencesRepository.physicalCheckEncryptionEnabled.first()
        val key = activeSecretKey

        if (isEncryptionEnabled && key != null) {
            val encryptedBytes = cryptoManager.encryptData(compressedBytes, key)
            File(filesDirectory, "$filename.enc").writeBytes(encryptedBytes)
        } else {
            File(filesDirectory, filename).writeBytes(compressedBytes)
        }
        filename
    }

    /**
     * Legge una foto dal disco decifrandola se necessario.
     */
    suspend fun getPhotoBytes(filename: String): ByteArray? = withContext(Dispatchers.IO) {
        val isEncryptionEnabled = preferencesRepository.physicalCheckEncryptionEnabled.first()
        
        val encFile = File(filesDirectory, "$filename.enc")
        val plainFile = File(filesDirectory, filename)

        try {
            if (encFile.exists()) {
                val key = activeSecretKey ?: return@withContext null
                val encBytes = encFile.readBytes()
                cryptoManager.decryptData(encBytes, key)
            } else if (plainFile.exists()) {
                if (isEncryptionEnabled) {
                    // Se la crittografia è attiva ma il file è in chiaro, cifralo al volo per sicurezza
                    val key = activeSecretKey
                    if (key != null) {
                        val plainBytes = plainFile.readBytes()
                        val encryptedBytes = cryptoManager.encryptData(plainBytes, key)
                        encFile.writeBytes(encryptedBytes)
                        plainFile.delete()
                        return@withContext plainBytes
                    }
                }
                plainFile.readBytes()
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun insertPhysicalCheck(timestamp: Long, peso: Float?, note: String?, filenames: List<String>) {
        val csv = filenames.joinToString(",")
        physicalCheckDao.insertPhysicalCheck(
            PhysicalCheckEntity(
                timestamp = timestamp,
                peso = peso,
                note = note,
                fotoFilenames = csv
            )
        )
    }

    suspend fun updatePhysicalCheck(check: PhysicalCheckEntity) = withContext(Dispatchers.IO) {
        physicalCheckDao.updatePhysicalCheck(check)
    }

    suspend fun addPhotosToCheck(checkId: Int, newPhotoBytes: List<ByteArray>): List<String> = withContext(Dispatchers.IO) {
        val newFilenames = newPhotoBytes.map { savePhoto(it) }
        val existing = physicalCheckDao.getPhysicalCheckById(checkId) ?: return@withContext newFilenames
        val allFilenames = if (existing.fotoFilenames.isEmpty()) {
            newFilenames
        } else {
            existing.fotoFilenames.split(",") + newFilenames
        }
        physicalCheckDao.updatePhysicalCheck(existing.copy(fotoFilenames = allFilenames.joinToString(",")))
        newFilenames
    }

    suspend fun deletePhotoFromCheck(checkId: Int, filenameToDelete: String) = withContext(Dispatchers.IO) {
        File(filesDirectory, filenameToDelete).delete()
        File(filesDirectory, "$filenameToDelete.enc").delete()
        val existing = physicalCheckDao.getPhysicalCheckById(checkId) ?: return@withContext
        val remainingFilenames = existing.fotoFilenames.split(",").filter { it != filenameToDelete }
        physicalCheckDao.updatePhysicalCheck(existing.copy(fotoFilenames = remainingFilenames.joinToString(",")))
    }

    suspend fun deletePhysicalCheck(check: PhysicalCheckEntity) = withContext(Dispatchers.IO) {
        // Elimina i file delle foto associati
        if (check.fotoFilenames.isNotEmpty()) {
            check.fotoFilenames.split(",").forEach { filename ->
                File(filesDirectory, filename).delete()
                File(filesDirectory, "$filename.enc").delete()
            }
        }
        physicalCheckDao.deletePhysicalCheck(check)
    }

    // Helper conversioni HEX
    private fun toHex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }
    private fun decodeHex(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
