package com.example.saathi.data

import android.content.Context
import com.example.saathi.screens.ChatMessage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class LocalChatRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("saathi_local_chats", Context.MODE_PRIVATE)
    private val globalPrefs = context.getSharedPreferences("saathi_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getStorageMode(): String {
        return globalPrefs.getString("chat_storage_mode", "cloud") ?: "cloud"
    }

    fun setStorageMode(mode: String) {
        globalPrefs.edit().putString("chat_storage_mode", mode).apply()
    }

    fun saveLastUserTextTime(userId: String, timestamp: Long) {
        globalPrefs.edit().putLong("last_user_text_time_$userId", timestamp).apply()
    }

    fun getLastUserTextTime(userId: String): Long {
        return globalPrefs.getLong("last_user_text_time_$userId", 0L)
    }

    fun saveLocalMessage(userId: String, message: ChatMessage) {
        val currentMessages = getLocalMessages(userId).toMutableList()
        val exists = currentMessages.any { 
            (it.id.isNotEmpty() && it.id == message.id) || 
            (it.timestamp == message.timestamp && it.text == message.text)
        }
        if (!exists) {
            currentMessages.add(message)
            val json = gson.toJson(currentMessages)
            prefs.edit().putString("local_chat_$userId", json).apply()
        }
    }

    fun getLocalMessages(userId: String): List<ChatMessage> {
        val json = prefs.getString("local_chat_$userId", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<ChatMessage>>() {}.type
            gson.fromJson<List<ChatMessage>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
