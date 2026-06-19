package com.example.android_ai_chatbot.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android_ai_chatbot.domian.model.AiModel
import com.example.android_ai_chatbot.domian.model.AvailableModels
import com.example.android_ai_chatbot.domian.usecase.DeleteAllConversationsUseCase
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val firebaseAuth: FirebaseAuth,
    private val deleteAllConversationsUseCase: DeleteAllConversationsUseCase

) : ViewModel() {

    val isDarkMode: StateFlow<Boolean> = userPreferences.isDarkMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false
        )

    val selectedModelId: StateFlow<String> = userPreferences.selectedModelId
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            AvailableModels.default.id
        )

    val selectedModel: AiModel
        get() = AvailableModels.findById(selectedModelId.value)

    val userEmail: String
        get() = firebaseAuth.currentUser?.email ?: "Not signed in"

    fun signOut() {
        firebaseAuth.signOut()
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setDarkMode(enabled)
        }
    }

    fun selectModel(modelId: String) {
        viewModelScope.launch { userPreferences.setSelectedModel(modelId) }
    }

    fun deleteAllHistory() {
        viewModelScope.launch {
            deleteAllConversationsUseCase()
        }
    }
}
