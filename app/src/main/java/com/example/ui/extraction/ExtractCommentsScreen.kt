package com.example.ui.extraction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.InstagramBrowserLoginDialog

@Composable
fun ExtractCommentsScreen(viewModel: ExtractCommentsViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showBrowserLoginDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("extract_comments_screen")
    ) {
        Text(
            text = "استخراج و پارس کامنت‌ها (مبتنی بر HTML و کوکی)",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 14.dp)
        )

        // Session & Cookie Status Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (state.hasActiveSession) Color(0xFF10B981).copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = if (state.hasActiveSession) Icons.Default.CheckCircle else Icons.Default.VpnKey,
                        contentDescription = null,
                        tint = if (state.hasActiveSession) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (state.hasActiveSession) "نشست مرورگر و csrftoken فعال است" else "نشست مرورگر تنظیم نشده است",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (state.hasActiveSession) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (state.hasActiveSession) "حساب فعال: @${state.sessionUsername} (کوکی‌ها ارسال می‌شوند)" else "برای دسترسی کامل به کامنت‌های پست، لاگین کنید",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                TextButton(
                    onClick = { showBrowserLoginDialog = true },
                    modifier = Modifier.testTag("extract_login_browser_btn")
                ) {
                    Text(if (state.hasActiveSession) "تغییر نشست" else "لاگین مرورگر")
                }
            }
        }

        // URL Input Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = state.postUrl,
                    onValueChange = { viewModel.onUrlChanged(it) },
                    label = { Text("لینک پست اینستاگرام") },
                    placeholder = { Text("https://www.instagram.com/p/XXXXXXXX/") },
                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                    singleLine = true,
                    enabled = !state.isExtracting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("post_url_input")
                )

                if (state.errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { viewModel.startExtraction() },
                        enabled = !state.isExtracting,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("start_extract_button"),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("شروع استخراج", fontSize = 12.sp, maxLines = 1, softWrap = false)
                    }

                    OutlinedButton(
                        onClick = { viewModel.stopExtraction() },
                        enabled = state.isExtracting,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stop_extract_button"),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("توقف", fontSize = 12.sp, maxLines = 1, softWrap = false)
                    }
                }
            }
        }

        // Live Progress & Extraction Stats Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "وضعیت پارس و استخراج:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (state.isExtracting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = state.statusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Stats summary lines
                StatRow(
                    label = "کل کامنت‌های دریافت‌شده از HTML:",
                    value = "${state.commentsFetched}",
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                StatRow(
                    label = "آیدی‌های یکتا (ذخیره در دیتابیس):",
                    value = "${state.uniqueCount}",
                    color = Color(0xFF10B981)
                )
                Spacer(modifier = Modifier.height(10.dp))
                StatRow(
                    label = "آیدی‌های تکراری (تفکیک و حذف):",
                    value = "${state.duplicateCount}",
                    color = Color(0xFFF59E0B)
                )
                Spacer(modifier = Modifier.height(10.dp))
                StatRow(
                    label = "خطاها:",
                    value = "${state.errorCount}",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        // Browser Login Dialog
        if (showBrowserLoginDialog) {
            InstagramBrowserLoginDialog(
                onDismiss = { showBrowserLoginDialog = false },
                onLoginSuccess = { cookies, csrf, username ->
                    viewModel.saveBrowserLogin(cookies, csrf, username)
                    showBrowserLoginDialog = false
                }
            )
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            ),
            color = color
        )
    }
}
