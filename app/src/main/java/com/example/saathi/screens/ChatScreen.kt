package com.example.saathi.screens

import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.saathi.ui.theme.*
import com.example.saathi.data.JourneyMemory
import com.example.saathi.data.JourneyRepository
import com.example.saathi.viewmodel.ChatViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

// Constants
private const val TAG = "ChatScreen"
private const val INITIAL_PAGE_SIZE = 25
private const val PAGE_INCREMENT = 25

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val timestamp: Long = 0,
    val replyToId: String? = null,
    val journeyTitle: String? = null,
    val journeyImportance: Int? = null
)

@Composable
fun ChatScreen(
    isDarkTheme: Boolean,
    chatViewModel: ChatViewModel = viewModel()
) {
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: return
    val database = FirebaseDatabase.getInstance("https://saathi-5f51d-default-rtdb.firebaseio.com/")
    val chatRef = database.getReference("chats").child(userId)
    val context = androidx.compose.ui.platform.LocalContext.current
    val localChatRepo = remember { com.example.saathi.data.LocalChatRepository(context) }
    
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var inputText by remember { mutableStateOf("") }

    val isAiThinking by chatViewModel.isAiThinking.collectAsState()
    val messageEvent by chatViewModel.messageEvent.collectAsState()
    
    // Pagination & State
    var messageLimit by remember { mutableIntStateOf(INITIAL_PAGE_SIZE) }
    var hasMoreMessages by remember { mutableStateOf(false) }
    var isLoadingOlder by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Refresh Local + Cloud messages whenever ChatViewModel saves a new message
    LaunchedEffect(messageEvent) {
        if (messageEvent > 0L) {
            val localList = localChatRepo.getLocalMessages(userId)
            val combined = (messages + localList).distinctBy { msg ->
                if (msg.id.isNotEmpty()) msg.id else "${msg.timestamp}_${msg.text}"
            }.sortedBy { it.timestamp }
            messages = combined
        }
    }

    // Observe Chat Messages from Firebase + Local Storage with Pagination
    DisposableEffect(userId, messageLimit) {
        val query = chatRef.orderByChild("timestamp").limitToLast(messageLimit)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val cloudList = mutableListOf<ChatMessage>()
                snapshot.children.forEach { child ->
                    val msg = child.getValue(ChatMessage::class.java)?.copy(id = child.key ?: "")
                    if (msg != null) cloudList.add(msg)
                }
                
                val localList = localChatRepo.getLocalMessages(userId)
                val combined = (cloudList + localList).distinctBy { msg ->
                    if (msg.id.isNotEmpty()) msg.id else "${msg.timestamp}_${msg.text}"
                }.sortedBy { it.timestamp }
                
                messages = combined
                hasMoreMessages = snapshot.childrenCount.toInt() >= messageLimit
                isLoadingOlder = false
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error fetching chats", error.toException())
                val localList = localChatRepo.getLocalMessages(userId).sortedBy { it.timestamp }
                messages = localList
                isLoadingOlder = false
            }
        }
        query.addValueEventListener(listener)
        onDispose { query.removeEventListener(listener) }
    }

    // Pagination Scroll Listener: Load older messages when user reaches top
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { firstIndex ->
                if (firstIndex <= 1 && hasMoreMessages && !isLoadingOlder && messages.size >= messageLimit) {
                    isLoadingOlder = true
                    messageLimit += PAGE_INCREMENT
                }
            }
    }

    // Scroll to bottom on new message send
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty() && !isLoadingOlder) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    ChatBackground(isDarkTheme = isDarkTheme) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
        ) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (messages.isEmpty() && !isAiThinking) {
                    // Polished Empty State Banner with Quick Suggestion Chips
                    ChatEmptyState(onSuggestionClick = { prompt ->
                        inputText = prompt
                    })
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
                    ) {
                        // Top loading indicator when pulling older messages
                        if (isLoadingOlder) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = PrimaryPurple,
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        }

                        // Render Messages with Date Section Headers
                        itemsIndexed(
                            items = messages,
                            key = { _, msg -> msg.id.ifEmpty { msg.timestamp.toString() } }
                        ) { index, message ->
                            val showDateHeader = index == 0 || !isSameDay(messages[index].timestamp, messages[index - 1].timestamp)
                            if (showDateHeader) {
                                DateHeaderItem(timestamp = message.timestamp)
                            }

                            ChatBubble(
                                message = message,
                                isFromMe = message.senderId == userId,
                                allMessages = messages
                            )
                        }

                        // Typing / Thinking Indicator
                        if (isAiThinking) {
                            item {
                                RahiTypingBubble()
                            }
                        }
                    }
                }
            }

            // Clean Modern Chat Input Bar
            ChatInput(
                value = inputText,
                onValueChange = { inputText = it },
                onSend = {
                    val trimmedText = inputText.trim()
                    if (trimmedText.isNotBlank()) {
                        inputText = ""
                        scope.launch {
                            try {
                                val storageMode = localChatRepo.getStorageMode()
                                val newRef = chatRef.push()
                                val msgId = if (storageMode == "cloud") newRef.key ?: UUID.randomUUID().toString() else "local_" + System.currentTimeMillis()
                                val userMsg = ChatMessage(
                                    id = msgId,
                                    senderId = userId,
                                    text = trimmedText,
                                    timestamp = System.currentTimeMillis()
                                )
                                
                                localChatRepo.saveLastUserTextTime(userId, userMsg.timestamp)
                                
                                if (storageMode == "cloud") {
                                    newRef.setValue(userMsg).await()
                                } else {
                                    localChatRepo.saveLocalMessage(userId, userMsg)
                                    val cloudList = messages.filter { !it.id.startsWith("local_") }
                                    val localList = localChatRepo.getLocalMessages(userId)
                                    messages = (cloudList + localList).distinctBy { msg ->
                                        if (msg.id.isNotEmpty()) msg.id else "${msg.timestamp}_${msg.text}"
                                    }.sortedBy { it.timestamp }
                                }
                                
                                chatViewModel.sendMessage(userId, userMsg, storageMode, localChatRepo)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error sending message", e)
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun ChatBackground(
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bgPulse")
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.04f,
        targetValue = 0.09f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val bgGradient = if (isDarkTheme) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0D0B18),
                Color(0xFF161227),
                Color(0xFF0D0B18)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFAF8FF),
                Color(0xFFF3EDFC),
                Color(0xFFFAF8FF)
            )
        )
    }

    val glowColor1 = PrimaryPurple.copy(alpha = if (isDarkTheme) pulseAlpha else pulseAlpha * 0.5f)
    val glowColor2 = Color(0xFF7C4DFF).copy(alpha = if (isDarkTheme) pulseAlpha * 0.8f else pulseAlpha * 0.4f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgGradient)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor1, Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.15f),
                    radius = size.width * 0.65f * pulseScale
                ),
                center = Offset(size.width * 0.85f, size.height * 0.15f),
                radius = size.width * 0.65f * pulseScale
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor2, Color.Transparent),
                    center = Offset(size.width * 0.15f, size.height * 0.75f),
                    radius = size.width * 0.75f * pulseScale
                ),
                center = Offset(size.width * 0.15f, size.height * 0.75f),
                radius = size.width * 0.75f * pulseScale
            )

            val patternColor = (if (isDarkTheme) Color.White else Color(0xFF5B308C)).copy(alpha = 0.035f)
            val step = 44.dp.toPx()
            var x = 22.dp.toPx()
            while (x < size.width) {
                var y = 22.dp.toPx()
                while (y < size.height) {
                    drawCircle(
                        color = patternColor,
                        radius = 1.5.dp.toPx(),
                        center = Offset(x, y)
                    )
                    y += step
                }
                x += step
            }
        }

        content()
    }
}

