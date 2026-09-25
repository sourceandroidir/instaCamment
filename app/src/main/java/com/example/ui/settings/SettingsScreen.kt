package com.example.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("settings_screen")
    ) {
        Text(
            text = "تنظیمات برنامه و تم",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Theme Selection Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "پوسته‌ی ظاهری برنامه (تم)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = state.themeMode == "LIGHT",
                        onClick = { viewModel.onThemeModeChanged("LIGHT") },
                        label = { Text("روشن") },
                        leadingIcon = {
                            Icon(Icons.Default.LightMode, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier.weight(1f).testTag("theme_light_chip")
                    )

                    FilterChip(
                        selected = state.themeMode == "DARK",
                        onClick = { viewModel.onThemeModeChanged("DARK") },
                        label = { Text("تاریک") },
                        leadingIcon = {
                            Icon(Icons.Default.DarkMode, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier.weight(1f).testTag("theme_dark_chip")
                    )

                    FilterChip(
                        selected = state.themeMode == "SYSTEM",
                        onClick = { viewModel.onThemeModeChanged("SYSTEM") },
                        label = { Text("سیستم") },
                        leadingIcon = {
                            Icon(Icons.Default.BrightnessAuto, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier.weight(1f).testTag("theme_system_chip")
                    )
                }
            }
        }

        // Rate Limit & API Settings Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "کنترل Rate Limit و وقفه ارسال", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(12.dp))

                Text(text = "وقفه ارسال پیام (ثانیه): ${state.rateLimitDelay}")
                Slider(
                    value = state.rateLimitDelay.toFloat(),
                    onValueChange = { viewModel.onDelayChanged(it.toInt()) },
                    valueRange = 5f..60f,
                    steps = 11,
                    modifier = Modifier.testTag("delay_slider")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "حداکثر ارسال روزانه per حساب: ${state.maxDailyMessages}")
                Slider(
                    value = state.maxDailyMessages.toFloat(),
                    onValueChange = { viewModel.onMaxDailyChanged(it.toInt()) },
                    valueRange = 10f..200f,
                    steps = 18,
                    modifier = Modifier.testTag("max_daily_slider")
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "تنظیمات توزیع پیام", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "توزیع خودکار پیام بین چند حساب", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = state.autoAssignAccounts,
                        onCheckedChange = { viewModel.onAutoAssignChanged(it) },
                        modifier = Modifier.testTag("auto_assign_switch")
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { viewModel.saveSettings() },
                    modifier = Modifier.fillMaxWidth().testTag("save_settings_btn")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ذخیره تغییرات")
                }

                if (state.isSaved) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "تنظیمات با موفقیت ذخیره شد.",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
