package com.example.android_ai_chatbot.feature.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android_ai_chatbot.core.util.ModelDownloader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ModelDownloadViewModel @Inject constructor(
    private val modelDownloader: ModelDownloader
) : ViewModel() {

    private val _progress = MutableStateFlow(0)
    val progress: StateFlow<Int> = _progress.asStateFlow()

    private val _isDownloaded = MutableStateFlow(modelDownloader.isModelDownloaded)
    val isDownloaded: StateFlow<Boolean> = _isDownloaded.asStateFlow()  // ← isDownloaded not isDownload

    fun startDownload() {
        viewModelScope.launch {
            modelDownloader.downloadModel().collect { p ->
                _progress.value = p
                if (p == 100) _isDownloaded.value = true
            }
        }
    }
}