@Composable
fun DateHeaderItem(timestamp: Long) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = formatDateHeader(timestamp),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun RahiTypingBubble() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(PrimaryPurple.copy(alpha = 0.3f), PrimaryPurple.copy(alpha = 0.1f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("✨", fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Rahi is typing",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "•••",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryPurple.copy(alpha = alpha)
                )
            }
        }
    }
}

@Composable
fun ChatEmptyState(onSuggestionClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(PrimaryPurple.copy(alpha = 0.2f), PrimaryPurple.copy(alpha = 0.05f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("✨", fontSize = 36.sp)
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Meet Rahi 👋",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Your witty, supportive AI companion. Ask anything or share what's on your mind!",
            style = MaterialTheme.typography.bodyMedium,
            color = SecondaryText,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Try asking:",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = SecondaryText
        )

        Spacer(modifier = Modifier.height(12.dp))

        val suggestions = listOf(
            "How was your day? 😊",
            "Give me a quick daily boost 🚀",
            "Let's talk about my goals 🎯",
            "Tell me something interesting 💡"
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            suggestions.forEach { prompt ->
                Surface(
                    onClick = { onSuggestionClick(prompt) },
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Text(
                        text = prompt,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

private fun formatDateHeader(timestamp: Long): String {
    val messageCal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val nowCal = Calendar.getInstance()
    
    return when {
        isSameDay(timestamp, System.currentTimeMillis()) -> "Today"
        isYesterday(messageCal, nowCal) -> "Yesterday"
        else -> SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}

private fun isSameDay(t1: Long, t2: Long): Boolean {
    val c1 = Calendar.getInstance().apply { timeInMillis = t1 }
    val c2 = Calendar.getInstance().apply { timeInMillis = t2 }
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
            c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}

private fun isYesterday(messageCal: Calendar, nowCal: Calendar): Boolean {
    val yesterday = (nowCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
    return messageCal.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
            messageCal.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR)
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    isFromMe: Boolean,
    allMessages: List<ChatMessage>
) {
    val isDarkTheme = MaterialTheme.colorScheme.background != LightBackground
    val repliedToMessage = message.replyToId?.let { id -> allMessages.find { it.id == id } }
    val screenWidth = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp
    val maxBubbleWidth = screenWidth * 0.70f
    
    val formattedTime = remember(message.timestamp) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(message.timestamp))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isFromMe) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(PrimaryPurple.copy(alpha = 0.3f), PrimaryPurple.copy(alpha = 0.1f))
                        )
                    ), 
                contentAlignment = Alignment.Center
            ) {
                Text("✨", fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        Column(
            horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = maxBubbleWidth)
        ) {
            val showTag = remember(message, allMessages) {
                if (isFromMe || message.replyToId == null) false
                else {
                    val uIndex = allMessages.indexOfFirst { it.id == message.replyToId }
                    val bIndex = allMessages.indexOf(message)
                    if (uIndex == -1 || bIndex == -1) false
                    else {
                        val prevMessageWasUser = uIndex > 0 && allMessages[uIndex - 1].senderId != "rahi"
                        val anyUserMessageBetween = allMessages.subList(uIndex + 1, bIndex).any { it.senderId != "rahi" }
                        prevMessageWasUser || anyUserMessageBetween
                    }
                }
            }

            if (repliedToMessage != null && showTag) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(bottom = 4.dp).widthIn(max = maxBubbleWidth)
                ) {
                    Text(
                        text = repliedToMessage.text,
                        fontSize = 12.sp,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Surface(
                color = if (isFromMe) PrimaryPurple else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                shape = RoundedCornerShape(
                    topStart = 18.dp, 
                    topEnd = 18.dp, 
                    bottomStart = if (isFromMe) 18.dp else 4.dp, 
                    bottomEnd = if (isFromMe) 4.dp else 18.dp
                ),
                shadowElevation = if (isDarkTheme) 0.dp else 0.5.dp
            ) {
                Column(
                    modifier = Modifier.padding(start = 12.dp, end = 10.dp, top = 7.dp, bottom = 6.dp)
                ) {
                    Text(
                        text = message.text, 
                        color = if (isFromMe) Color.White else MaterialTheme.colorScheme.onSurface, 
                        fontSize = 15.sp,
                        lineHeight = 20.sp
                    )

                    if (message.journeyTitle != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            color = Color(0xFFFFF8E1),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "🌟 Journey Milestone",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE65100)
                                    )
                                    if (message.journeyImportance != null) {
                                        Text(
                                            text = "⭐ ${message.journeyImportance}/10",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFE65100)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "\"${message.journeyTitle}\"",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFBF360C)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formattedTime, 
                        color = (if (isFromMe) Color.White else MaterialTheme.colorScheme.onSurface).copy(alpha = 0.6f), 
                        fontSize = 10.sp, 
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInput(value: String, onValueChange: (String) -> Unit, onSend: () -> Unit) {
    Surface(
        color = Color.Transparent, 
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .fillMaxWidth(), 
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { }, 
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), 
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.Add, 
                    contentDescription = "Add", 
                    tint = PrimaryPurple
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            
            TextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text("Message Rahi...", color = SecondaryText, fontSize = 15.sp) },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp, max = 150.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), 
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), 
                    focusedIndicatorColor = Color.Transparent, 
                    unfocusedIndicatorColor = Color.Transparent, 
                    cursorColor = PrimaryPurple,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface, 
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(28.dp)
            )
            
            Spacer(modifier = Modifier.width(10.dp))
            
            IconButton(
                onClick = onSend, 
                enabled = value.isNotBlank(),
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        if (value.isNotBlank()) PrimaryPurple else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send, 
                    contentDescription = "Send", 
                    tint = if (value.isNotBlank()) Color.White else SecondaryText
                )
            }
        }
    }
}
