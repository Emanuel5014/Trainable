package com.emanuel5014.trainable.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.data.repository.ExerciseMergeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** What every "new exercise" form checks a name against, whatever screen it opens from. */
@HiltViewModel
class ExerciseNamesViewModel @Inject constructor(mergeRepository: ExerciseMergeRepository) : ViewModel() {
    val namesInUse: StateFlow<List<ExerciseEntity>> = mergeRepository.observeNamesInUse()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

/** The exercises a new name is checked against; read it only where the form is shown, it starts the observation. */
@Composable
fun rememberNamesInUse(): List<ExerciseEntity> {
    val viewModel: ExerciseNamesViewModel = hiltViewModel()
    val names by viewModel.namesInUse.collectAsState()
    return names
}
