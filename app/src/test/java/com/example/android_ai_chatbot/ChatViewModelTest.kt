package com.example.android_ai_chatbot

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.android_ai_chatbot.domian.model.ChatState
import com.example.android_ai_chatbot.domian.repository.ChatRepository
import com.example.android_ai_chatbot.domian.usecase.GetMessagesUseCase
import com.example.android_ai_chatbot.domian.usecase.SendMessageUseCase
import com.example.android_ai_chatbot.feature.chat.ChatViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalMaterial3Api::class)
class ChatViewModelTest{
    private val testDispatcher= UnconfinedTestDispatcher()

    private lateinit var sendMessageUseCase: SendMessageUseCase
    private lateinit var getMessagesUseCase: GetMessagesUseCase
    private lateinit var chatRepository: ChatRepository
    private lateinit var savedStateHandle: SavedStateHandle

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        sendMessageUseCase = mockk()
        getMessagesUseCase = mockk()
        chatRepository     = mockk(relaxed = true)
        savedStateHandle   = SavedStateHandle(
            mapOf("conversationId" to "test-conv-id")
        )

        // Default: getMessages returns empty list
        every { getMessagesUseCase("test-conv-id") } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = ChatViewModel(
        sendMessageUseCase = sendMessageUseCase,
        getMessagesUseCase = getMessagesUseCase,
        chatRepository = chatRepository,
        savedStateHandle = savedStateHandle
    )

    @Test
    fun `initial state is Idle with empty messages`() = runTest {
        val vm = createViewModel()
        Assert.assertEquals(ChatState.Idle, vm.uiState.value.chatState)
        Assert.assertTrue(vm.uiState.value.messages.isEmpty())
    }

    @Test
    fun `onInputChanged updates inputText`() = runTest {
        val vm = createViewModel()
        vm.onInputChanged("Hello AI")
        Assert.assertEquals("Hello AI", vm.uiState.value.inputText)
    }

    @Test
    fun `sendMessage sets Streaming state then returns to Idle`() = runTest {
        every { getMessagesUseCase("test-conv-id") } returns flowOf(emptyList())

        // Simulate streaming: emits two tokens then completes
        coEvery {
            sendMessageUseCase("test-conv-id", "Hello", any())
        } returns flow {
            emit("Hi ")
            emit("there!")
        }

        val vm = createViewModel()
        vm.onInputChanged("Hello")

        vm.uiState.test {
            val initial = awaitItem()
            Assert.assertEquals(ChatState.Idle, initial.chatState)

            vm.sendMessage()

            // Input should clear immediately
            val afterSend = awaitItem()
            Assert.assertEquals("", afterSend.inputText)

            // Should eventually return to Idle
            val finalState = awaitItem()
            Assert.assertEquals(ChatState.Idle, finalState.chatState)
            cancelAndIgnoreRemainingEvents()
        }
    }
    @Test
    fun `sendMessage does nothing when input is blank`() = runTest {
        val vm = createViewModel()
        vm.onInputChanged("   ")
        vm.sendMessage()

        // sendMessageUseCase should never be called
        coVerify(exactly = 0) { sendMessageUseCase(any(), any(), any()) }
    }

    @Test
    fun `sendMessage does nothing when already streaming`() = runTest {
        val vm = createViewModel()
        // Manually put viewmodel in Streaming state
        vm.onInputChanged("question")

        // First send starts streaming
        coEvery {
            sendMessageUseCase(any(), any(), any())
        } returns flow { delay(1000) }

        vm.sendMessage()
        vm.sendMessage()  // second call should be ignored

        coVerify(exactly = 1) { sendMessageUseCase(any(), any(), any()) }
    }

    @Test
    fun `setVoiceInput updates inputText`() = runTest {
        val vm = createViewModel()
        vm.setVoiceInput("voice transcribed text")
        Assert.assertEquals("voice transcribed text", vm.uiState.value.inputText)
    }

    @Test
    fun `error state is set when streaming throws`() = runTest {
        coEvery {
            sendMessageUseCase(any(), any(), any())
        } returns flow { throw Exception("Network error") }

        val vm = createViewModel()
        vm.onInputChanged("Hello")
        vm.sendMessage()

        // Wait for coroutines to finish
        testScheduler.advanceUntilIdle()

        Assert.assertTrue(vm.uiState.value.chatState is ChatState.Error)
        Assert.assertEquals(
            "Network error",
            (vm.uiState.value.chatState as ChatState.Error).message
        )
    }

    @Test
    fun `clearError resets state to Idle`() = runTest {
        coEvery {
            sendMessageUseCase(any(), any(), any())
        } returns flow { throw Exception("fail") }

        val vm = createViewModel()
        vm.onInputChanged("hi")
        vm.sendMessage()
        testScheduler.advanceUntilIdle()

        Assert.assertTrue(vm.uiState.value.chatState is ChatState.Error)
        vm.clearError()
        Assert.assertEquals(ChatState.Idle, vm.uiState.value.chatState)
    }

}