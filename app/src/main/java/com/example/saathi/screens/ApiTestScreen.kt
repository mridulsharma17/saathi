package com.example.saathi.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saathi.ui.theme.DarkBackground
import com.example.saathi.ui.theme.DarkSurface
import com.example.saathi.ui.theme.TextGrey
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "ApiTestScreen"

data class ApiKeyResult(
    val apiName: String,
    val keyName: String,
    val status: String,
    val time: String? = null,
    val reason: String? = null
)

@Composable
fun ApiTestScreen() {
    val results = remember { mutableStateListOf<ApiKeyResult>() }
    val scope = rememberCoroutineScope()
    var isTesting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp)
    ) {
        Text(
            text = "API Key Diagnostics",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (!isTesting) {
                    isTesting = true
                    results.clear()
                    scope.launch {
                        runDiagnostics { result ->
                            results.add(result)
                            logResult(result)
                        }
                        isTesting = false
                    }
                }
            },
            enabled = !isTesting,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isTesting) "Checking Keys..." else "Start Check")
        }

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(results) { result ->
                ApiResultItem(result)
            }
        }
    }
}

@Composable
fun ApiResultItem(result: ApiKeyResult) {
    Surface(
        color = DarkSurface,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${result.apiName} (${result.keyName})",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 18.sp
            )
            
            val statusColor = when (result.status) {
                "WORKING" -> Color.Green
                "RATE LIMITED" -> Color.Yellow
                else -> Color.Red
            }
            
            Text(
                text = "Status : ${result.status}",
                color = statusColor,
                fontWeight = FontWeight.Medium
            )
            
            if (result.time != null) {
                Text(text = "Time   : ${result.time}", color = TextGrey)
            }
            
            if (result.reason != null) {
                Text(text = "Reason : ${result.reason}", color = Color.Red.copy(alpha = 0.8f))
            }
        }
    }
}

private suspend fun runDiagnostics(onResult: (ApiKeyResult) -> Unit) {
    val database = FirebaseDatabase.getInstance("https://saathi-5f51d-default-rtdb.firebaseio.com/")
    val client = OkHttpClient()
    
    val snapshot = try {
        database.getReference("api_keys").get().await()
    } catch (e: Exception) {
        Log.e(TAG, "Firebase read failed", e)
        null
    }

    if (snapshot == null || !snapshot.exists()) {
        onResult(ApiKeyResult("Firebase", "N/A", "ERROR", reason = "Could not read api_keys from DB: ${snapshot == null}"))
        return
    }

    val keysToCheck = listOf("gemni_1", "gemni_2", "gemni_3", "grok_1", "grok_2", "grok_3")
    
    for (keyName in keysToCheck) {
        val apiKey = snapshot.child(keyName).value?.toString()
        
        if (apiKey.isNullOrBlank()) {
            onResult(ApiKeyResult(getApiType(keyName), keyName, "INVALID", reason = "Key missing in DB"))
            continue
        }

        val result = if (keyName.contains("gemni")) {
            testGeminiKey(client, keyName, apiKey)
        } else {
            testGrokKey(client, keyName, apiKey)
        }
        
        onResult(result)
    }
}

private fun getApiType(keyName: String) = if (keyName.contains("gemni")) "Gemini" else "Grok"

private suspend fun testGeminiKey(client: OkHttpClient, keyName: String, apiKey: String): ApiKeyResult {
    return withContext(Dispatchers.IO) {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"
        val json = """{"contents": [{"parts":[{"text": "Hello"}]}]}"""
        val requestBody = json.toRequestBody("application/json".toMediaType())
        
        val request = Request.Builder()
            .url(url)
            .header("x-goog-api-key", apiKey)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                when (response.code) {
                    200 -> ApiKeyResult("Gemini", keyName, "WORKING", time = now)
                    429 -> ApiKeyResult("Gemini", keyName, "RATE LIMITED")
                    else -> {
                        val body = response.body?.string() ?: ""
                        ApiKeyResult("Gemini", keyName, "INVALID", reason = "HTTP ${response.code}: $body")
                    }
                }
            }
        } catch (e: Exception) {
            ApiKeyResult("Gemini", keyName, "INVALID", reason = e.message)
        }
    }
}

private suspend fun testGrokKey(client: OkHttpClient, keyName: String, apiKey: String): ApiKeyResult {
    return withContext(Dispatchers.IO) {
        val url = "https://openrouter.ai/api/v1/chat/completions"
        val json = """
            {
                "model": "openrouter/free",
                "messages": [{"role": "user", "content": "Hello"}],
                "max_tokens": 5
            }
        """.trimIndent()
        val requestBody = json.toRequestBody("application/json".toMediaType())
        
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                when (response.code) {
                    200 -> ApiKeyResult("OpenRouter", keyName, "WORKING", time = now)
                    429 -> ApiKeyResult("OpenRouter", keyName, "RATE LIMITED")
                    401 -> ApiKeyResult("OpenRouter", keyName, "INVALID KEY")
                    402 -> ApiKeyResult("OpenRouter", keyName, "NO CREDITS")
                    403 -> ApiKeyResult("OpenRouter", keyName, "FORBIDDEN")
                    else -> {
                        val body = response.body?.string() ?: ""
                        ApiKeyResult("OpenRouter", keyName, "INVALID", reason = "HTTP ${response.code}: $body")
                    }
                }
            }
        } catch (e: Exception) {
            ApiKeyResult("OpenRouter", keyName, "INVALID", reason = e.message)
        }
    }
}

private fun logResult(result: ApiKeyResult) {
    val message = buildString {
        append("${result.apiName} (${result.keyName})\n")
        append("Status : ${result.status}\n")
        if (result.time != null) append("Time   : ${result.time}\n")
        if (result.reason != null) append("Reason : ${result.reason}\n")
    }
    Log.d(TAG, message)
}
