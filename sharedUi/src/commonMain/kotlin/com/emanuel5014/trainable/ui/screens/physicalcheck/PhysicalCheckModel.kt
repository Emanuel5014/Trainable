package com.emanuel5014.trainable.ui.screens.physicalcheck

import com.emanuel5014.trainable.data.local.entity.PhysicalCheckEntity
import com.emanuel5014.trainable.data.repository.PhysicalCheckRepository
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.concurrent.Volatile
import kotlin.time.Clock

/**
 * The state of the physical checks (list, compare and settings screens): the checks, the lock (password and
 * biometric session) and the photo operations. The owner reports the app going to the background and back with
 * [onAppStop] / [onAppStart], and cancels [scope] when the screen goes away.
 */
class PhysicalCheckModel(
    private val repository: PhysicalCheckRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val scope: CoroutineScope
) {

    val checks: StateFlow<List<PhysicalCheckEntity>> = repository.getAllPhysicalChecks()
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    val biometricEnabled = preferencesRepository.physicalCheckBiometricEnabled
    val encryptionEnabled = preferencesRepository.physicalCheckEncryptionEnabled
    val swipeActionsEnabled: Flow<Boolean> = preferencesRepository.swipeActionsEnabled
    val weightUnit = preferencesRepository.weightUnit
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), "kg")

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked

    private val _sessionActive = MutableStateFlow(true)
    val sessionActive: StateFlow<Boolean> = _sessionActive

    private val _isAutoUnlocking = MutableStateFlow(true)
    val isAutoUnlocking: StateFlow<Boolean> = _isAutoUnlocking

    private val _lastActivityTime = MutableStateFlow(now())

    companion object {
        private const val SESSION_TIMEOUT_MS = 5 * 60 * 1000L
        private const val INACTIVITY_CHECK_INTERVAL_MS = 60_000L

        private fun now() = Clock.System.now().toEpochMilliseconds()
    }

    init {
        autoUnlock()
        startInactivityMonitor()
    }

    private fun autoUnlock() {
        if (_isUnlocked.value) return
        _isAutoUnlocking.value = true
        scope.launch {
            val unlocked = repository.tryAutoUnlock()
            _isUnlocked.value = unlocked
            _isAutoUnlocking.value = false
        }
    }

    /** The app went to the background: the biometric session ends (unless a photo is being taken). */
    fun onAppStop() {
        lockSession()
    }

    /** The app came back: the key is loaded again from the device when possible. */
    fun onAppStart() {
        autoUnlock()
    }

    fun setSessionActive(active: Boolean) {
        _sessionActive.value = active
        if (active) _lastActivityTime.value = now()
    }

    fun touch() {
        _lastActivityTime.value = now()
    }

    @Volatile
    private var photoCaptureInProgress = false

    fun setPhotoCaptureStarted() {
        photoCaptureInProgress = true
    }

    fun setPhotoCaptureCompleted() {
        photoCaptureInProgress = false
    }

    fun lockSession() {
        if (photoCaptureInProgress) return
        _sessionActive.value = false
    }

    fun unlock(password: String, onSuccess: () -> Unit, onError: () -> Unit) {
        scope.launch {
            val success = repository.unlock(password)
            if (success) {
                _isUnlocked.value = true
                _lastActivityTime.value = now()
                onSuccess()
            } else {
                onError()
            }
        }
    }

    fun enableEncryption(password: String, onSuccess: () -> Unit, onError: () -> Unit) {
        scope.launch {
            val success = repository.enableEncryption(password)
            if (success) {
                _isUnlocked.value = true
                _lastActivityTime.value = now()
                onSuccess()
            } else {
                onError()
            }
        }
    }

    fun disableEncryption(password: String, onSuccess: () -> Unit, onError: () -> Unit) {
        scope.launch {
            val success = repository.disableEncryption(password)
            if (success) {
                _isUnlocked.value = false
                onSuccess()
            } else {
                onError()
            }
        }
    }

    fun resetAllData(onComplete: () -> Unit) {
        scope.launch {
            repository.resetAllAndDisableEncryption()
            _isUnlocked.value = false
            onComplete()
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        scope.launch {
            preferencesRepository.setPhysicalCheckBiometricEnabled(enabled)
        }
    }

    fun addCheck(timestamp: Long, peso: Float?, note: String?, photosBytes: List<ByteArray>, onComplete: () -> Unit) {
        scope.launch {
            val filenames = photosBytes.map { bytes ->
                repository.savePhoto(bytes)
            }
            repository.insertPhysicalCheck(timestamp, peso, note, filenames)
            onComplete()
        }
    }

    fun updateCheck(checkId: Int, timestamp: Long, peso: Float?, note: String?, onComplete: () -> Unit) {
        scope.launch {
            val existing = repository.getPhysicalCheckById(checkId) ?: return@launch
            repository.updatePhysicalCheck(
                existing.copy(timestamp = timestamp, peso = peso, note = note)
            )
            onComplete()
        }
    }

    fun addPhotosToCheck(checkId: Int, photosBytes: List<ByteArray>, onComplete: (List<String>) -> Unit = {}) {
        scope.launch {
            val newFilenames = repository.addPhotosToCheck(checkId, photosBytes)
            onComplete(newFilenames)
        }
    }

    fun deletePhotoFromCheck(checkId: Int, filename: String) {
        scope.launch {
            repository.deletePhotoFromCheck(checkId, filename)
        }
    }

    fun deleteCheck(check: PhysicalCheckEntity) {
        scope.launch {
            repository.deletePhysicalCheck(check)
        }
    }

    suspend fun getPhotoBytes(filename: String): ByteArray? {
        return repository.getPhotoBytes(filename)
    }

    private fun startInactivityMonitor() {
        scope.launch {
            while (true) {
                delay(INACTIVITY_CHECK_INTERVAL_MS)
                val elapsed = now() - _lastActivityTime.value
                if (elapsed >= SESSION_TIMEOUT_MS && _sessionActive.value) {
                    lockSession()
                }
            }
        }
    }
}
