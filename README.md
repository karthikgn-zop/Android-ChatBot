# 🤖 Android AI ChatBot

An AI-powered chat application for Android built with **Jetpack Compose**, **Clean Architecture**, and **Groq API** for real-time streaming responses. Features Google Sign-In, conversation history, voice input, image understanding, markdown rendering, and multi-model selection.

---

## 📱 Screenshots

| Login | Chat | History | Settings |
|-------|------|---------|----------|
| <img src="screenshots/LoginScreen.png" width="200"/> | <img src="screenshots/ChatScreen.png" width="200"/> | <img src="screenshots/HistoryScreen.png" width="200"/> | <img src="screenshots/SettingsScreen.png" width="200"/> |

---

## ✨ Features

- 🔐 **Google Sign-In** — Firebase Authentication with secure login/logout
- 💬 **Real-time AI Streaming** — Token-by-token response streaming using SSE (Server-Sent Events)
- 🖼️ **Image Understanding** — Attach images from gallery and ask the AI about them (Llama 4 Scout)
- 🎙️ **Voice Input** — Speak your message using the device microphone
- 📝 **Conversation History** — All chats saved locally with Room database
- 🏷️ **Auto Title Generation** — Conversations are automatically named based on the first message
- 🌙 **Dark Mode** — Persistent dark/light theme toggle via DataStore
- 🗑️ **Clear History** — Delete all conversations with confirmation dialog
- ⚙️ **Settings Screen** — Manage theme, model, account, and data
- 🔤 **Markdown Rendering** — AI responses render bold, code blocks, lists, and headings properly
- 🔍 **Search Conversations** — Full-text search through conversation history with highlighted results
- 🤖 **Multi-Model Selection** — Switch between 5 Groq AI models from Settings
- ⚠️ **Image Support Indicator** — Warning shown when selected model doesn't support images

---

## 🏗️ Architecture

This project follows **Clean Architecture** with **MVVM** pattern, separating concerns into three layers:

```
app/
├── core/
│   ├── di/                      # Hilt dependency injection modules
│   └── network/                 # OkHttp interceptors, NetworkMonitor, NetworkResults
│
├── data/
│   ├── local/                   # Room database, DAOs, Entities, FTS
│   └── repository/              # Repository implementations
│
├── domain/
│   ├── model/                   # Domain models (Message, Conversation, AiModel, ChatState)
│   ├── repository/              # Repository interfaces
│   └── usecase/                 # Use cases (SendMessage, GetMessages, DeleteAll, etc.)
│
└── feature/
    ├── auth/                    # Login screen + AuthViewModel
    ├── chat/                    # Chat screen + ChatViewModel
    ├── history/                 # History screen + HistoryViewModel
    └── settings/                # Settings screen + SettingsViewModel
```

### Why Clean Architecture?

- **Separation of concerns** — UI, business logic, and data are fully independent
- **Testability** — Each layer can be unit tested in isolation
- **Scalability** — Easy to swap API providers or databases without touching UI code

---

## 🛠️ Tech Stack

| Category | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | Clean Architecture + MVVM |
| DI | Hilt (with KSP) |
| Database | Room + FTS4 (Full Text Search) |
| Networking | Retrofit + OkHttp |
| Streaming | SSE (Server-Sent Events) via OkHttp |
| Auth | Firebase Authentication (Google Sign-In) |
| Preferences | DataStore |
| Navigation | Navigation Compose |
| AI Provider | Groq API (multiple models) |
| Image Loading | Coil |
| Markdown | compose-markdown |
| Async | Kotlin Coroutines + Flow |

---

## 🤖 Supported AI Models

| Model | Image Support | Best For |
|-------|--------------|----------|
| Llama 3.1 8B | ❌ | Fast everyday tasks |
| Llama 3.3 70B | ❌ | Complex reasoning |
| Llama 4 Scout | ✅ | Text + image understanding |
| Gemma 2 9B | ❌ | Google's open model |
| Mixtral 8x7B | ❌ | Large context (32k tokens) |

Switch models anytime from **Settings → AI Model**.

