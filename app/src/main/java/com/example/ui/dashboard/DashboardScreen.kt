package com.example.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.StatCard
import com.example.ui.navigation.Screen

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigate: (Screen) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("dashboard_screen")
    ) {
        // Hero Service Status Card (Clickable)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .clickable { onNavigate(Screen.Accounts) }
                .testTag("dashboard_status_card"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "وضعیت سرویس و حساب‌ها (کلیک کنید)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = state.serviceStatus,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        // Stats Grid
        Text(
            text = "خلاصه آمار و عملکرد (جهت مشاهده جزئیات کلیک کنید)",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            item {
                StatCard(
                    title = "پست‌های ثبت‌شده",
                    count = state.totalPosts,
                    icon = Icons.Default.Article,
                    accentColor = Color(0xFF8B5CF6),
                    subText = "مشاهده لیست پست‌ها",
                    onClick = { onNavigate(Screen.Posts) }
                )
            }
            item {
                StatCard(
                    title = "کاربران استخراج‌شده",
                    count = state.totalUsers,
                    icon = Icons.Default.People,
                    accentColor = Color(0xFF06B6D4),
                    subText = "مشاهده لیست کاربران",
                    onClick = { onNavigate(Screen.Users) }
                )
            }
            item {
                StatCard(
                    title = "ارسال موفق",
                    count = state.sentMessages,
                    icon = Icons.Default.Send,
                    accentColor = Color(0xFF10B981),
                    subText = "مشاهده صف پیام‌ها",
                    onClick = { onNavigate(Screen.Queue) }
                )
            }
            item {
                StatCard(
                    title = "در صف ارسال",
                    count = state.queuedMessages,
                    icon = Icons.Default.FormatListNumbered,
                    accentColor = Color(0xFFF59E0B),
                    subText = "مدیریت صف",
                    onClick = { onNavigate(Screen.Queue) }
                )
            }
            item {
                StatCard(
                    title = "ارسال ناموفق / خطا",
                    count = state.failedMessages,
                    icon = Icons.Default.Error,
                    accentColor = Color(0xFFEF4444),
                    subText = "تلاش مجدد در صف",
                    onClick = { onNavigate(Screen.Queue) }
                )
            }
            item {
                StatCard(
                    title = "حساب‌های متصل",
                    count = state.connectedAccounts,
                    icon = Icons.Default.AccountCircle,
                    accentColor = Color(0xFF3B82F6),
                    subText = "مدیریت و لاگین",
                    onClick = { onNavigate(Screen.Accounts) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { onNavigate(Screen.Extract) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("dashboard_extract_btn")
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("استخراج جدید")
            }

            OutlinedButton(
                onClick = { onNavigate(Screen.Campaigns) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("dashboard_campaign_btn")
            ) {
                Icon(Icons.Default.Campaign, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("ساخت کمپین")
            }
        }
    }
}
