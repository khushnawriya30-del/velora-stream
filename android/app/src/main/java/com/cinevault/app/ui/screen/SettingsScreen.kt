package com.cinevault.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cinevault.app.ui.theme.CineVaultTheme
import com.cinevault.app.ui.viewmodel.AuthViewModel
import com.cinevault.app.ui.theme.LocalAppDimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToWatchHistory: () -> Unit = {},
    onNavigateToAccountSettings: () -> Unit = {},
    onNavigateToPrivacySecurity: () -> Unit = {},
    onNavigateToChangePassword: () -> Unit = {},
    onNavigateToPlaybackQuality: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToPrivacyPolicy: () -> Unit = {},
    onNavigateToTerms: () -> Unit = {},
    onNavigateToPremium: () -> Unit = {},
    onNavigateToLinkTv: () -> Unit = {},
    authViewModel: AuthViewModel = hiltViewModel(),
) {
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
                }) { Text("Sign Out", color = CineVaultTheme.colors.error) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel", color = CineVaultTheme.colors.textSecondary) }
            },
            containerColor = CineVaultTheme.colors.surface,
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CineVaultTheme.colors.background),
    ) {
        TopAppBar(
            title = {
                Text(
                    "Settings",
                    style = CineVaultTheme.typography.sectionTitle,
                    color = CineVaultTheme.colors.textPrimary,
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = CineVaultTheme.colors.textPrimary,
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = CineVaultTheme.colors.background),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = LocalAppDimens.current.pad20),
        ) {
            Spacer(Modifier.height(LocalAppDimens.current.pad8))

            SettingsGroupHeader("Account")
            Surface(shape = RoundedCornerShape(14.dp), color = CineVaultTheme.colors.surface, modifier = Modifier.fillMaxWidth()) {
                Column {
                    SettingsRowItem(icon = Icons.Filled.Person, title = "Account Settings", onClick = onNavigateToAccountSettings)
                    HorizontalDivider(color = CineVaultTheme.colors.border.copy(alpha = 0.3f), modifier = Modifier.padding(start = 52.dp))
                    SettingsRowItem(icon = Icons.Filled.Lock, title = "Privacy & Security", onClick = onNavigateToPrivacySecurity)
                    HorizontalDivider(color = CineVaultTheme.colors.border.copy(alpha = 0.3f), modifier = Modifier.padding(start = 52.dp))
                    SettingsRowItem(icon = Icons.Filled.Password, title = "Change Password", onClick = onNavigateToChangePassword)
                }
            }

            Spacer(Modifier.height(LocalAppDimens.current.pad20))

            SettingsGroupHeader("Premium")
            Surface(shape = RoundedCornerShape(14.dp), color = CineVaultTheme.colors.surface, modifier = Modifier.fillMaxWidth()) {
                Column {
                    SettingsRowItem(icon = Icons.Filled.Star, title = "Premium Subscription", onClick = onNavigateToPremium)
                }
            }

            Spacer(Modifier.height(LocalAppDimens.current.pad20))

            SettingsGroupHeader("Devices")
            Surface(shape = RoundedCornerShape(14.dp), color = CineVaultTheme.colors.surface, modifier = Modifier.fillMaxWidth()) {
                Column {
                    SettingsRowItem(icon = Icons.Filled.Tv, title = "Link TV", onClick = onNavigateToLinkTv)
                }
            }

            Spacer(Modifier.height(LocalAppDimens.current.pad20))

            SettingsGroupHeader("Preferences")
            Surface(shape = RoundedCornerShape(14.dp), color = CineVaultTheme.colors.surface, modifier = Modifier.fillMaxWidth()) {
                Column {
                    SettingsRowItem(icon = Icons.Filled.Notifications, title = "Notifications", onClick = onNavigateToNotifications)
                    HorizontalDivider(color = CineVaultTheme.colors.border.copy(alpha = 0.3f), modifier = Modifier.padding(start = 52.dp))
                    SettingsRowItem(icon = Icons.Filled.Download, title = "Downloads", onClick = {})
                    HorizontalDivider(color = CineVaultTheme.colors.border.copy(alpha = 0.3f), modifier = Modifier.padding(start = 52.dp))
                    SettingsRowItem(icon = Icons.Filled.Tune, title = "Playback Quality", onClick = onNavigateToPlaybackQuality)
                    HorizontalDivider(color = CineVaultTheme.colors.border.copy(alpha = 0.3f), modifier = Modifier.padding(start = 52.dp))
                    SettingsRowItem(icon = Icons.Filled.History, title = "Watch History", onClick = onNavigateToWatchHistory)
                }
            }

            Spacer(Modifier.height(LocalAppDimens.current.pad20))

            SettingsGroupHeader("About")
            Surface(shape = RoundedCornerShape(14.dp), color = CineVaultTheme.colors.surface, modifier = Modifier.fillMaxWidth()) {
                Column {
                    SettingsRowItem(icon = Icons.Filled.Info, title = "About VELORA", onClick = onNavigateToAbout)
                    HorizontalDivider(color = CineVaultTheme.colors.border.copy(alpha = 0.3f), modifier = Modifier.padding(start = 52.dp))
                    SettingsRowItem(icon = Icons.Filled.Shield, title = "Privacy Policy", onClick = onNavigateToPrivacyPolicy)
                    HorizontalDivider(color = CineVaultTheme.colors.border.copy(alpha = 0.3f), modifier = Modifier.padding(start = 52.dp))
                    SettingsRowItem(icon = Icons.Filled.Description, title = "Terms of Service", onClick = onNavigateToTerms)
                }
            }

            Spacer(Modifier.height(LocalAppDimens.current.pad32))

            SettingsGroupHeader("Account Actions")
            Surface(shape = RoundedCornerShape(14.dp), color = CineVaultTheme.colors.surface, modifier = Modifier.fillMaxWidth()) {
                SettingsRowItem(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    title = "Sign Out",
                    onClick = { showLogoutDialog = true },
                    tint = CineVaultTheme.colors.error,
                )
            }

            Spacer(Modifier.height(LocalAppDimens.current.pad32))

            Text(
                "VELORA v1.0.0",
                style = CineVaultTheme.typography.labelSmall.copy(fontSize = LocalAppDimens.current.font12),
                color = CineVaultTheme.colors.textSecondary,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(100.dp))
        }
    }
}

@Composable
private fun SettingsGroupHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = CineVaultTheme.typography.labelSmall,
        color = CineVaultTheme.colors.textSecondary,
        modifier = Modifier.padding(start = LocalAppDimens.current.pad4, bottom = LocalAppDimens.current.pad8),
    )
}

@Composable
private fun SettingsRowItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    tint: Color = CineVaultTheme.colors.textPrimary,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = LocalAppDimens.current.pad16, vertical = LocalAppDimens.current.pad14),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(CineVaultTheme.colors.accentGold.copy(alpha = 0.08f), RoundedCornerShape(LocalAppDimens.current.radius8)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(LocalAppDimens.current.pad14))
        Text(text = title, style = CineVaultTheme.typography.body, color = tint, modifier = Modifier.weight(1f))
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = CineVaultTheme.colors.textSecondary, modifier = Modifier.size(20.dp))
    }
}