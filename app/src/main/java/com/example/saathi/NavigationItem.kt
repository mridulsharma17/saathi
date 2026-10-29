package com.example.saathi

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavigationItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Chat : NavigationItem(
        route = "chat",
        title = "Chat",
        selectedIcon = Icons.Filled.ChatBubble,
        unselectedIcon = Icons.Outlined.ChatBubbleOutline
    )

    object Journey : NavigationItem(
        route = "journey",
        title = "Journey",
        selectedIcon = Icons.Filled.Eco,
        unselectedIcon = Icons.Outlined.Eco
    )

    object Settings : NavigationItem(
        route = "settings",
        title = "Profile",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )

    object SmartMemories : NavigationItem(
        route = "smart_memories",
        title = "Memories",
        selectedIcon = Icons.Filled.Person, // We can use something else if needed
        unselectedIcon = Icons.Outlined.Person
    )
}
