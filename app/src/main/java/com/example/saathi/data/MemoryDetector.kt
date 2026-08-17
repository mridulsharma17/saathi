package com.example.saathi.data

import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class MemoryDetector(private val database: FirebaseDatabase) {
    private val TAG = "MemoryDetector"
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val MEMORY_SYSTEM_PROMPT = """
        You are a memory detection system. Analyze the latest conversation exchange and determine if it represents an important life event worth saving.
        
        Important events include: Relationship milestones, career achievements, college admissions, job offers, placements, travel, birthdays, family events, turning points, major goals completed, or important decisions.
        
        Do NOT save: Small talk, greetings, coding help, casual questions, weather, or temporary discussions.
        
        Return ONLY valid JSON in this format:
        {
          "save": true,
          "category": "category_name",
          "title": "Short Title",
          "description": "Brief description of the event",
          "importance": 1-10
        }
        If nothing important happened:
        { "save": false }
    """.trimIndent()

    suspend fun detectMemory(userMsg: String, botMsg: String): JourneyMemory? {
        Log.d("JourneyDebug", "--------------------------------")
        Log.d("JourneyDebug", "STEP 1\nUser Message:\n$userMsg")
        Log.d("JourneyDebug", "--------------------------------")
        Log.d("JourneyDebug", "STEP 2\nAI Response:\n$botMsg")
        Log.d("JourneyDebug", "--------------------------------")

        val snapshot = try {
            database.getReference("api_keys").get().await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load API keys", e)
            Log.d("JourneyDebug", "STEP 3: Failed to load API keys")
            return null
        }

        val keysToTry = listOf("gemni_1", "gemni_2", "gemni_3", "grok_1", "grok_2", "grok_3")
        val combinedContent = "User: $userMsg\nAssistant: $botMsg"

        Log.d("JourneyDebug", "STEP 3\nMemory Prompt:\n$MEMORY_SYSTEM_PROMPT\n\nCONTENT:\n$combinedContent")
        Log.d("JourneyDebug", "--------------------------------")

        for (keyName in keysToTry) {
            val apiKey = snapshot.child(keyName).value?.toString() ?: continue
            val response = if (keyName.contains("gemni")) {
                callGeminiApi(apiKey, combinedContent)
            } else {
                callOpenRouterApi(apiKey, combinedContent)
            }

            Log.d("JourneyDebug", "STEP 4\nRaw Memory Response (via $keyName):\n$response")
            Log.d("JourneyDebug", "--------------------------------")

            if (response != null) {
                try {
                    // Extract JSON if AI wrapped it in markdown or text
                    val start = response.indexOf('{')
                    val end = response.lastIndexOf('}')
                    val jsonString = if (start != -1 && end != -1) response.substring(start, end + 1) else response
                    
                    val json = JSONObject(jsonString)
                    val save = json.optBoolean("save", false)
                    
                    if (save) {
                        val memory = JourneyMemory(
                            title = json.optString("title", "Important Memory"),
                            description = json.optString("description", ""),
                            category = json.optString("category", "general"),
                            importance = json.optInt("importance", 5),
                            createdAt = System.currentTimeMillis(),
                            sourceMessage = userMsg,
                            aiGenerated = true
                        )
                        Log.d("JourneyDebug", "STEP 5\nParsed Memory\nsave = true\ntitle = ${memory.title}\ndescription = ${memory.description}\ncategory = ${memory.category}\nimportance = ${memory.importance}")
                        Log.d("JourneyDebug", "--------------------------------")
                        Log.d("JourneyDebug", "STEP 6\ndetectMemory returned JourneyMemory(title=${memory.title}, category=${memory.category})")
                        return memory
                    } else {
                        Log.d("JourneyDebug", "STEP 5\nParsed Memory\nsave = false for $keyName. Continuing loop to try other keys.")
                        continue // FIX 1: Don't stop, try the next key
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse memory JSON: $response", e)
                    Log.d("JourneyDebug", "STEP 5: JSON Parsing Failed for $keyName. Exception: ${e.message}\nRaw JSON: $response")
                }
            }
        }
        Log.d("JourneyDebug", "STEP 6\ndetectMemory returned NULL (all keys failed or returned save=false)")
        return null
    }

    private suspend fun callGeminiApi(apiKey: String, content: String): String? {
        return withContext(Dispatchers.IO) {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val json = JSONObject().apply {
                put("system_instruction", JSONObject().apply {
                    put("parts", org.json.JSONArray().apply { put(JSONObject().apply { put("text", MEMORY_SYSTEM_PROMPT) }) })
                })
                put("contents", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", org.json.JSONArray().apply { put(JSONObject().apply { put("text", content) }) })
                    })
                })
            }
            try {
                val request = Request.Builder().url(url).post(json.toString().toRequestBody("application/json".toMediaType())).build()
                httpClient.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: ""
                    if (response.isSuccessful) {
                        JSONObject(body).getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts")
                            .getJSONObject(0).getString("text")
                    } else {
                        Log.e(TAG, "Gemini Memory Error: ${response.code} - $body")
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gemini Memory Exception", e)
                null
            }
        }
    }

    private suspend fun callOpenRouterApi(apiKey: String, content: String): String? {
        return withContext(Dispatchers.IO) {
            val url = "https://openrouter.ai/api/v1/chat/completions"
            val json = JSONObject().apply {
                put("model", "openrouter/free")
                put("messages", org.json.JSONArray().apply {
                    put(JSONObject().apply { put("role", "system"); put("content", MEMORY_SYSTEM_PROMPT) })
                    put(JSONObject().apply { put("role", "user"); put("content", content) })
                })
            }
            try {
                val request = Request.Builder().url(url).header("Authorization", "Bearer $apiKey").post(json.toString().toRequestBody("application/json".toMediaType())).build()
                httpClient.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: ""
                    if (response.isSuccessful) {
                        JSONObject(body).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
                    } else {
                        Log.e(TAG, "OpenRouter Memory Error: ${response.code} - $body")
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "OpenRouter Memory Exception", e)
                null
            }
        }
    }
}
