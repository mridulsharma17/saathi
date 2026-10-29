package com.example.saathi.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.saathi.data.LocalChatRepository
import com.example.saathi.screens.ChatMessage
import com.example.saathi.util.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

data class CheckinResult(
    val reply: String?,
    val updatedContext: String?
)

class InactivityWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val TAG = "InactivityWorker"
    private val INACTIVITY_THRESHOLD_MS = 15 * 60 * 60 * 1000L // 15 Hours in milliseconds
    private val CHECKIN_COOLDOWN_MS = 15 * 60 * 60 * 1000L

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val auth = FirebaseAuth.getInstance()
            val user = auth.currentUser
            
            if (user != null) {
                val userId = user.uid
                val database = FirebaseDatabase.getInstance("https://saathi-5f51d-default-rtdb.firebaseio.com/")
                val chatRef = database.getReference("chats").child(userId)
                val contextRef = database.getReference("conversation_context").child(userId)
                val localChatRepo = LocalChatRepository(appContext)

                val currentTime = System.currentTimeMillis()

                // 1. Fetch timestamp of the last text sent specifically by the USER (not Rahi/app)
                var lastUserMessageTimestamp = localChatRepo.getLastUserTextTime(userId)

                if (lastUserMessageTimestamp == 0L) {
                    // Fallback: Scan Cloud & Local for the latest message where senderId != "rahi"
                    try {
                        val cloudSnapshot = chatRef.orderByChild("timestamp").limitToLast(10).get().await()
                        cloudSnapshot.children.forEach { child ->
                            val msg = child.getValue(ChatMessage::class.java)
                            if (msg != null && msg.senderId != "rahi" && msg.timestamp > lastUserMessageTimestamp) {
                                lastUserMessageTimestamp = msg.timestamp
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error fetching cloud user message", e)
                    }

                    val localUserMsgs = localChatRepo.getLocalMessages(userId).filter { it.senderId != "rahi" }
                    localUserMsgs.maxByOrNull { it.timestamp }?.let { msg ->
                        if (msg.timestamp > lastUserMessageTimestamp) {
                            lastUserMessageTimestamp = msg.timestamp
                        }
                    }
                }

                val timeDiffMs = currentTime - lastUserMessageTimestamp

                val prefs = appContext.getSharedPreferences("saathi_worker_prefs", Context.MODE_PRIVATE)
                val lastCheckinTime = prefs.getLong("last_checkin_time_$userId", 0L)
                val timeSinceLastCheckin = currentTime - lastCheckinTime

                // Check if user's last text is older than 15 hours AND cooldown passed
                if (lastUserMessageTimestamp > 0 && timeDiffMs >= INACTIVITY_THRESHOLD_MS && timeSinceLastCheckin >= CHECKIN_COOLDOWN_MS) {
                    Log.d(TAG, "User inactivity threshold exceeded (>= 15 hours). Locking check-in timestamp before API call...")

                    // ATOMIC LOCK: Update lastCheckinTime IMMEDIATELY with commit() before making the API call to prevent duplicate parallel executions!
                    prefs.edit().putLong("last_checkin_time_$userId", currentTime).commit()

                    val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy 'at' h:mm a", Locale.getDefault())
                    val lastMsgDateStr = dateFormat.format(Date(lastUserMessageTimestamp))
                    val currentDateStr = dateFormat.format(Date(currentTime))
                    val timeDiffText = formatTimeDifference(timeDiffMs)

                    // Fetch conversation context
                    val currentContext = try {
                        val contextSnapshot = contextRef.get().await()
                        contextSnapshot.value?.toString() ?: "No previous conversation context is available."
                    } catch (e: Exception) {
                        "No previous conversation context is available."
                    }

                    val apiKeysSnapshot = try {
                        database.getReference("api_keys").get().await()
                    } catch (e: Exception) { null }

                    // Generate customized AI check-in adhering to full Rahi rules & persona
                    val result = generateCheckinReplyAndContext(
                        apiKeysSnapshot = apiKeysSnapshot,
                        context = currentContext,
                        lastMsgDateStr = lastMsgDateStr,
                        currentDateStr = currentDateStr,
                        timeDiffText = timeDiffText
                    )

                    if (result != null && !result.reply.isNullOrBlank()) {
                        val replyText = result.reply
                        val storageMode = localChatRepo.getStorageMode()
                        val botMsgId = if (storageMode == "cloud") chatRef.push().key ?: UUID.randomUUID().toString() else "local_" + System.currentTimeMillis()
                        
                        val botMsg = ChatMessage(
                            id = botMsgId,
                            senderId = "rahi",
                            text = replyText,
                            timestamp = currentTime
                        )

                        if (storageMode == "cloud") {
                            try {
                                chatRef.child(botMsgId).setValue(botMsg).await()
                            } catch (e: Exception) {
                                localChatRepo.saveLocalMessage(userId, botMsg)
                            }
                        } else {
                            localChatRepo.saveLocalMessage(userId, botMsg)
                        }

                        // Update rolling context with Rahi's check-in
                        if (!result.updatedContext.isNullOrBlank()) {
                            try {
                                contextRef.setValue(result.updatedContext).await()
                            } catch (e: Exception) { }
                        }

                        // Send Push Notification
                        NotificationHelper.showCheckInNotification(appContext, replyText)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Inactivity worker error", e)
        } finally {
            // Re-enqueue work for periodic checking
            scheduleNextMinuteCheck(appContext)
        }

        Result.success()
    }

    private fun formatTimeDifference(timeDiffMs: Long): String {
        val hours = timeDiffMs / (1000 * 60 * 60)
        val days = hours / 24
        return when {
            days >= 1 -> "$days day${if (days > 1) "s" else ""}"
            else -> "$hours hour${if (hours > 1) "s" else ""}"
        }
    }

    private fun generateCheckinReplyAndContext(
        apiKeysSnapshot: com.google.firebase.database.DataSnapshot?,
        context: String,
        lastMsgDateStr: String,
        currentDateStr: String,
        timeDiffText: String
    ): CheckinResult? {
        if (apiKeysSnapshot == null) return null
        val keysToTry = listOf("gemni_1", "gemni_2", "gemni_3", "grok_1", "grok_2")
        
        val checkinPrompt = """
            INSTRUCTION: The user sent their last text on $lastMsgDateStr. Current time is $currentDateStr.
            The time gap since the user's last text is $timeDiffText.
            Write a short, warm, caring WhatsApp-style check-in message (20-30 words) in Hindi/Hinglish/English naturally referencing the time gap (e.g., "Where have you been for the last $timeDiffText?", "Haven't heard from you in $timeDiffText!", etc.) and previous conversation context.
            Also update the rolling context (200-300 words) to incorporate Rahi's check-in message.
        """.trimIndent()

        val fullInput = "Previous Context: $context\n\nTask: $checkinPrompt"

        val systemPrompt = """
            You are Rahi, a close Indian friend. You are witty, natural, and emotionally intelligent.
            
            YOUR CORE IDENTITY:
            - Talk like a close Indian friend on WhatsApp. Use Hindi, Hinglish, or English naturally.
            - Be honest. Don't always agree. Give "truth bombs" when needed.
            - Be caring during hard times, celebrate wins, and tease lightly when appropriate.
            - Keep replies short (20-50 words) unless details are requested.

            Return ONLY a valid JSON:
            {
              "reply": "Your check-in message here",
              "context": "Updated rolling context incorporating this check-in message"
            }
        """.trimIndent()

        val httpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        for (keyName in keysToTry) {
            val apiKey = apiKeysSnapshot.child(keyName).value?.toString() ?: continue
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val json = JSONObject().apply {
                put("system_instruction", JSONObject().apply {
                    put("parts", org.json.JSONArray().apply { put(JSONObject().apply { put("text", systemPrompt) }) })
                })
                put("contents", org.json.JSONArray().apply {
                    put(JSONObject().apply { put("parts", org.json.JSONArray().apply { put(JSONObject().apply { put("text", fullInput) }) }) })
                })
            }
            try {
                val request = Request.Builder().url(url).post(json.toString().toRequestBody("application/json".toMediaType())).build()
                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val candidates = JSONObject(body).getJSONArray("candidates")
                        val text = candidates.getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
                        val start = text.indexOf('{')
                        val end = text.lastIndexOf('}')
                        val jsonStr = if (start != -1 && end != -1) text.substring(start, end + 1) else text
                        val jsonObj = JSONObject(jsonStr)
                        
                        val reply = if (jsonObj.has("reply") && !jsonObj.isNull("reply")) jsonObj.getString("reply") else null
                        val updatedContext = if (jsonObj.has("context") && !jsonObj.isNull("context")) jsonObj.getString("context") else null
                        
                        if (!reply.isNullOrBlank()) {
                            return CheckinResult(reply = reply, updatedContext = updatedContext)
                        }
                    }
                }
            } catch (e: Exception) { }
        }
        return null
    }

    companion object {
        fun scheduleNextMinuteCheck(context: Context) {
            try {
                val nextWorkRequest = OneTimeWorkRequestBuilder<InactivityWorker>()
                    .setInitialDelay(1, TimeUnit.HOURS)
                    .build()

                WorkManager.getInstance(context).enqueueUniqueWork(
                    "RahiInactivityMinuteCheck",
                    ExistingWorkPolicy.REPLACE,
                    nextWorkRequest
                )
            } catch (e: Exception) {
                Log.e("InactivityWorker", "Error scheduling next minute check", e)
            }
        }
    }
}
