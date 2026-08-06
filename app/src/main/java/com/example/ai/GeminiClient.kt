package com.example.ai

import com.example.BuildConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import kotlinx.coroutines.Dispatchers
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
    val responseFormat: ResponseFormat? = null,
    val temperature: Float? = null
)

@Serializable
data class ResponseFormat(
    val text: ResponseFormatText? = null
)

@Serializable
data class ResponseFormatText(
    val mimeType: String,
    val schema: JsonObject? = null
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

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
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
    suspend fun generateIdentity(prompt: String): String = withContext(Dispatchers.IO) {
        val vaultManager = com.example.data.VaultManager(context)
        var apiKey = vaultManager.getGeminiApiKey()
        if (apiKey.isBlank()) apiKey = BuildConfig.GEMINI_API_KEY
        
        if (apiKey.isEmpty() || apiKey.contains("MY_GEMINI_API_KEY")) {
            return@withContext "{\"fakeName\": \"John Doe (Fallback)\", \"jobTitle\": \"Data Analyst\", \"location\": \"Seattle, US\", \"dob\": \"1985-10-22\", \"address\": \"123 Mock St\", \"email\": \"j.doe@example.com\"}"
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(
                parts = listOf(Part(text = "Generate a JSON string simulating a fake persona. Prompt: $prompt\nOutput format: {\"fakeName\": \"Jane\", \"jobTitle\": \"Engineer\", \"location\": \"Berlin, DE\", \"dob\": \"1992-05-15\", \"address\": \"Alexanderplatz 1, 10178 Berlin\", \"email\": \"jane.eng@example.com\"}"))
            )),
            generationConfig = GenerationConfig(
                temperature = 0.8f
            ),
            systemInstruction = Content(
                parts = listOf(Part(text = "You are a privacy proxy that generates anonymous realistic fake user personas based on user requests. Return ONLY strict JSON matching the requested keys, without markdown wrapping."))
            )
        )
        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No response text"
        } catch (e: Exception) {
            "{\"fakeName\": \"Error Fallback\", \"jobTitle\": \"Unknown\", \"location\": \"Internet\", \"dob\": \"\", \"address\": \"\", \"email\": \"\"}"
        }
    }
}
