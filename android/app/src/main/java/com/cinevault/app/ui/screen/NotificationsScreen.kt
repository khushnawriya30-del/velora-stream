package com.cinevault.app.ui.screen

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cinevault.app.data.model.NotificationDto
import com.cinevault.app.ui.theme.CineVaultTheme
import com.cinevault.app.ui.viewmodel.SettingsViewModel
import com.cinevault.app.ui.theme.LocalAppDimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onMovieClick: (String) -> Unit = {},
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Notifications", "Downloads")

    LaunchedEffect(Unit) {
        settingsViewModel.loadNotifications()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CineVaultTheme.colors.background)
    ) {
        TopAppBar(
            title = {
                Text(
                    tabs[selectedTab],
                    style = CineVaultTheme.typography.sectionTitle,
                    color = CineVaultTheme.colors.textPrimary,
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = CineVaultTheme.colors.textPrimary
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = CineVaultTheme.colors.background
            ),
        )

        // Tab Row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = CineVaultTheme.colors.background,
            contentColor = CineVaultTheme.colors.accentGold,
            indicator = { tabPositions ->
                @Suppress("DEPRECATION")
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = CineVaultTheme.colors.accentGold,
                    height = 3.dp
                )
            },
            divider = {
                @Suppress("DEPRECATION")
                Divider(color = CineVaultTheme.colors.border, thickness = 0.5.dp)
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            title,
                            color = if (selectedTab == index) CineVaultTheme.colors.accentGold
                            else CineVaultTheme.colors.textSecondary,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = LocalAppDimens.current.font14
                        )
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> NotificationsTab(settingsViewModel)
            1 -> DownloadsTab()
        }
    }
}

@Composable
private fun NotificationsTab(viewModel: SettingsViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = CineVaultTheme.colors.accentGold, strokeWidth = LocalAppDimens.current.strokeWidth)
        }
    } else if (uiState.notifications.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(LocalAppDimens.current.pad16),
                modifier = Modifier.padding(40.dp)
            ) {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = CineVaultTheme.colors.surface,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.NotificationsNone,
                            contentDescription = null,
                            tint = CineVaultTheme.colors.textMuted,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                Text(
                    "No Notifications",
                    fontSize = LocalAppDimens.current.font20,
                    fontWeight = FontWeight.Bold,
                    color = CineVaultTheme.colors.textPrimary,
                )
                Text(
                    "You're all caught up!\nNew notifications will appear here.",
                    fontSize = LocalAppDimens.current.font14,
                    color = CineVaultTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = LocalAppDimens.current.lineHeight22
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = LocalAppDimens.current.pad16, vertical = LocalAppDimens.current.pad12),
            verticalArrangement = Arrangement.spacedBy(LocalAppDimens.current.pad10),
        ) {
            items(uiState.notifications) { notification ->
                NotificationItem(notification)
            }
        }
    }
}

@Composable
private fun NotificationItem(notification: NotificationDto) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = CineVaultTheme.colors.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(LocalAppDimens.current.pad14),
            horizontalArrangement = Arrangement.spacedBy(LocalAppDimens.current.pad12),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = CineVaultTheme.colors.accentGold.copy(alpha = 0.12f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = CineVaultTheme.colors.accentGold, modifier = Modifier.size(20.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    notification.title,
                    fontSize = LocalAppDimens.current.font14,
                    fontWeight = FontWeight.SemiBold,
                    color = CineVaultTheme.colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(LocalAppDimens.current.padTiny))
                Text(
                    notification.body,
                    fontSize = LocalAppDimens.current.font13,
                    color = CineVaultTheme.colors.textSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                notification.sentAt?.let {
                    Spacer(Modifier.height(LocalAppDimens.current.pad4))
                    Text(it, fontSize = LocalAppDimens.current.font11, color = CineVaultTheme.colors.textMuted)
                }
            }
        }
    }
}

@Composable
private fun DownloadsTab() {
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        // Download stats bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LocalAppDimens.current.pad16),
            shape = RoundedCornerShape(LocalAppDimens.current.radius16),
            color = CineVaultTheme.colors.surface,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(LocalAppDimens.current.pad16),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LocalAppDimens.current.pad12)
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = CineVaultTheme.colors.accentGold.copy(alpha = 0.15f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = CineVaultTheme.colors.accentGold,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column {
                    Text(
                        "0 Movies Downloaded",
                        fontSize = LocalAppDimens.current.font16,
                        fontWeight = FontWeight.Bold,
                        color = CineVaultTheme.colors.textPrimary,
                    )
                    Text(
                        "No storage used",
                        fontSize = LocalAppDimens.current.font12,
                        color = CineVaultTheme.colors.textSecondary,
                    )
                }
            }
        }

        // Empty state
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(LocalAppDimens.current.pad16),
                modifier = Modifier.padding(40.dp)
            ) {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = CineVaultTheme.colors.surface,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = CineVaultTheme.colors.textMuted,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                Text(
                    "No Downloads Yet",
                    fontSize = LocalAppDimens.current.font20,
                    fontWeight = FontWeight.Bold,
                    color = CineVaultTheme.colors.textPrimary,
                )
                Text(
                    "Download movies to watch them offline,\neven without an internet connection.\n\nTap the download icon on any movie\nto save it for offline viewing.",
                    fontSize = LocalAppDimens.current.font14,
                    color = CineVaultTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = LocalAppDimens.current.lineHeight22
                )
            }
        }
    }
}
