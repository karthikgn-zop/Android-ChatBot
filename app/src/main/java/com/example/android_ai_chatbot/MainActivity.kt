package com.example.android_ai_chatbot

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.android_ai_chatbot.feature.chat.ChatScreen
import com.example.android_ai_chatbot.feature.history.HistoryScreen
import com.example.android_ai_chatbot.feature.settings.SettingsScreen
import com.example.android_ai_chatbot.feature.settings.SettingsViewModel
import com.example.android_ai_chatbot.ui.theme.Android_AI_ChatbotTheme as AIChatTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AIChatApplication : Application()

// ─── Main Activity ────────────────────────────────────────────────────────────

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
    const val HISTORY  = "history"
    const val SETTINGS = "settings"
    const val CHAT     = "chat/{conversationId}/{title}"

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

    NavHost(
        navController    = navController,
        startDestination = Routes.HISTORY
    ) {
        // History (home screen)
        composable(Routes.HISTORY) {
            HistoryScreen(
                onOpenConversation = { id, title ->
                    navController.navigate(Routes.chat(id, title))
                }
            )
        }

        // Chat screen
        composable(
            route     = Routes.CHAT,
            arguments = listOf(
                navArgument("conversationId") { type = NavType.StringType },
                navArgument("title")          { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val title = backStackEntry.arguments
                ?.getString("title")
                ?.decodeFromNav() ?: "Chat"

            ChatScreen(
                conversationTitle = title,
                onNavigateBack    = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen()
        }
    }
}
