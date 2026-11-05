package com.emanuel5014.trainable.data.repository

import com.emanuel5014.trainable.data.local.dao.PhysicalCheckDao
import com.emanuel5014.trainable.data.local.entity.PhysicalCheckEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlin.time.Clock

/**
 * The physical checks and their photos, optionally encrypted with a password ([PhysicalCheckCrypto]). The photos
 * live in [files]; their names, comma separated, in the check's row.
 */
class PhysicalCheckRepository(
    private val physicalCheckDao: PhysicalCheckDao,
    private val cryptoManager: PhysicalCheckCrypto,
    private val files: PhysicalCheckFiles,
    private val preferencesRepository: UserPreferencesRepository
) {

    private val validationString = "TRAINABLE_SECURE_VAL"

    // Chiave in memoria per la sessione corrente dell'app
    private var activeSecretKey: ByteArray? = null

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

            val password = cryptoManager.unwrapPassword(encryptedPass, iv)
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
            val decryptedBytes = cryptoManager.decryptWithIv(validation, valIv, derivedKey)
            val decryptedStr = decryptedBytes.decodeToString()

            if (decryptedStr == validationString) {
                activeSecretKey = derivedKey

                // Salva nuovamente la password tramite Keystore sul dispositivo per auto-sblocco futuro
                val (wrapped, newIv) = cryptoManager.wrapPassword(password)
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
            val (valBytes, valIv) = cryptoManager.encryptWithIv(validationString.encodeToByteArray(), derivedKey)

            // Salva credenziali e salt
            preferencesRepository.setPhysicalCheckEncryptionSalt(toHex(salt))
            preferencesRepository.setPhysicalCheckValidationBlock(toHex(valBytes))
            preferencesRepository.setPhysicalCheckValidationIv(toHex(valIv))

            // Salva password cifrata via Keystore per uso locale
            val (wrapped, iv) = cryptoManager.wrapPassword(password)
            preferencesRepository.setPhysicalCheckWrappedKeyKeystore(toHex(wrapped))
            preferencesRepository.setPhysicalCheckWrappedKeyIv(toHex(iv))

            activeSecretKey = derivedKey

            // Cifra i file esistenti
            files.list().forEach { name ->
                if (!name.endsWith(".enc")) {
                    val plainBytes = files.read(name)
                    val encryptedBytes = cryptoManager.encryptData(plainBytes, derivedKey)
                    files.write("$name.enc", encryptedBytes)
                    files.delete(name)
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
            files.list().forEach { name ->
                if (name.endsWith(".enc")) {
                    val encBytes = files.read(name)
                    val plainBytes = cryptoManager.decryptData(encBytes, key)
                    files.write(name.removeSuffix(".enc"), plainBytes)
                    files.delete(name)
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
        val filename = "check_${Clock.System.now().toEpochMilliseconds()}_${(100..999).random()}.jpg"
        val compressedBytes = files.compressImage(imageBytes)
        val isEncryptionEnabled = preferencesRepository.physicalCheckEncryptionEnabled.first()
        val key = activeSecretKey

        if (isEncryptionEnabled && key != null) {
            val encryptedBytes = cryptoManager.encryptData(compressedBytes, key)
            files.write("$filename.enc", encryptedBytes)
        } else {
            files.write(filename, compressedBytes)
        }
        filename
    }

    /**
     * Legge una foto dal disco decifrandola se necessario.
     */
    suspend fun getPhotoBytes(filename: String): ByteArray? = withContext(Dispatchers.IO) {
        val isEncryptionEnabled = preferencesRepository.physicalCheckEncryptionEnabled.first()

        val encName = "$filename.enc"

        try {
            if (files.exists(encName)) {
                val key = activeSecretKey ?: return@withContext null
                val encBytes = files.read(encName)
                cryptoManager.decryptData(encBytes, key)
            } else if (files.exists(filename)) {
                if (isEncryptionEnabled) {
                    // Se la crittografia è attiva ma il file è in chiaro, cifralo al volo per sicurezza
                    val key = activeSecretKey
                    if (key != null) {
                        val plainBytes = files.read(filename)
                        val encryptedBytes = cryptoManager.encryptData(plainBytes, key)
                        files.write(encName, encryptedBytes)
                        files.delete(filename)
                        return@withContext plainBytes
                    }
                }
                files.read(filename)
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
        files.delete(filenameToDelete)
        files.delete("$filenameToDelete.enc")
        val existing = physicalCheckDao.getPhysicalCheckById(checkId) ?: return@withContext
        val remainingFilenames = existing.fotoFilenames.split(",").filter { it != filenameToDelete }
        physicalCheckDao.updatePhysicalCheck(existing.copy(fotoFilenames = remainingFilenames.joinToString(",")))
    }

    suspend fun resetAllAndDisableEncryption() = withContext(Dispatchers.IO) {
        // Elimina tutte le foto dal disco
        files.list().forEach { files.delete(it) }
        // Elimina tutti i record dal database
        physicalCheckDao.deleteAll()
        // Pulisci tutte le preferenze di crittografia
        preferencesRepository.setPhysicalCheckEncryptionEnabled(false)
        preferencesRepository.setPhysicalCheckEncryptionSalt(null)
        preferencesRepository.setPhysicalCheckValidationBlock(null)
        preferencesRepository.setPhysicalCheckValidationIv(null)
        preferencesRepository.setPhysicalCheckWrappedKeyKeystore(null)
        preferencesRepository.setPhysicalCheckWrappedKeyIv(null)
        activeSecretKey = null
    }

    suspend fun deletePhysicalCheck(check: PhysicalCheckEntity) = withContext(Dispatchers.IO) {
        // Elimina i file delle foto associati
        if (check.fotoFilenames.isNotEmpty()) {
            check.fotoFilenames.split(",").forEach { filename ->
                files.delete(filename)
                files.delete("$filename.enc")
            }
        }
        physicalCheckDao.deletePhysicalCheck(check)
    }

    companion object {
        // Helper conversioni HEX
        fun toHex(bytes: ByteArray): String =
            bytes.joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }

        fun decodeHex(hex: String): ByteArray {
            val len = hex.length
            val data = ByteArray(len / 2)
            var i = 0
            while (i < len) {
                data[i / 2] = ((hex[i].digitToInt(16) shl 4) + hex[i + 1].digitToInt(16)).toByte()
                i += 2
            }
            return data
        }
    }
}