---

## 🚀 Getting Started

### Prerequisites

- Android Studio Hedgehog or newer
- Android device or emulator running API 24+
- A free [Groq API key](https://console.groq.com)
- A Firebase project with Google Sign-In enabled

---

### 1. Clone the Repository

```bash
git clone https://github.com/karthikgn-zop/Android-ChatBot.git
cd Android-ChatBot
```

---

### 2. Set Up Groq API Key

1. Go to [console.groq.com](https://console.groq.com) and create a free account
2. Generate an API key
3. Open `local.properties` in the root of the project
4. Add the following:

```properties
sdk.dir=/path/to/your/Android/sdk
API_KEY=your_groq_api_key_here
MODEL=llama-3.1-8b-instant
```

> ⚠️ Never commit `local.properties` to version control. It is already in `.gitignore`.

---

### 3. Set Up Firebase

1. Go to [console.firebase.google.com](https://console.firebase.google.com)
2. Create a new project
3. Add an Android app with package name `com.example.android_ai_chatbot`
4. Enable **Google Sign-In** under Authentication → Sign-in method
5. Download `google-services.json` and place it in the `app/` folder
6. Add your debug SHA-1 fingerprint:

```bash
./gradlew signingReport
```

Copy the `SHA-1` from the debug variant → Firebase Console → Project Settings → Your App → Add fingerprint

7. Re-download `google-services.json` after adding the fingerprint

---

### 4. Build and Run

Open the project in Android Studio, let Gradle sync, then click **Run**.

---

## 🔑 Environment Variables

All sensitive configuration is stored in `local.properties` and exposed to the app via `BuildConfig`:

| Key | Description |
|---|---|
| `API_KEY` | Your Groq API key |
| `MODEL` | Default AI model (e.g. `llama-3.1-8b-instant`) |

These are injected at build time and never hardcoded in source files.

> The active model at runtime is controlled by the **Settings → AI Model** selector and persisted in DataStore — `local.properties` only sets the initial default.

---

## 📡 How Streaming Works

The app uses **Server-Sent Events (SSE)** to stream AI responses token by token:

1. User sends a message
2. A `POST` request is made to `https://api.groq.com/openai/v1/chat/completions` with `"stream": true`
3. The response body is read line by line using OkHttp's `ResponseBody.source()`
4. Each `data: {...}` line is parsed as an `OpenAIStreamChunk`
5. The token is extracted from `choices[0].delta.content` and emitted via Kotlin `Flow`
6. The UI collects the flow and appends each token to the message bubble in real time

```
User sends message
      ↓
SendMessageUseCase → ChatRepository.sendMessageStream()
      ↓
Retrofit @Streaming POST → Groq API
      ↓
OkHttp reads SSE line by line
      ↓
Each token emitted via Flow<String>
      ↓
ChatViewModel collects → updates Room DB
      ↓
ChatScreen LazyColumn re-renders with each token
```

---

## 🖼️ How Image Attachments Work

1. User picks an image from gallery via `ActivityResultContracts.GetContent()`
2. Image URI is stored in `ChatUiState.attachedImageUri`
3. On send, the image is encoded to **base64** using `ContentResolver`
4. A multimodal message is built with both `TextContentPart` and `ImageContentPart`
5. The image URI is persisted as a string in Room's `messages` table
6. `MessageBubble` loads it back via Coil's `AsyncImage`

> ⚠️ Image understanding only works with **Llama 4 Scout**. A warning banner is shown if you attach an image while a text-only model is selected.

---

## 🔍 How Search Works

Conversation search uses **Room FTS4** (Full Text Search):

- `ConversationFtsEntity` is an FTS4 virtual table linked to `ConversationEntity`
- Queries use SQLite `MATCH` operator with prefix matching (`query*`)
- Results are highlighted in the UI using `AnnotatedString` with `SpanStyle`
- The search bar replaces the top app bar when activated

---

## 🗄️ Database Schema

### conversations
| Column | Type | Description |
|---|---|---|
| id | TEXT (PK) | UUID |
| title | TEXT | Auto-generated conversation title |
| createdAt | INTEGER | Unix timestamp |
| updatedAt | INTEGER | Updated on each message |

### messages
| Column | Type | Description |
|---|---|---|
| id | TEXT (PK) | UUID |
| conversationId | TEXT (FK) | References conversations.id — CASCADE delete |
| content | TEXT | Message text |
| role | TEXT | "USER" or "ASSISTANT" |
| timestamp | INTEGER | Unix timestamp |
| isStreaming | INTEGER | Boolean — true while AI is generating |
| imageUri | TEXT | URI of attached image (nullable) |

### conversations_fts
| Column | Type | Description |
|---|---|---|
| title | TEXT | FTS4 virtual table for full-text search |

---

## 📁 Key Files Reference

| File | Purpose |
|-------|---------|
| `ChatRepositoryImpl.kt` | SSE streaming, message persistence, title generation, dynamic model |
| `ChatViewModel.kt` | Chat UI state, message sending, image handling, model awareness |
| `AppModules.kt` | Hilt DI — networking, database, auth, repositories |
| `OpenAIApiService.kt` | Retrofit interface for Groq API (OpenAI-compatible) |
| `AuthRepository.kt` | Firebase + Credential Manager Google Sign-In |
| `MainActivity.kt` | App entry point, NavGraph, dark mode theming |
| `ChatScreen.kt` | Chat UI — markdown bubbles, input bar, voice, image picker, warning banner |
| `HistoryScreen.kt` | Conversation list with search, rename, delete |
| `SettingsScreen.kt` | Dark mode, model selector, clear history, sign out |
| `UserPreferences.kt` | DataStore — dark mode + selected model persistence |
| `AiModel.kt` | Model definitions with image support flags |

---

## 🧪 Testing

The project includes unit test setup with:

- **JUnit4** — test runner
- **MockK** — Kotlin-friendly mocking library
- **Turbine** — Flow testing utility
- **kotlinx-coroutines-test** — coroutine test dispatcher

Run tests with:
```bash
./gradlew test
```

---

## 🔄 Switching AI Models

**At runtime:** Go to **Settings → AI Model** and select from the list.

**Default model** can be changed in `local.properties`:
```properties
MODEL=llama-3.1-8b-instant       # Fast, free
MODEL=llama-3.3-70b-versatile    # More capable
MODEL=meta-llama/llama-4-scout-17b-16e-instruct  # Images + text
MODEL=gemma2-9b-it               # Google Gemma
MODEL=mixtral-8x7b-32768         # 32k context window
```

---

## 🛣️ Planned Features

- 📋 **Copy message** — Long press AI response to copy text
- 🔁 **Regenerate response** — Retry button on AI messages
- 📤 **Export chat** — Share conversation as text
- 🎭 **System prompt customization** — Set custom AI persona in Settings
- 🌐 **Offline mode** — On-device AI via MediaPipe (Gemma 2B)

---

## ⚠️ Known Challenges Solved

| Challenge | Solution |
|-----------|----------|
| SSE streaming not natively supported by Retrofit | Used OkHttp `ResponseBody.source()` with manual line parsing |
| Regional Gemini API quota = 0 in India | Migrated to Groq (free, OpenAI-compatible, works globally) |
| Auto-rename race condition | Captured `isFirstMessage` before state update; renamed after streaming completes |
| Image URI lost after state cleared | Captured URI before clearing UI state; persisted as string in Room |
| Firebase SHA-1 conflict on office laptop | Generated project-specific debug keystore |
| Kotlin/Firebase version mismatch | Aligned Firebase BOM `32.7.4` with Kotlin `2.2.0` |

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/your-feature`)
3. Commit your changes (`git commit -m 'Add your feature'`)
4. Push to the branch (`git push origin feature/your-feature`)
5. Open a Pull Request

---

## 📄 License

```
MIT License

Copyright (c) 2026 Karthik

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.
```

---

## 👤 Author

**Karthik**
- Stack: Kotlin, Jetpack Compose, Clean Architecture, Hilt, Room, Groq API, Firebase

---

