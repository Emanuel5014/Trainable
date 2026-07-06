package com.emanuel5014.trainable.ui.screens.physicalcheck

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emanuel5014.trainable.data.local.entity.PhysicalCheckEntity
import com.emanuel5014.trainable.data.repository.PhysicalCheckRepository
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PhysicalCheckViewModel @Inject constructor(
    private val repository: PhysicalCheckRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val checks: StateFlow<List<PhysicalCheckEntity>> = repository.getAllPhysicalChecks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val biometricEnabled = preferencesRepository.physicalCheckBiometricEnabled
    val encryptionEnabled = preferencesRepository.physicalCheckEncryptionEnabled

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked

    private val _sessionActive = MutableStateFlow(true)
    val sessionActive: StateFlow<Boolean> = _sessionActive

    private val _isAutoUnlocking = MutableStateFlow(true)
    val isAutoUnlocking: StateFlow<Boolean> = _isAutoUnlocking

    private val _lastActivityTime = MutableStateFlow(System.currentTimeMillis())

    companion object {
        private const val SESSION_TIMEOUT_MS = 5 * 60 * 1000L
        private const val INACTIVITY_CHECK_INTERVAL_MS = 60_000L
    }

    init {
        autoUnlock()
        observeAppLifecycle()
        startInactivityMonitor()
    }

    private fun autoUnlock() {
        if (_isUnlocked.value) return
        _isAutoUnlocking.value = true
        viewModelScope.launch {
            val unlocked = repository.tryAutoUnlock()
            _isUnlocked.value = unlocked
            _isAutoUnlocking.value = false
        }
    }

    fun setSessionActive(active: Boolean) {
        _sessionActive.value = active
        if (active) _lastActivityTime.value = System.currentTimeMillis()
    }

    fun touch() {
        _lastActivityTime.value = System.currentTimeMillis()
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
        viewModelScope.launch {
            val success = repository.unlock(password)
            if (success) {
                _isUnlocked.value = true
                _lastActivityTime.value = System.currentTimeMillis()
                onSuccess()
            } else {
                onError()
            }
        }
    }

    fun enableEncryption(password: String, onSuccess: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            val success = repository.enableEncryption(password)
            if (success) {
                _isUnlocked.value = true
                _lastActivityTime.value = System.currentTimeMillis()
                onSuccess()
            } else {
                onError()
            }
        }
    }

    fun disableEncryption(password: String, onSuccess: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
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
        viewModelScope.launch {
            repository.resetAllAndDisableEncryption()
            _isUnlocked.value = false
            onComplete()
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setPhysicalCheckBiometricEnabled(enabled)
        }
    }

    fun addCheck(timestamp: Long, peso: Float?, note: String?, photosBytes: List<ByteArray>, onComplete: () -> Unit) {
        viewModelScope.launch {
            val filenames = photosBytes.map { bytes ->
                repository.savePhoto(bytes)
            }
            repository.insertPhysicalCheck(timestamp, peso, note, filenames)
            onComplete()
        }
    }

    fun updateCheck(checkId: Int, timestamp: Long, peso: Float?, note: String?, onComplete: () -> Unit) {
        viewModelScope.launch {
            val existing = repository.getPhysicalCheckById(checkId) ?: return@launch
            repository.updatePhysicalCheck(
                existing.copy(timestamp = timestamp, peso = peso, note = note)
            )
            onComplete()
        }
    }

    fun addPhotosToCheck(checkId: Int, photosBytes: List<ByteArray>, onComplete: (List<String>) -> Unit = {}) {
        viewModelScope.launch {
            val newFilenames = repository.addPhotosToCheck(checkId, photosBytes)
            onComplete(newFilenames)
        }
    }

    fun deletePhotoFromCheck(checkId: Int, filename: String) {
        viewModelScope.launch {
            repository.deletePhotoFromCheck(checkId, filename)
        }
    }

    fun deleteCheck(check: PhysicalCheckEntity) {
        viewModelScope.launch {
            repository.deletePhysicalCheck(check)
        }
    }

    suspend fun getPhotoBytes(filename: String): ByteArray? {
        return repository.getPhotoBytes(filename)
    }

    private fun observeAppLifecycle() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                lockSession()
            }

            override fun onStart(owner: LifecycleOwner) {
                autoUnlock()
            }
        })
    }

    private fun startInactivityMonitor() {
        viewModelScope.launch {
            while (true) {
                delay(INACTIVITY_CHECK_INTERVAL_MS)
                val elapsed = System.currentTimeMillis() - _lastActivityTime.value
                if (elapsed >= SESSION_TIMEOUT_MS && _sessionActive.value) {
                    lockSession()
                }
            }
        }
    }
}
