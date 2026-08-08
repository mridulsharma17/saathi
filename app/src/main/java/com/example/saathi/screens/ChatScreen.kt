package com.example.saathi.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saathi.ui.theme.ChatGrey
import com.example.saathi.ui.theme.ChatPurple
import com.example.saathi.ui.theme.DarkBackground
import com.example.saathi.ui.theme.AccentPurple
import com.example.saathi.ui.theme.LightBackground
import com.example.saathi.data.JourneyMemory
import com.example.saathi.data.JourneyRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

// Constants
private const val TAG = "ChatScreen"

private val UNIFIED_SYSTEM_PROMPT = """
You are Rahi, a close Indian friend. You are witty, natural, and emotionally intelligent. 

YOUR CORE IDENTITY:
- Talk like a close Indian friend on WhatsApp. Use Hindi, Hinglish, or English naturally.
- Be honest. Don't always agree. Give "truth bombs" when needed.
- Be caring during hard times, celebrate wins, and tease lightly when appropriate.
- Keep replies short (20-50 words) unless details are requested.

YOUR TASK:
You must process the current interaction and return ONLY a valid JSON object with three fields:
1. "reply": Your natural conversational response to the user.
2. "context": An updated rolling memory (200-300 words). Treat the previous context as the base and merge new facts. Preserve important events, goals, names, and unresolved topics. Do not store small talk.
3. "journey": If an important life event occurred (milestones, career, relationships, achievements), set "save": true and provide details. Otherwise, set "save": false.

OUTPUT FORMAT (JSON ONLY):
{
  "reply": "...",
  "context": "...",
  "journey": {
    "save": true/false,
    "category": "career/relationship/achievement/etc",
    "title": "...",
    "description": "...",
    "importance": 1-10
  }
}
""".trimIndent()

private const val DEFAULT_CONTEXT = "No previous conversation context is available. Treat this as the beginning of a new conversation."

private val httpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .writeTimeout(15, TimeUnit.SECONDS)
    .build()

data class ChatMessage(
    val senderId: String = "",
    val text: String = "",
    val timestamp: Long = 0
)

data class UnifiedAiResponse(
    val reply: String,
    val context: String,
    val journey: JourneyMemory?,
    val keyUsed: String
)

@Composable
fun ChatScreen(isDarkTheme: Boolean) {
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: return
    val database = FirebaseDatabase.getInstance("https://saathi-5f51d-default-rtdb.firebaseio.com/")
    val chatRef = database.getReference("chats").child(userId)
    val contextRef = database.getReference("conversation_context").child(userId)
    val journeyRepo = remember { JourneyRepository() }
    
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var inputText by remember { mutableStateOf("") }
    var showMemoryDialog by remember { mutableStateOf(false) }
    var detectedMemory by remember { mutableStateOf<JourneyMemory?>(null) }
    
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    DisposableEffect(userId) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newList = mutableListOf<ChatMessage>()
                snapshot.children.forEach { child ->
                    val msg = child.getValue(ChatMessage::class.java)
                    if (msg != null) newList.add(msg)
                }
                messages = newList.sortedBy { it.timestamp }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error fetching chats", error.toException())
            }
        }
        chatRef.addValueEventListener(listener)
        onDispose { chatRef.removeEventListener(listener) }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .imePadding()
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(messages) { message ->
                ChatBubble(
                    message = message.text,
                    time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(message.timestamp)),
                    isFromMe = message.senderId == userId
                )
            }
        }

        ChatInput(
            value = inputText,
            onValueChange = { inputText = it },
            onSend = {
                val trimmedText = inputText.trim()
                if (trimmedText.isNotBlank()) {
                    val userMsgText = trimmedText
                    val userMsg = ChatMessage(userId, userMsgText, System.currentTimeMillis())
                    inputText = ""
                    
                    scope.launch {
                        try {
                            chatRef.push().setValue(userMsg).await()

                            val contextSnapshot = contextRef.get().await()
                            val currentContext = contextSnapshot.value?.toString() ?: DEFAULT_CONTEXT
                            
                            val response = getUnifiedAiResponse(database, userMsgText, currentContext)
                            
                            // 1. Display and Save Reply
                            val botMsg = ChatMessage("rahi", "${response.reply}\n\n(via ${response.keyUsed})", System.currentTimeMillis())
                            chatRef.push().setValue(botMsg).await()
                            
                            // 2. Update Context
                            contextRef.setValue(response.context).await()
                            
                            // 3. Handle Journey Memory
                            if (response.journey != null) {
                                detectedMemory = response.journey.copy(sourceMessage = userMsgText)
                                showMemoryDialog = true
                            }
                            
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed chat cycle", e)
                        }
                    }
                }
            }
        )

        if (showMemoryDialog && detectedMemory != null) {
            AlertDialog(
                onDismissRequest = { showMemoryDialog = false },
                title = { Text("We found an important memory", color = Color.White) },
                text = {
                    Column {
                        Text(detectedMemory?.title ?: "", fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(detectedMemory?.description ?: "", color = Color.White.copy(alpha = 0.8f))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Category: ${detectedMemory?.category}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            detectedMemory?.let { journeyRepo.saveJourneyMemory(userId, it) }
                            showMemoryDialog = false
                        }
                    }) {
                        Text("Save", color = AccentPurple)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showMemoryDialog = false }) {
                        Text("Dismiss", color = Color.Gray)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                textContentColor = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private suspend fun getUnifiedAiResponse(database: FirebaseDatabase, prompt: String, context: String): UnifiedAiResponse {
    val snapshot = try {
        database.getReference("api_keys").get().await()
    } catch (e: Exception) {
        return UnifiedAiResponse("System error: Config unreachable.", context, null, "none")
    }

    val keysToTry = listOf("gemni_1", "gemni_2", "gemni_3", "grok_1", "grok_2", "grok_3")
    val fullInput = "Current Memory: $context\n\nUser Message: $prompt"

    for (keyName in keysToTry) {
        val apiKey = snapshot.child(keyName).value?.toString() ?: continue
        val rawResponse = if (keyName.contains("gemni")) callGeminiUnified(apiKey, fullInput) else callOpenRouterUnified(apiKey, fullInput)

        if (rawResponse != null) {
            try {
                // Extract JSON block
                val start = rawResponse.indexOf('{')
                val end = rawResponse.lastIndexOf('}')
                val jsonStr = if (start != -1 && end != -1) rawResponse.substring(start, end + 1) else rawResponse
                val json = JSONObject(jsonStr)

                val journey = if (json.optJSONObject("journey")?.optBoolean("save") == true) {
                    val jObj = json.getJSONObject("journey")
                    JourneyMemory(
                        title = jObj.getString("title"),
                        description = jObj.getString("description"),
                        category = jObj.getString("category"),
                        importance = jObj.getInt("importance"),
                        createdAt = System.currentTimeMillis()
                    )
                } else null

                return UnifiedAiResponse(
                    reply = json.getString("reply"),
                    context = json.getString("context"),
                    journey = journey,
                    keyUsed = keyName
                )
            } catch (e: Exception) {
                Log.e(TAG, "Parsing error for $keyName: ${e.message}")
            }
        }
    }
    return UnifiedAiResponse("All systems busy. Try again?", context, null, "none")
}

private suspend fun callGeminiUnified(apiKey: String, input: String): String? {
    return withContext(Dispatchers.IO) {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        val json = JSONObject().apply {
            put("system_instruction", JSONObject().apply {
                put("parts", org.json.JSONArray().apply { put(JSONObject().apply { put("text", UNIFIED_SYSTEM_PROMPT) }) })
            })
            put("contents", org.json.JSONArray().apply {
                put(JSONObject().apply { put("parts", org.json.JSONArray().apply { put(JSONObject().apply { put("text", input) }) }) })
            })
        }
        try {
            val request = Request.Builder().url(url).post(json.toString().toRequestBody("application/json".toMediaType())).build()
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    JSONObject(body).getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
                } else null
            }
        } catch (e: Exception) { null }
    }
}

