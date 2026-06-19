package com.example.android_ai_chatbot.core.util

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelDownloader @Inject constructor(
    @ApplicationContext private val context: Context
){
    companion object {
        const val MODEL_URL = "https://storage.googleapis.com/mediapipe-models/llm_inference/gemma-2b-it-cpu-int8.bin"
        const val MODEL_FILENAME = "gemma-2b-it-cpu-int8.bin"
    }
    val modelPath: String
        get()= File(context.filesDir, MODEL_FILENAME).absolutePath

    val isModelDownloaded: Boolean
        get() = File(modelPath).exists()

    fun downloadModel(): Flow<Int> = flow {
        val file = File(context.filesDir, MODEL_FILENAME)
        if (file.exists()) {
            emit(100)
            return@flow
        }

        try {
            val url = java.net.URL(MODEL_URL)
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout    = 15000
            connection.connect()

            if (connection.responseCode != 200) {
                throw Exception("Server returned ${connection.responseCode}")
            }

            val fileSize = connection.contentLength
            val tempFile = File(context.filesDir, "$MODEL_FILENAME.tmp")

            connection.inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    val buffer    = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (fileSize > 0) {
                            emit(((totalRead * 100) / fileSize).toInt())
                        }
                    }
                }
            }

            // Rename temp file to final name only after complete download
            tempFile.renameTo(file)
            emit(100)

        } catch (e: Exception) {
            // Clean up partial download
            File(context.filesDir, "$MODEL_FILENAME.tmp").delete()
            throw Exception("Download failed: ${e.message}")
        }
    }.flowOn(Dispatchers.IO)

    fun deleteModel(){
        File(modelPath).delete()
    }
}