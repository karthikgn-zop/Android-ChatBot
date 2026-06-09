package com.example.android_ai_chatbot.core.network

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import retrofit2.HttpException
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton


sealed class NetworkResults<out T>{
    data class Success<T>(val data: T): NetworkResults<T>()
    data class Error(val message: String, val code: Int? = null) : NetworkResults<Nothing>()
    object Loading : NetworkResults<Nothing>()
}

suspend fun <T> safeApiCall(apiCall:suspend ()->T): NetworkResults<T>{
    return try{
        NetworkResults.Success(apiCall())
    } catch (e: HttpException) {
        val errorMessage = when (e.code()) {
            400 -> "Bad request — check your prompt"
            401 -> "Invalid API key — check local.properties"
            403 -> "Access forbidden"
            429 -> "Rate limit exceeded — wait a moment and retry"
            500 -> "OpenAI server error — try again later"
            503 -> "OpenAI service unavailable"
            else -> "HTTP error ${e.code()}: ${e.message()}"
        }
        NetworkResults.Error(errorMessage, e.code())
    } catch (e: SocketTimeoutException) {
        NetworkResults.Error("Request timed out — check your connection")
    } catch (e: IOException) {
        NetworkResults.Error("Network error — check your internet connection")
    } catch (e: Exception) {
        NetworkResults.Error(e.message ?: "An unexpected error occurred")
    } as NetworkResults<T>
}

fun <T> safeStreamingFlow(block: suspend () ->Flow<T>): Flow<NetworkResults<T>> = flow{
    emit(NetworkResults.Loading)
    try {
        block().collect { token ->
            emit(NetworkResults.Success(token))
        }
    } catch ( e: HttpException){
        emit(NetworkResults.Error("HTTP ${e.code()}: ${e.message()}", e.code()))
    } catch (e: SocketTimeoutException) {
        emit(NetworkResults.Error("Stream timed out"))
    } catch (e: IOException) {
        emit(NetworkResults.Error("Stream interrupted — check your connection"))
    } catch (e: Exception) {
        emit(NetworkResults.Error(e.message ?: "Streaming error"))
    }
}

@Singleton
class NetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val isOnline: Boolean
        @SuppressLint("ServiceCast")
        get() {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                    as ConnectivityManager
            val network = cm.activeNetwork ?: return false
            val caps    = cm.getNetworkCapabilities(network) ?: return false
            return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }
}



class ApiKeyInterceptor(private val apiKey: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original: Request = chain.request()

        val newRequest = original.newBuilder()
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .build()

        return chain.proceed(newRequest)
    }
}




class RetryInterceptor(private val maxRetries: Int = 3) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var attempt  = 0
        var lastException: IOException? = null

        while (attempt < maxRetries) {
            try {
                val response = chain.proceed(request)
                if (response.isSuccessful || response.code < 500) return response
                response.close()
            } catch (e: IOException) {
                lastException = e
            }
            attempt++
            if (attempt < maxRetries) Thread.sleep(500L * attempt)
        }

        throw lastException ?: IOException("Request failed after $maxRetries attempts")
    }
}
