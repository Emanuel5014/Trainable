package com.emanuel5014.trainable.data.repository

import com.emanuel5014.trainable.data.local.dao.PhysicalCheckDao
import com.emanuel5014.trainable.data.local.entity.PhysicalCheckEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.time.Clock

/**
 * The physical checks and their photos, optionally locked in a vault with a password ([PhysicalCheckCrypto]).
 *
 * With the vault on, each photo is a `<name>.enc` file and the weight and note of a check are sealed in its
 * [PhysicalCheckEntity.secret]; only the date stays readable (it orders the list). Rows handed out by this class
 * always carry the weight and note in clear when the key is loaded.
 *
 * Turning the vault on or off, and changing its password, never deletes the old copy of anything before the new
 * one is written, so an interrupted change leaves both copies; [finishPendingWork] keeps the right one the next time
 * the vault opens (and also drops photos no check refers to any more, e.g. after a backup was imported).
 */
class PhysicalCheckRepository(
    private val physicalCheckDao: PhysicalCheckDao,
    private val cryptoManager: PhysicalCheckCrypto,
    private val files: PhysicalCheckFiles,
    private val preferencesRepository: UserPreferencesRepository
) {

    private val validationString = "TRAINABLE_SECURE_VAL"

    // The vault's key while it is open in this process
    private var activeSecretKey: ByteArray? = null

    // Bumped whenever the key changes, so the list decrypts its rows again
    private val keyVersion = MutableStateFlow(0)

    // Vault changes, clean-ups and "save photos then the row" run one at a time, so a clean-up never deletes a photo
    // whose row is about to be written
    private val vaultLock = Mutex()

    /** The fingerprint/PIN session of the whole section, shared by its screens (list, compare, settings). */
    val session = PhysicalCheckSession()

    private fun setKey(key: ByteArray?) {
        activeSecretKey = key
        keyVersion.value++
    }

    private suspend fun isVaultOn(): Boolean = preferencesRepository.physicalCheckEncryptionEnabled.first()

    fun getAllPhysicalChecks(): Flow<List<PhysicalCheckEntity>> =
        combine(physicalCheckDao.getAllPhysicalChecks(), keyVersion) { rows, _ -> rows.map(::reveal) }
            .flowOn(Dispatchers.IO)

    suspend fun getPhysicalCheckById(id: Int): PhysicalCheckEntity? =
        physicalCheckDao.getPhysicalCheckById(id)?.let(::reveal)

    // --- Weight and note in the secret ---

    private fun reveal(row: PhysicalCheckEntity): PhysicalCheckEntity = revealWith(row, activeSecretKey)

    /**
     * The row with its weight and note decrypted with [key] (secret null); unchanged, secret kept, when it has no
     * secret, there is no key or the key does not open it.
     */
    private fun revealWith(row: PhysicalCheckEntity, key: ByteArray?): PhysicalCheckEntity {
        val secret = row.secret ?: return row
        if (key == null) return row
        return try {
            val (peso, note) = PhysicalCheckSecret.decode(cryptoManager.decryptData(decodeHex(secret), key).decodeToString())
            row.copy(peso = peso, note = note, secret = null)
        } catch (e: Exception) {
            row
        }
    }

    /** The row as stored with the vault on: weight and note sealed with [key], none of them in clear. */
    private fun sealWith(row: PhysicalCheckEntity, key: ByteArray): PhysicalCheckEntity =
        if (row.peso == null && row.note == null) row.copy(secret = null)
        else row.copy(
            peso = null,
            note = null,
            secret = toHex(cryptoManager.encryptData(PhysicalCheckSecret.encode(row.peso, row.note).encodeToByteArray(), key))
        )

    /** A revealed row as it must be stored now: sealed with the vault on, in clear with it off. */
    private suspend fun stored(row: PhysicalCheckEntity): PhysicalCheckEntity {
        if (!isVaultOn()) return row.copy(secret = null)
        val key = activeSecretKey ?: error("Vault locked")
        return sealWith(row, key)
    }

    // --- Unlocking ---

    /**
     * Opens the vault with the password this device saved (Keystore / Keychain). False when the vault is on and the
     * password has to be typed.
     */
    suspend fun tryAutoUnlock(): Boolean = withContext(Dispatchers.IO) {
        if (activeSecretKey != null) return@withContext true
        if (!isVaultOn()) {
            finishPendingWork()
            return@withContext true
        }

        val saltHex = preferencesRepository.physicalCheckEncryptionSalt.first() ?: return@withContext false
        val wrappedPassHex = preferencesRepository.physicalCheckWrappedKeyKeystore.first() ?: return@withContext false
        val ivHex = preferencesRepository.physicalCheckWrappedKeyIv.first() ?: return@withContext false

        try {
            val password = cryptoManager.unwrapPassword(decodeHex(wrappedPassHex), decodeHex(ivHex))
            if (password != null) {
                val key = cryptoManager.deriveKey(password, decodeHex(saltHex))
                // The saved password may belong to another vault (an imported backup with another password):
                // only a key that opens this vault's validation block unlocks, else the password is asked
                if (opensValidationBlock(key)) {
                    setKey(key)
                    finishPendingWork()
                    return@withContext true
                }
                preferencesRepository.setPhysicalCheckWrappedKeyKeystore(null)
                preferencesRepository.setPhysicalCheckWrappedKeyIv(null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        false
    }

    /** Whether [key] opens this vault's validation block (the password is right for these photos). */
    private suspend fun opensValidationBlock(key: ByteArray): Boolean {
        val validationHex = preferencesRepository.physicalCheckValidationBlock.first() ?: return false
        val validationIvHex = preferencesRepository.physicalCheckValidationIv.first() ?: return false
        return try {
            cryptoManager.decryptWithIv(decodeHex(validationHex), decodeHex(validationIvHex), key).decodeToString() == validationString
        } catch (e: Exception) {
            false
        }
    }

    /** The key of [password] when it opens this vault, else null. */
    private suspend fun keyFor(password: String): ByteArray? {
        val saltHex = preferencesRepository.physicalCheckEncryptionSalt.first() ?: return null
        return try {
            cryptoManager.deriveKey(password, decodeHex(saltHex)).takeIf { opensValidationBlock(it) }
        } catch (e: Exception) {
            null
        }
    }

    /** Opens the vault with the password typed by the user and saves it on this device for the next time. */
    suspend fun unlock(password: String): Boolean = withContext(Dispatchers.IO) {
        val key = keyFor(password) ?: return@withContext false
        setKey(key)
        try {
            val (wrapped, newIv) = cryptoManager.wrapPassword(password)
            preferencesRepository.setPhysicalCheckWrappedKeyKeystore(toHex(wrapped))
            preferencesRepository.setPhysicalCheckWrappedKeyIv(toHex(newIv))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        finishPendingWork()
        true
    }

    // --- Turning the vault on and off, changing its password ---

    /**
     * Turns the vault on: the keys are saved first, every photo gets its encrypted copy, then the vault is marked on,
     * the rows are sealed and only at the end the plain photos go.
     */
    suspend fun enableEncryption(password: String): Boolean = withContext(Dispatchers.IO) {
        vaultLock.withLock {
            try {
                val salt = cryptoManager.generateSalt()
                val key = cryptoManager.deriveKey(password, salt)
                val (valBytes, valIv) = cryptoManager.encryptWithIv(validationString.encodeToByteArray(), key)
                val (wrapped, wrappedIv) = cryptoManager.wrapPassword(password)
                preferencesRepository.setPhysicalCheckVaultKeys(toHex(salt), toHex(valBytes), toHex(valIv), toHex(wrapped), toHex(wrappedIv))

                files.list().filter { isPhotoName(it) }.forEach { name ->
                    files.write("$name.enc", cryptoManager.encryptData(files.read(name), key))
                }
                preferencesRepository.setPhysicalCheckEncryptionEnabled(true)
                setKey(key)
                finishPendingWorkLocked()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    /**
     * Turns the vault off: every photo gets its plain copy and the rows are opened, then the vault is marked off and
     * only at the end the encrypted photos go.
     */
    suspend fun disableEncryption(password: String): Boolean = withContext(Dispatchers.IO) {
        vaultLock.withLock {
            val key = keyFor(password) ?: return@withLock false
            try {
                setKey(key)
                files.list().filter { it.endsWith(ENC) }.forEach { name ->
                    val plain = name.removeSuffix(ENC)
                    if (!files.exists(plain)) files.write(plain, cryptoManager.decryptData(files.read(name), key))
                }
                val sealed = physicalCheckDao.getAllOnce().filter { it.secret != null }
                val opened = sealed.map { revealWith(it, key) }
                // A row the key cannot open would lose its weight and note: stop, the vault stays on
                check(opened.none { it.secret != null }) { "A check cannot be opened" }
                physicalCheckDao.updateAll(opened)

                preferencesRepository.setPhysicalCheckEncryptionEnabled(false)
                preferencesRepository.setPhysicalCheckVaultKeys(null, null, null, null, null)
                setKey(null)
                finishPendingWorkLocked()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    /**
     * A new password for the vault: every photo is encrypted again into a `.enc.new` copy and every row sealed again,
     * then the new keys are saved and the copies replace the old files. Until the keys are saved the old password
     * stays valid; after, a copy left behind is put in place by [finishPendingWork].
     */
    suspend fun changePassword(oldPassword: String, newPassword: String): Boolean = withContext(Dispatchers.IO) {
        vaultLock.withLock {
            if (!isVaultOn()) return@withLock false
            val oldKey = keyFor(oldPassword) ?: return@withLock false
            try {
                val salt = cryptoManager.generateSalt()
                val newKey = cryptoManager.deriveKey(newPassword, salt)
                val (valBytes, valIv) = cryptoManager.encryptWithIv(validationString.encodeToByteArray(), newKey)
                val (wrapped, wrappedIv) = cryptoManager.wrapPassword(newPassword)

                val encrypted = files.list().filter { it.endsWith(ENC) }
                encrypted.forEach { name ->
                    files.write("$name$NEW", cryptoManager.encryptData(cryptoManager.decryptData(files.read(name), oldKey), newKey))
                }
                val rows = physicalCheckDao.getAllOnce()
                val resealed = rows.filter { it.secret != null || it.peso != null || it.note != null }.map { row ->
                    val opened = revealWith(row, oldKey)
                    check(opened.secret == null) { "A check cannot be opened" }
                    sealWith(opened, newKey)
                }

                preferencesRepository.setPhysicalCheckVaultKeys(toHex(salt), toHex(valBytes), toHex(valIv), toHex(wrapped), toHex(wrappedIv))
                physicalCheckDao.updateAll(resealed)
                setKey(newKey)
                encrypted.forEach { name -> files.move("$name$NEW", name) }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                // The keys were not switched: drop the copies, the old password stays
                files.list().filter { it.endsWith(NEW) }.forEach { files.delete(it) }
                setKey(oldKey)
                false
            }
        }
    }

    // --- Clean-up ---

    /**
     * Finishes what an interrupted vault change left and removes photos no check refers to. Runs when the vault
     * opens (or at start with the vault off).
     */
    suspend fun finishPendingWork() = withContext(Dispatchers.IO) {
        vaultLock.withLock { finishPendingWorkLocked() }
    }

    private suspend fun finishPendingWorkLocked() {
        try {
            val vaultOn = isVaultOn()
            val key = activeSecretKey
            if (vaultOn && key == null) return
            val names = files.list()

            names.filter { it.endsWith(TMP) }.forEach { files.delete(it) }
            names.filter { it.endsWith(NEW) }.forEach { name ->
                // A copy of a password change: in place only if the current key opens it (the keys were switched)
                val opens = key != null && try {
                    cryptoManager.decryptData(files.read(name), key); true
                } catch (e: Exception) {
                    false
                }
                if (opens) files.move(name, name.removeSuffix(NEW)) else files.delete(name)
            }

            val current = files.list().toSet()
            current.filter { isPhotoName(it) }.forEach { plain ->
                val enc = "$plain$ENC"
                if (vaultOn) {
                    if (enc !in current) files.write(enc, cryptoManager.encryptData(files.read(plain), key!!))
                    files.delete(plain)
                } else if (enc in current) {
                    files.delete(enc)
                }
            }

            // Rows: sealed with the vault on, in clear with it off
            val rows = physicalCheckDao.getAllOnce()
            if (vaultOn) {
                physicalCheckDao.updateAll(rows.filter { it.secret == null && (it.peso != null || it.note != null) }.map { sealWith(it, key!!) })
            } else if (key != null) {
                // Only rows the key really opens (secret gone), never dropping a weight or note it cannot read
                physicalCheckDao.updateAll(rows.filter { it.secret != null }.map { revealWith(it, key) }.filter { it.secret == null })
            }

            // Photos no check refers to (left by an import or an interrupted delete). Skipped when there is no row
            // at all, so a database that failed to load never costs the photos.
            if (rows.isNotEmpty()) {
                val referenced = rows.flatMap { photoNames(it) }.toSet()
                files.list().forEach { name ->
                    if (baseName(name) !in referenced) files.delete(name)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- Photos ---

    /**
     * Saves a new photo of a check: encrypted with the vault on (never in clear while it is on), else plain. Called
     * with [vaultLock] held, together with writing the row.
     */
    private suspend fun savePhotoLocked(imageBytes: ByteArray): String {
        val filename = "check_${Clock.System.now().toEpochMilliseconds()}_${(100..999).random()}.jpg"
        val compressedBytes = files.compressImage(imageBytes)
        if (isVaultOn()) {
            val key = activeSecretKey ?: error("Vault locked")
            files.write("$filename$ENC", cryptoManager.encryptData(compressedBytes, key))
        } else {
            files.write(filename, compressedBytes)
        }
        return filename
    }

    /**
     * A photo for display. Only reads: with both copies of a photo on disk (an interrupted vault change) it takes
     * the one that matches the vault, and [finishPendingWork] removes the other.
     */
    suspend fun getPhotoBytes(filename: String): ByteArray? = withContext(Dispatchers.IO) {
        val encName = "$filename$ENC"
        try {
            val key = activeSecretKey
            if (isVaultOn()) {
                when {
                    key == null -> null
                    files.exists(encName) -> cryptoManager.decryptData(files.read(encName), key)
                    files.exists(filename) -> files.read(filename)
                    else -> null
                }
            } else {
                when {
                    files.exists(filename) -> files.read(filename)
                    files.exists(encName) && key != null -> cryptoManager.decryptData(files.read(encName), key)
                    else -> null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** A new check with its photos, saved together so a clean-up never sees the photos without their row. */
    suspend fun addCheck(timestamp: Long, peso: Float?, note: String?, photos: List<ByteArray>) = withContext(Dispatchers.IO) {
        vaultLock.withLock {
            val filenames = photos.map { savePhotoLocked(it) }
            val row = PhysicalCheckEntity(timestamp = timestamp, peso = peso, note = note, fotoFilenames = filenames.joinToString(","))
            physicalCheckDao.insertPhysicalCheck(stored(row))
        }
    }

    /** Saves the check's date, weight and note ([check] as handed out, weight and note in clear). */
    suspend fun updatePhysicalCheck(check: PhysicalCheckEntity) = withContext(Dispatchers.IO) {
        vaultLock.withLock {
            val current = physicalCheckDao.getPhysicalCheckById(check.id) ?: return@withLock
            // The photo list is the database's: it may have changed while the check was being edited
            physicalCheckDao.updatePhysicalCheck(stored(check.copy(fotoFilenames = current.fotoFilenames)))
        }
    }

    suspend fun addPhotosToCheck(checkId: Int, newPhotoBytes: List<ByteArray>): List<String> = withContext(Dispatchers.IO) {
        vaultLock.withLock {
            val newFilenames = newPhotoBytes.map { savePhotoLocked(it) }
            val existing = physicalCheckDao.getPhysicalCheckById(checkId) ?: return@withLock newFilenames
            val allFilenames = photoNames(existing) + newFilenames
            physicalCheckDao.updatePhysicalCheck(existing.copy(fotoFilenames = allFilenames.joinToString(",")))
            newFilenames
        }
    }

    suspend fun deletePhotoFromCheck(checkId: Int, filenameToDelete: String) = withContext(Dispatchers.IO) {
        vaultLock.withLock {
            files.delete(filenameToDelete)
            files.delete("$filenameToDelete$ENC")
            val existing = physicalCheckDao.getPhysicalCheckById(checkId) ?: return@withLock
            val remainingFilenames = photoNames(existing).filter { it != filenameToDelete }
            physicalCheckDao.updatePhysicalCheck(existing.copy(fotoFilenames = remainingFilenames.joinToString(",")))
        }
    }

    suspend fun resetAllAndDisableEncryption() = withContext(Dispatchers.IO) {
        vaultLock.withLock {
            files.list().forEach { files.delete(it) }
            physicalCheckDao.deleteAll()
            preferencesRepository.setPhysicalCheckEncryptionEnabled(false)
            preferencesRepository.setPhysicalCheckVaultKeys(null, null, null, null, null)
            setKey(null)
        }
    }

    suspend fun deletePhysicalCheck(check: PhysicalCheckEntity) = withContext(Dispatchers.IO) {
        vaultLock.withLock {
            photoNames(check).forEach { filename ->
                files.delete(filename)
                files.delete("$filename$ENC")
            }
            physicalCheckDao.deletePhysicalCheck(check)
        }
    }

    companion object {
        private const val ENC = ".enc"
        private const val NEW = ".new"
        private const val TMP = ".tmp"

        /** The smallest password for a new vault or a new password (an existing shorter one still opens). */
        const val MIN_PASSWORD_LENGTH = 8

        private fun photoNames(row: PhysicalCheckEntity): List<String> =
            if (row.fotoFilenames.isEmpty()) emptyList() else row.fotoFilenames.split(",").filter { it.isNotBlank() }

        /** A photo stored in clear (no `.enc`, `.new` or `.tmp` ending). */
        private fun isPhotoName(name: String): Boolean =
            !name.endsWith(ENC) && !name.endsWith(NEW) && !name.endsWith(TMP)

        /** The photo a file belongs to: `a.jpg.enc.new` → `a.jpg`. */
        private fun baseName(name: String): String =
            name.removeSuffix(TMP).removeSuffix(NEW).removeSuffix(ENC)

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

/** The weight and note of a check as sealed in its secret: `v1`, the weight (empty when none), a note flag, the note. */
internal object PhysicalCheckSecret {
    fun encode(peso: Float?, note: String?): String =
        "v1\n${peso?.toString() ?: ""}\n${if (note != null) "1" else "0"}\n${note ?: ""}"

    fun decode(text: String): Pair<Float?, String?> {
        val parts = text.split('\n', limit = 4)
        require(parts.size == 4 && parts[0] == "v1") { "Unknown secret" }
        val peso = parts[1].takeIf { it.isNotEmpty() }?.toFloat()
        val note = if (parts[2] == "1") parts[3] else null
        return peso to note
    }
}
