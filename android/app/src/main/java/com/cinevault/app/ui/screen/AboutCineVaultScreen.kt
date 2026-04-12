package com.cinevault.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinevault.app.ui.theme.CineVaultTheme
import com.cinevault.app.ui.theme.LocalAppDimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutCineVaultScreen(
    onBack: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    onTermsOfService: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CineVaultTheme.colors.background),
    ) {
        TopAppBar(
            title = { Text("About VELORA", style = CineVaultTheme.typography.sectionTitle, color = CineVaultTheme.colors.textPrimary) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CineVaultTheme.colors.textPrimary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = CineVaultTheme.colors.background),
        )

        Column(modifier = Modifier.padding(horizontal = LocalAppDimens.current.pad20)) {
            Spacer(Modifier.height(LocalAppDimens.current.pad24))

            // App logo + name
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = CineVaultTheme.colors.accentGold.copy(alpha = 0.12f),
                        modifier = Modifier.size(72.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Movie, contentDescription = null, tint = CineVaultTheme.colors.accentGold, modifier = Modifier.size(36.dp))
                        }
                    }
                    Spacer(Modifier.height(LocalAppDimens.current.pad12))
                    Text("VELORA", fontSize = LocalAppDimens.current.font22, fontWeight = FontWeight.Bold, color = CineVaultTheme.colors.textPrimary)
                    Text("Version 1.0.0", fontSize = LocalAppDimens.current.font13, color = CineVaultTheme.colors.textSecondary)
                }
            }

            Spacer(Modifier.height(LocalAppDimens.current.pad32))

            Surface(shape = RoundedCornerShape(14.dp), color = CineVaultTheme.colors.surface, modifier = Modifier.fillMaxWidth()) {
                Column {
                    AboutItem(icon = Icons.Default.Description, label = "Privacy Policy", onClick = onPrivacyPolicy)
                    HorizontalDivider(color = CineVaultTheme.colors.border.copy(alpha = 0.3f), modifier = Modifier.padding(start = 54.dp))
                    AboutItem(icon = Icons.Default.Gavel, label = "Terms of Service", onClick = onTermsOfService)
                }
            }

            Spacer(Modifier.height(LocalAppDimens.current.pad16))

            Surface(shape = RoundedCornerShape(14.dp), color = CineVaultTheme.colors.surface, modifier = Modifier.fillMaxWidth()) {
                Column {
                    AboutItem(icon = Icons.Default.Email, label = "Contact Us", subtitle = "veloraapp@gmail.com")
                    HorizontalDivider(color = CineVaultTheme.colors.border.copy(alpha = 0.3f), modifier = Modifier.padding(start = 54.dp))
                    AboutItem(icon = Icons.Default.Code, label = "Developer", subtitle = "VELORA Team")
                }
            }

            Spacer(Modifier.height(LocalAppDimens.current.pad32))

            Text(
                "\u00a9 2025 VELORA. All rights reserved.",
                fontSize = LocalAppDimens.current.font12,
                color = CineVaultTheme.colors.textSecondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun AboutItem(
    icon: ImageVector,
    label: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = LocalAppDimens.current.pad16, vertical = LocalAppDimens.current.pad14),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = CineVaultTheme.colors.accentGold, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(LocalAppDimens.current.pad14))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = LocalAppDimens.current.font15, color = CineVaultTheme.colors.textPrimary)
            if (subtitle != null) {
                Text(subtitle, fontSize = LocalAppDimens.current.font12, color = CineVaultTheme.colors.textSecondary)
            }
        }
        if (onClick != null) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = CineVaultTheme.colors.textSecondary, modifier = Modifier.size(20.dp))
        }
    }
}
