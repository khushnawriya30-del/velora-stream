package com.cinevault.app.ui.screen

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinevault.app.ui.theme.CineVaultTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onMovieClick: (String) -> Unit = {},
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Notifications", "Downloads")

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
                            fontSize = 14.sp
                        )
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> NotificationsTab()
            1 -> DownloadsTab()
        }
    }
}

@Composable
private fun NotificationsTab() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
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
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = CineVaultTheme.colors.textPrimary,
            )
            Text(
                "You're all caught up!\nNew notifications will appear here.",
                fontSize = 14.sp,
                color = CineVaultTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
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
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = CineVaultTheme.colors.surface,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = CineVaultTheme.colors.textPrimary,
                    )
                    Text(
                        "No storage used",
                        fontSize = 12.sp,
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
                verticalArrangement = Arrangement.spacedBy(16.dp),
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
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = CineVaultTheme.colors.textPrimary,
                )
                Text(
                    "Download movies to watch them offline,\neven without an internet connection.\n\nTap the download icon on any movie\nto save it for offline viewing.",
                    fontSize = 14.sp,
                    color = CineVaultTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }
        }
    }
}
