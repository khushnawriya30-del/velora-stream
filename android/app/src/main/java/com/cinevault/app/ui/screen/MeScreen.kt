package com.cinevault.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.cinevault.app.data.model.ProfileDto
import com.cinevault.app.ui.theme.CineVaultTheme
import com.cinevault.app.ui.viewmodel.AuthViewModel
import com.cinevault.app.ui.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeScreen(
    onLogout: () -> Unit,
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToWatchHistory: () -> Unit = {},
    profileViewModel: ProfileViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by profileViewModel.uiState.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Sign Out", color = CineVaultTheme.colors.textPrimary) },
            text = { Text("Are you sure you want to sign out?", color = CineVaultTheme.colors.textSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    authViewModel.logout()
                    onLogout()
                }) {
                    Text("Sign Out", color = CineVaultTheme.colors.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = CineVaultTheme.colors.textSecondary)
                }
            },
            containerColor = CineVaultTheme.colors.surface,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CineVaultTheme.colors.background)
            .verticalScroll(rememberScrollState()),
    ) {
        TopAppBar(
            title = {
                Text(
                    "Profile",
                    style = CineVaultTheme.typography.sectionTitle,
                    color = CineVaultTheme.colors.textPrimary,
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = CineVaultTheme.colors.background),
        )

        // User info
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(CineVaultTheme.colors.surface),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    uiState.userName.take(1).uppercase(),
                    style = CineVaultTheme.typography.displayLarge.copy(fontSize = 32.sp),
                    color = CineVaultTheme.colors.accentGold,
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                uiState.userName,
                style = CineVaultTheme.typography.sectionTitle,
                color = CineVaultTheme.colors.textPrimary,
            )
            Text(
                uiState.userEmail,
                style = CineVaultTheme.typography.body,
                color = CineVaultTheme.colors.textSecondary,
            )
        }

        Spacer(Modifier.height(24.dp))

        // Profiles section
        if (uiState.profiles.isNotEmpty()) {
            Text(
                "Profiles",
                style = CineVaultTheme.typography.sectionTitle,
                color = CineVaultTheme.colors.textPrimary,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(12.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(uiState.profiles) { profile ->
                    ProfileItem(
                        profile = profile,
                        isActive = profile.id == uiState.activeProfile?.id,
                        onClick = { profileViewModel.selectProfile(profile) },
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // Settings items
        @Suppress("DEPRECATION")
        Divider(color = CineVaultTheme.colors.border, modifier = Modifier.padding(horizontal = 16.dp))

        SettingsItem(icon = Icons.Filled.Person, title = "Account Settings", onClick = { })
        SettingsItem(icon = Icons.Filled.History, title = "Watch History", onClick = onNavigateToWatchHistory)
        SettingsItem(icon = Icons.Filled.Notifications, title = "Notifications", onClick = onNavigateToNotifications)
        SettingsItem(icon = Icons.Filled.Download, title = "Downloads", onClick = onNavigateToNotifications)
        SettingsItem(icon = Icons.Filled.Settings, title = "Settings", onClick = { })
        SettingsItem(icon = Icons.Filled.Security, title = "Privacy & Security", onClick = { })
        SettingsItem(icon = Icons.Filled.Info, title = "About CineVault", onClick = { })

        @Suppress("DEPRECATION")
        Divider(color = CineVaultTheme.colors.border, modifier = Modifier.padding(horizontal = 16.dp))

        SettingsItem(
            icon = Icons.AutoMirrored.Filled.Logout,
            title = "Sign Out",
            onClick = { showLogoutDialog = true },
            tint = CineVaultTheme.colors.error,
        )

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun ProfileItem(profile: ProfileDto, isActive: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(72.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    if (isActive) CineVaultTheme.colors.accentGold.copy(alpha = 0.2f)
                    else CineVaultTheme.colors.surface,
                )
                .then(
                    if (isActive) Modifier else Modifier
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (profile.avatarUrl?.isNotBlank() == true) {
                AsyncImage(
                    model = profile.avatarUrl,
                    contentDescription = profile.name,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                )
            } else {
                Text(
                    profile.name.take(1).uppercase(),
                    style = CineVaultTheme.typography.sectionTitle,
                    color = if (isActive) CineVaultTheme.colors.accentGold else CineVaultTheme.colors.textSecondary,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            profile.name,
            style = CineVaultTheme.typography.labelSmall,
            color = if (isActive) CineVaultTheme.colors.accentGold else CineVaultTheme.colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color = CineVaultTheme.colors.textPrimary,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(
            title,
            style = CineVaultTheme.typography.body,
            color = tint,
            modifier = Modifier.weight(1f),
        )
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = CineVaultTheme.colors.textSecondary,
            modifier = Modifier.size(20.dp),
        )
    }
}
