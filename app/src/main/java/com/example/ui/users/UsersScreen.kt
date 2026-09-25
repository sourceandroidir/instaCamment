package com.example.ui.users

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.InstaGradientEnd
import com.example.ui.theme.InstaGradientMiddle
import com.example.ui.theme.InstaGradientStart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersScreen(viewModel: UsersViewModel) {
    val users by viewModel.users.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showExportMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("users_screen")
    ) {
        // Minimal Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "کاربران استخراج‌شده",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    softWrap = false
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${users.size}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Box {
                OutlinedButton(
                    onClick = { showExportMenu = true },
                    modifier = Modifier.testTag("export_users_button"),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("خروجی", fontSize = 12.sp, maxLines = 1, softWrap = false)
                }

                DropdownMenu(
                    expanded = showExportMenu,
                    onDismissRequest = { showExportMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("خروجی اکسل (CSV)", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            showExportMenu = false
                            viewModel.exportUsers("csv") { intent -> context.startActivity(intent) }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("خروجی JSON", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.DataObject, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            showExportMenu = false
                            viewModel.exportUsers("json") { intent -> context.startActivity(intent) }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("خروجی متنی (TXT)", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            showExportMenu = false
                            viewModel.exportUsers("txt") { intent -> context.startActivity(intent) }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Minimal Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            placeholder = { Text("جستجوی نام کاربری یا نام...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "پاک کردن", modifier = Modifier.size(18.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_user_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (users.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "کاربری با این مشخصات یافت نشد." else "هنوز کاربری استخراج نشده است.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(users, key = { it.id }) { user ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("user_item_${user.username}"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // User Info with Profile Picture Avatar
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                // Profile Picture Avatar with Fallback
                                if (user.profilePicUrl.isNotBlank()) {
                                    SubcomposeAsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(user.profilePicUrl)
                                            .setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
                                            .setHeader("Referer", "https://www.instagram.com/")
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "پروفایل ${user.username}",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape),
                                        loading = {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                            }
                                        },
                                        error = {
                                            DefaultAvatar(user.username)
                                        }
                                    )
                                } else {
                                    DefaultAvatar(user.username)
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "@${user.username}",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    if (user.displayName.isNotEmpty() && user.displayName != user.username) {
                                        Text(
                                            text = user.displayName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    } else if (user.instagramUserId.isNotBlank()) {
                                        Text(
                                            text = "شناسه عددی: ${user.instagramUserId}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f),
                                            fontSize = 10.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            // Minimal Action Buttons
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Open Profile in Instagram
                                IconButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/${user.username}/"))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.size(36.dp).testTag("open_profile_${user.username}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.OpenInNew,
                                        contentDescription = "مشاهده در اینستاگرام",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Add to Blacklist
                                IconButton(
                                    onClick = { viewModel.addToBlacklist(user) },
                                    modifier = Modifier.size(36.dp).testTag("blacklist_user_${user.username}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Block,
                                        contentDescription = "افزودن به لیست سیاه",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DefaultAvatar(username: String) {
    val initial = username.take(1).uppercase()
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(
                brush = Brush.linearGradient(
                    listOf(InstaGradientStart, InstaGradientMiddle, InstaGradientEnd)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (initial.isNotBlank()) initial else "U",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}
