package com.example.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.StatCard
import com.example.ui.navigation.Screen
import com.example.ui.theme.InstaGradientEnd
import com.example.ui.theme.InstaGradientMiddle
import com.example.ui.theme.InstaGradientStart

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
        // Hero Minimal Status Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .clickable { onNavigate(Screen.Accounts) }
                .testTag("dashboard_status_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(
                                InstaGradientStart.copy(alpha = 0.08f),
                                InstaGradientMiddle.copy(alpha = 0.05f),
                                InstaGradientEnd.copy(alpha = 0.02f)
                            )
                        )
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        RoundedCornerShape(18.dp)
                    )
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (state.connectedAccounts > 0) Color(0xFF10B981) else Color(0xFFF59E0B))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "وضعیت نشست و سرویس‌ها",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = state.serviceStatus,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = if (state.connectedAccounts > 0) "${state.connectedAccounts} حساب متصل و آماده استخراج/ارسال" else "جهت اتصال نشست مرورگر کلیک کنید",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    listOf(InstaGradientStart, InstaGradientMiddle, InstaGradientEnd)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (state.connectedAccounts > 0) Icons.Default.CheckCircle else Icons.Default.Key,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Stats Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "آمار و عملکرد",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = "لمس هر کارت برای ورود",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                softWrap = false
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Stats Grid with Refined Minimal Palette
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            item {
                StatCard(
                    title = "پست‌های ثبت‌شده",
                    count = state.totalPosts,
                    icon = Icons.Default.Article,
                    accentColor = Color(0xFF8B5CF6),
                    subText = "مدیریت و حذف",
                    onClick = { onNavigate(Screen.Posts) }
                )
            }
            item {
                StatCard(
                    title = "کاربران استخراج‌شده",
                    count = state.totalUsers,
                    icon = Icons.Default.People,
                    accentColor = Color(0xFF06B6D4),
                    subText = "مشاهده لیست",
                    onClick = { onNavigate(Screen.Users) }
                )
            }
            item {
                StatCard(
                    title = "ارسال موفق دایرکت",
                    count = state.sentMessages,
                    icon = Icons.Default.CheckCircleOutline,
                    accentColor = Color(0xFF10B981),
                    subText = "گزارش ارسال",
                    onClick = { onNavigate(Screen.Queue) }
                )
            }
            item {
                StatCard(
                    title = "در صف ارسال",
                    count = state.queuedMessages,
                    icon = Icons.Default.HourglassTop,
                    accentColor = Color(0xFFF59E0B),
                    subText = "مدیریت صف",
                    onClick = { onNavigate(Screen.Queue) }
                )
            }
            item {
                StatCard(
                    title = "ناموفق / خطا",
                    count = state.failedMessages,
                    icon = Icons.Default.ErrorOutline,
                    accentColor = Color(0xFFEF4444),
                    subText = "تلاش دوباره",
                    onClick = { onNavigate(Screen.Queue) }
                )
            }
            item {
                StatCard(
                    title = "حساب‌های متصل",
                    count = state.connectedAccounts,
                    icon = Icons.Default.AccountCircle,
                    accentColor = Color(0xFF3B82F6),
                    subText = "مدیریت حساب‌ها",
                    onClick = { onNavigate(Screen.Accounts) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Minimal Action Buttons (Single line guaranteed)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onNavigate(Screen.Extract) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("dashboard_extract_btn"),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 9.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("استخراج جدید", fontSize = 11.sp, maxLines = 1, softWrap = false)
            }

            FilledTonalButton(
                onClick = { onNavigate(Screen.Posts) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("dashboard_posts_btn"),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 9.dp)
            ) {
                Icon(Icons.Default.Article, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("پست‌ها", fontSize = 11.sp, maxLines = 1, softWrap = false)
            }

            OutlinedButton(
                onClick = { onNavigate(Screen.Campaigns) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("dashboard_campaign_btn"),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 9.dp)
            ) {
                Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("کمپین", fontSize = 11.sp, maxLines = 1, softWrap = false)
            }
        }
    }
}
