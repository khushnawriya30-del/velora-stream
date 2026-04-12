package com.cinevault.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinevault.app.ui.theme.CineVaultTheme
import com.cinevault.app.ui.theme.LocalAppDimens

@Composable
fun DownloadsScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CineVaultTheme.colors.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(LocalAppDimens.current.pad32)
        ) {
            Icon(
                Icons.Default.FileDownload,
                contentDescription = null,
                tint = CineVaultTheme.colors.textMuted,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(LocalAppDimens.current.pad20))
            Text(
                "No Downloads",
                fontSize = LocalAppDimens.current.font20,
                fontWeight = FontWeight.Bold,
                color = CineVaultTheme.colors.textPrimary,
            )
            Spacer(modifier = Modifier.height(LocalAppDimens.current.pad8))
            Text(
                "Movies and shows you download will appear here.",
                fontSize = LocalAppDimens.current.font14,
                color = CineVaultTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}
