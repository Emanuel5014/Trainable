package com.emanuel5014.trainable.ui.screens.physicalcheck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emanuel5014.trainable.data.local.entity.PhysicalCheckEntity
import com.emanuel5014.trainable.data.repository.PhysicalCheckRepository
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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

    init {
        viewModelScope.launch {
            // Tenta l'autologin all'avvio
            val autoUnlocked = repository.tryAutoUnlock()
            _isUnlocked.value = autoUnlocked
        }
    }

    fun unlock(password: String, onSuccess: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            val success = repository.unlock(password)
            if (success) {
                _isUnlocked.value = true
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

    fun addPhotosToCheck(checkId: Int, photosBytes: List<ByteArray>) {
        viewModelScope.launch {
            repository.addPhotosToCheck(checkId, photosBytes)
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
}
