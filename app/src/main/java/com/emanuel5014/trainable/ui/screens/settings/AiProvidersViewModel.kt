package com.emanuel5014.trainable.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emanuel5014.trainable.data.ai.remote.AiProfileStore
import com.emanuel5014.trainable.data.ai.remote.AiRemoteHttp
import com.emanuel5014.trainable.data.ai.remote.OkHttpAiHttp
import dagger.hilt.android.lifecycle.HiltViewModel
import okhttp3.OkHttpClient
import javax.inject.Inject

/** Owns the shared [AiProvidersModel] for Settings → AI; the calls go through OkHttp set up by [AiRemoteHttp]. */
@HiltViewModel
class AiProvidersViewModel @Inject constructor(
    store: AiProfileStore,
    baseClient: OkHttpClient
) : ViewModel() {
    val model = AiProvidersModel(store, OkHttpAiHttp(AiRemoteHttp.client(baseClient)), viewModelScope)
}
