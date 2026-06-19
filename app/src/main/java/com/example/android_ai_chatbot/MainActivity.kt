package com.example.android_ai_chatbot

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.android_ai_chatbot.feature.auth.AuthViewModel
import com.example.android_ai_chatbot.feature.auth.LoginScreen
import com.example.android_ai_chatbot.feature.chat.ChatScreen
import com.example.android_ai_chatbot.feature.history.HistoryScreen
import com.example.android_ai_chatbot.feature.model.ModelDownloadScreen
import com.example.android_ai_chatbot.feature.model.ModelDownloadViewModel
import com.example.android_ai_chatbot.feature.settings.SettingsScreen
import com.example.android_ai_chatbot.feature.settings.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp
import com.example.android_ai_chatbot.ui.theme.Android_AI_ChatbotTheme as AIChatTheme

@HiltAndroidApp
class AIChatApplication : Application()


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val isDarkMode by settingsViewModel.isDarkMode.collectAsState()

            AIChatTheme(darkTheme = isDarkMode) {
                AIChatNavGraph()
            }
        }
    }
}


object Routes {
    const val LOGIN = "login"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val CHAT = "chat/{conversationId}/{title}"
    const val MODEL_DOWNLOAD = "model_download"

    fun chat(conversationId: String, title: String) =
        "chat/$conversationId/${title.encodeForNav()}"
}

private fun String.encodeForNav() =
    java.net.URLEncoder.encode(this, "UTF-8")

private fun String.decodeFromNav() =
    java.net.URLDecoder.decode(this, "UTF-8")

@Composable
fun AIChatNavGraph() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val modelDownloadViewModel: ModelDownloadViewModel = hiltViewModel()

    val uiState by authViewModel.uiState.collectAsState()
    val isModelDownloaded by modelDownloadViewModel.isDownloaded.collectAsState()

    LaunchedEffect(uiState.user, isModelDownloaded) {
        when {
            uiState.user == null -> {
                navController.navigate(Routes.LOGIN) {
                    popUpTo(0) { inclusive = true }
                }
            }
            !isModelDownloaded -> {
                navController.navigate(Routes.MODEL_DOWNLOAD) {
                    popUpTo(0) { inclusive = true }
                }
            }
            else -> {
                navController.navigate(Routes.HISTORY) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    // ← startDestination is just a placeholder now
    NavHost(
        navController = navController,
        startDestination = Routes.LOGIN  // always start here, LaunchedEffect handles routing
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(onLoginSuccess = {})  // ← LaunchedEffect handles navigation
        }

        composable(Routes.MODEL_DOWNLOAD) {
            ModelDownloadScreen(
                onDownloadComplete = {
                    navController.navigate(Routes.HISTORY) {
                        popUpTo(Routes.MODEL_DOWNLOAD) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HISTORY) {
            HistoryScreen(
                onOpenConversation   = { id, title -> navController.navigate(Routes.chat(id, title)) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack    = { navController.popBackStack() },
                onNavigateToLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route     = Routes.CHAT,
            arguments = listOf(
                navArgument("conversationId") { type = NavType.StringType },
                navArgument("title")          { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val title = backStackEntry.arguments
                ?.getString("title")?.decodeFromNav() ?: "Chat"
            ChatScreen(
                conversationTitle = title,
                onNavigateBack    = { navController.popBackStack() }
            )
        }
    }
}