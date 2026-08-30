package com.example.ai

import com.example.BuildConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@Serializable
data class Content(
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String? = null
)

@Serializable
data class GenerationConfig(
    val responseMimeType: String? = null,
    val temperature: Float? = null
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate>
)

@Serializable
data class Candidate(
    val content: Content
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

class RetryInterceptor(private val maxRetries: Int = 3) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        var response: Response? = null
        var exception: Exception? = null
        var tryCount = 0
        var backoffMs = 2000L
        
        while (tryCount <= maxRetries) {
            try {
                response = chain.proceed(request)
                if (response.isSuccessful) {
                    return response
                } else if (tryCount < maxRetries) {
                    response.close()
                }
            } catch (e: Exception) {
                exception = e
                if (tryCount == maxRetries) break
            }
            tryCount++
            if (tryCount <= maxRetries) {
                try {
                    Thread.sleep(backoffMs)
                } catch (e: InterruptedException) {
                    // ignore
                }
                backoffMs *= 2
            }
        }
        return response ?: throw exception ?: IOException("Unknown error during retries")
    }
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectionPool(okhttp3.ConnectionPool(5, 5, TimeUnit.MINUTES))
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(RetryInterceptor(maxRetries = 3))
        .build()

    val service: GeminiApiService by lazy {
        val json = Json { ignoreUnknownKeys = true }
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

class AiIdentityGenerator(private val context: android.content.Context) {
    companion object {
        private var lastRequestTime = 0L
        private const val MIN_INTERVAL_MS = 5000L // 5 seconds rate limit
        private val FALLBACK_JSON = """{"fakeName": "John Doe (Offline)", "jobTitle": "Developer", "location": "Seattle, US", "dob": "1990-01-01", "address": "123 Mock St", "email": "john.doe@example.com", "bio": "A passionate developer building secure applications.", "avatarSeed": "XyZ123AB", "passwords": ["Secur3P@ss!", "An0th3r0ne!"]}"""
    }

    suspend fun generateIdentity(prompt: String): String = withContext(Dispatchers.IO) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastRequestTime < MIN_INTERVAL_MS) {
            delay(MIN_INTERVAL_MS - (currentTime - lastRequestTime))
        }
        lastRequestTime = System.currentTimeMillis()

        val vaultManager = com.example.data.VaultManager(context)
        var apiKey = vaultManager.getGeminiApiKey()
        if (apiKey.isBlank()) apiKey = BuildConfig.GEMINI_API_KEY
        
        if (apiKey.isEmpty() || apiKey.contains("MY_GEMINI_API_KEY")) {
            return@withContext FALLBACK_JSON
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(
                parts = listOf(Part(text = "Generate a JSON string simulating a fake persona. Prompt: $prompt\nOutput format: {\"fakeName\": \"Jane\", \"jobTitle\": \"Engineer\", \"location\": \"Berlin, DE\", \"dob\": \"1992-05-15\", \"address\": \"Alexanderplatz 1, 10178 Berlin\", \"email\": \"jane.eng@example.com\", \"bio\": \"Software engineer focusing on backend systems. Loves solving complex problems.\", \"avatarSeed\": \"aB8sD9kL\", \"passwords\": [\"Pass1!\", \"Pass2@\"]}"))
            )),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.8f
            ),
            systemInstruction = Content(
                parts = listOf(Part(text = "You are a privacy proxy that generates anonymous realistic fake user personas based on user requests. Return ONLY strict valid JSON matching the exact requested structure, without markdown wrapping."))
            )
        )
        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: FALLBACK_JSON
        } catch (e: Exception) {
            FALLBACK_JSON
        }
    }
}
