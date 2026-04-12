package com.cinevault.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cinevault.app.ui.theme.CineVaultTheme
import com.cinevault.app.ui.viewmodel.SettingsViewModel
import com.cinevault.app.ui.theme.LocalAppDimens

private val QUALITY_OPTIONS = listOf("Auto", "1080p", "720p", "480p", "360p")

private fun qualitySubtitle(quality: String): String = when (quality) {
    "Auto" -> "Recommended — adapts to your connection"
    "1080p" -> "Full HD · Premium"
    "720p" -> "HD · Free"
    "480p" -> "SD · Free"
    "360p" -> "Low · Free"
    else -> ""
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackQualityScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CineVaultTheme.colors.background),
    ) {
        TopAppBar(
            title = { Text("Playback Quality", style = CineVaultTheme.typography.sectionTitle, color = CineVaultTheme.colors.textPrimary) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CineVaultTheme.colors.textPrimary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = CineVaultTheme.colors.background),
        )

        Column(modifier = Modifier.padding(horizontal = LocalAppDimens.current.pad20)) {
            Spacer(Modifier.height(LocalAppDimens.current.pad16))

            Text(
                "Select the default video playback quality. \"Auto\" adjusts quality based on your internet speed.",
                fontSize = LocalAppDimens.current.font13,
                color = CineVaultTheme.colors.textSecondary,
                lineHeight = LocalAppDimens.current.lineHeight18,
            )

            Spacer(Modifier.height(LocalAppDimens.current.pad20))

            Surface(shape = RoundedCornerShape(14.dp), color = CineVaultTheme.colors.surface, modifier = Modifier.fillMaxWidth()) {
                Column {
                    QUALITY_OPTIONS.forEachIndexed { index, quality ->
                        val isSelected = uiState.playbackQuality == quality
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setPlaybackQuality(quality) }
                                .padding(horizontal = LocalAppDimens.current.pad16, vertical = LocalAppDimens.current.pad14),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    quality,
                                    fontSize = LocalAppDimens.current.font16,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) CineVaultTheme.colors.accentGold else CineVaultTheme.colors.textPrimary,
                                )
                                Text(
                                    qualitySubtitle(quality),
                                    fontSize = LocalAppDimens.current.font12,
                                    color = if (quality == "1080p") CineVaultTheme.colors.accentGold.copy(alpha = 0.7f)
                                            else CineVaultTheme.colors.textSecondary,
                                )
                            }
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = CineVaultTheme.colors.accentGold, modifier = Modifier.size(22.dp))
                            }
                        }
                        if (index < QUALITY_OPTIONS.lastIndex) {
                            HorizontalDivider(color = CineVaultTheme.colors.border.copy(alpha = 0.3f), modifier = Modifier.padding(start = LocalAppDimens.current.pad16))
                        }
                    }
                }
            }
        }
    }
}
