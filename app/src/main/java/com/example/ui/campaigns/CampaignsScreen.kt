package com.example.ui.campaigns

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
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
import com.example.ui.components.StatusChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampaignsScreen(
    viewModel: CampaignsViewModel,
    initialPostId: Long = 0L
) {
    LaunchedEffect(initialPostId) {
        viewModel.setInitialPostId(initialPostId)
    }

    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val campaigns by viewModel.campaigns.collectAsStateWithLifecycle()
    val createState by viewModel.createState.collectAsStateWithLifecycle()

    var activeTab by remember { mutableIntStateOf(0) } // 0: Create, 1: History

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("campaigns_screen")
    ) {
        TabRow(selectedTabIndex = activeTab) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text("ساخت کمپین جدید") }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text("کمپین‌های گذشته (${campaigns.size})") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (activeTab == 0) {
            // New Campaign Form
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "تنظیمات کمپین ارسال پیام",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = createState.name,
                            onValueChange = { viewModel.onNameChanged(it) },
                            label = { Text("عنوان کمپین") },
                            placeholder = { Text("مثلاً: تبلیغات تابستانی - پست ۱") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("campaign_name_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("انتخاب لیست کاربران از پست:", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(4.dp))

                        // Post selector dropdown
                        var expanded by remember { mutableStateOf(false) }
                        val selectedPost = posts.find { it.id == createState.selectedPostId }

                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = selectedPost?.let { "پست ${it.instagramMediaId} (${it.uniqueUsers} کاربر)" } ?: "تمام پست‌ها / همه کاربران",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                                    .testTag("post_selector_dropdown")
                            )

                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("تمام پست‌ها / همه کاربران") },
                                    onClick = {
                                        viewModel.onPostSelected(0L)
                                        expanded = false
                                    }
                                )
                                posts.forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text("پست ${p.instagramMediaId} (${p.uniqueUsers} کاربر)") },
                                        onClick = {
                                            viewModel.onPostSelected(p.id)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = createState.messageText,
                            onValueChange = { viewModel.onMessageTextChanged(it) },
                            label = { Text("متن پیام (پشتیبانی از {username} و {display_name})") },
                            minLines = 4,
                            modifier = Modifier.fillMaxWidth().testTag("message_template_input")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Live Preview Card
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "پیش‌نمایش پیام:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val previewMsg = createState.messageText
                                    .replace("{username}", "ali123")
                                    .replace("{display_name}", "علی رضایی")
                                Text(text = previewMsg, style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Test Send Section (Real Direct Message)
                        Text(
                            text = "ارسال آزمایشی واقعی به دایرکت:",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = createState.testUsername,
                                onValueChange = { viewModel.onTestUsernameChanged(it) },
                                label = { Text("نام کاربری (@username)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("test_username_input")
                            )

                            Button(
                                onClick = { viewModel.runTestSend() },
                                enabled = !createState.isTestRunning,
                                modifier = Modifier.testTag("run_test_send_btn"),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                if (createState.isTestRunning) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                                } else {
                                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("ارسال تست", fontSize = 12.sp, maxLines = 1, softWrap = false)
                                }
                            }
                        }

                        if (createState.testResult != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val isError = createState.testResult!!.startsWith("خطا")
                            Surface(
                                color = if (isError) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f) else Color(0xFF10B981).copy(alpha = 0.15f),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = createState.testResult!!,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isError) MaterialTheme.colorScheme.error else Color(0xFF10B981),
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = { viewModel.launchCampaign() },
                            modifier = Modifier.fillMaxWidth().testTag("launch_campaign_button"),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("شروع کمپین", fontSize = 13.sp, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
        } else {
            // History list
            if (campaigns.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("هیچ کمپینی تا کنون ثبت نشده است.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(campaigns, key = { it.id }) { campaign ->
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("campaign_item_${campaign.id}"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = campaign.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    StatusChip(status = campaign.status)
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("کل: ${campaign.totalRecipients}")
                                    Text("موفق: ${campaign.sentCount}", color = MaterialTheme.colorScheme.primary)
                                    Text("خطا: ${campaign.failedCount}", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
