package com.emanuel5014.trainable.ui.screens.physicalcheck

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emanuel5014.trainable.data.repository.PhysicalCheckRepository
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** The Android owner of the shared [PhysicalCheckModel]: its scope and the app's background/foreground events. */
@HiltViewModel
class PhysicalCheckViewModel @Inject constructor(
    repository: PhysicalCheckRepository,
    preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val model = PhysicalCheckModel(repository, preferencesRepository, viewModelScope)

    private val appLifecycle = object : DefaultLifecycleObserver {
        override fun onStop(owner: LifecycleOwner) {
            model.onAppStop()
        }

        override fun onStart(owner: LifecycleOwner) {
            model.onAppStart()
        }
    }

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycle)
    }

    override fun onCleared() {
        // Removed, or every model ever opened stayed alive and kept reacting to the app's lifecycle
        ProcessLifecycleOwner.get().lifecycle.removeObserver(appLifecycle)
        model.close()
        super.onCleared()
    }
}
