package com.example.android_ai_chatbot.feature.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.android_ai_chatbot.domian.model.AvailableModels
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val SELECTED_MODEL_KEY = stringPreferencesKey("selected_model")  // ← move to companion
    }

    val isDarkMode: Flow<Boolean> = context.dataStore.data  // ← context.dataStore
        .map { it[DARK_MODE] ?: false }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[DARK_MODE] = enabled }  // ← context.dataStore
    }

    val selectedModelId: Flow<String> = context.dataStore.data  // ← context.dataStore
        .map { it[SELECTED_MODEL_KEY] ?: AvailableModels.default.id }

    suspend fun setSelectedModel(modelId: String) {
        context.dataStore.edit { it[SELECTED_MODEL_KEY] = modelId }  // ← context.dataStore
    }
}