private suspend fun callOpenRouterUnified(apiKey: String, input: String): String? {
    return withContext(Dispatchers.IO) {
        val url = "https://openrouter.ai/api/v1/chat/completions"
        val json = JSONObject().apply {
            put("model", "openrouter/free")
            put("messages", org.json.JSONArray().apply {
                put(JSONObject().apply { put("role", "system"); put("content", UNIFIED_SYSTEM_PROMPT) })
                put(JSONObject().apply { put("role", "user"); put("content", input) })
            })
        }
        try {
            val request = Request.Builder().url(url).header("Authorization", "Bearer $apiKey").post(json.toString().toRequestBody("application/json".toMediaType())).build()
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    JSONObject(body).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
                } else null
            }
        } catch (e: Exception) { null }
    }
}

@Composable
fun ChatBubble(message: String, time: String, isFromMe: Boolean) {
    val isDarkTheme = MaterialTheme.colorScheme.background != LightBackground
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isFromMe) {
            Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.Gray.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                Text("\u2728", fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        Surface(
            color = if (isFromMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = if (isFromMe) 16.dp else 0.dp, bottomEnd = if (isFromMe) 0.dp else 16.dp),
            shadowElevation = if (isDarkTheme) 0.dp else 1.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message, 
                    color = if (isFromMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, 
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = time, 
                    color = (if (isFromMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface).copy(alpha = 0.5f), 
                    fontSize = 10.sp, 
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInput(value: String, onValueChange: (String) -> Unit, onSend: () -> Unit) {
    val isDarkTheme = MaterialTheme.colorScheme.background != LightBackground
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { }, 
                modifier = Modifier.size(40.dp).background(
                    if (isDarkTheme) ChatGrey else MaterialTheme.colorScheme.surface, 
                    CircleShape
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            TextField(
                value = value, onValueChange = onValueChange,
                placeholder = { Text("Message Rahi...", color = Color.Gray, fontSize = 14.sp) },
                modifier = Modifier.weight(1f).heightIn(min = 44.dp, max = 120.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = if (isDarkTheme) ChatGrey else MaterialTheme.colorScheme.surface, 
                    unfocusedContainerColor = if (isDarkTheme) ChatGrey else MaterialTheme.colorScheme.surface, 
                    focusedIndicatorColor = Color.Transparent, 
                    unfocusedIndicatorColor = Color.Transparent, 
                    cursorColor = MaterialTheme.colorScheme.primary, 
                    focusedTextColor = MaterialTheme.colorScheme.onSurface, 
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onSend, 
                modifier = Modifier.size(44.dp).background(MaterialTheme.colorScheme.primary, CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
            }
        }
    }
}
