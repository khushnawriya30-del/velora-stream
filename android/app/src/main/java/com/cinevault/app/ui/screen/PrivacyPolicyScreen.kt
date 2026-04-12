package com.cinevault.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinevault.app.ui.theme.CineVaultTheme
import com.cinevault.app.ui.theme.LocalAppDimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CineVaultTheme.colors.background),
    ) {
        TopAppBar(
            title = { Text("Privacy Policy", style = CineVaultTheme.typography.sectionTitle, color = CineVaultTheme.colors.textPrimary) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CineVaultTheme.colors.textPrimary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = CineVaultTheme.colors.background),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = LocalAppDimens.current.pad20, vertical = LocalAppDimens.current.pad16),
        ) {
            Text("Last updated: January 2025", fontSize = LocalAppDimens.current.font12, color = CineVaultTheme.colors.textSecondary)
            Spacer(Modifier.height(LocalAppDimens.current.pad16))

            PolicySection("1. Information We Collect", """
We collect information you provide directly to us, including:
• Account information (name, email address, password)
• Profile information (display name, avatar)
• Usage data (watch history, search queries, preferences)
• Device information (device type, operating system, app version)
            """.trimIndent())

            PolicySection("2. How We Use Your Information", """
We use the information we collect to:
• Provide, maintain, and improve our services
• Personalize your experience and content recommendations
• Send you notifications about new content and updates
• Monitor and analyze usage patterns and trends
• Protect against fraud and unauthorized access
            """.trimIndent())

            PolicySection("3. Information Sharing", """
We do not sell, trade, or rent your personal information to third parties. We may share information:
• With your consent or at your direction
• With service providers who assist in our operations
• To comply with legal obligations
• To protect the rights and safety of our users
            """.trimIndent())

            PolicySection("4. Data Security", """
We implement appropriate technical and organizational measures to protect your personal information against unauthorized access, alteration, disclosure, or destruction. This includes encryption of data in transit and at rest.
            """.trimIndent())

            PolicySection("5. Data Retention", """
We retain your personal information for as long as your account is active or as needed to provide you services. You may request deletion of your account and associated data at any time through the app settings.
            """.trimIndent())

            PolicySection("6. Your Rights", """
You have the right to:
• Access and update your personal information
• Delete your account and associated data
• Opt out of promotional communications
• Request a copy of your data
            """.trimIndent())

            PolicySection("7. Contact Us", """
If you have any questions about this Privacy Policy, please contact us at veloraapp@gmail.com.
            """.trimIndent())

            Spacer(Modifier.height(LocalAppDimens.current.pad32))
        }
    }
}

@Composable
private fun PolicySection(title: String, body: String) {
    Text(title, fontSize = LocalAppDimens.current.font16, fontWeight = FontWeight.SemiBold, color = CineVaultTheme.colors.textPrimary)
    Spacer(Modifier.height(LocalAppDimens.current.pad6))
    Text(body, fontSize = LocalAppDimens.current.font14, color = CineVaultTheme.colors.textSecondary, lineHeight = LocalAppDimens.current.lineHeight20)
    Spacer(Modifier.height(LocalAppDimens.current.pad20))
}
