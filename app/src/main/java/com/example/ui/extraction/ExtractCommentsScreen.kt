package com.example.ui.extraction

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.local.entity.InstagramPost
import com.example.ui.components.InstagramBrowserLoginDialog
import com.example.ui.theme.InstaGradientEnd
import com.example.ui.theme.InstaGradientMiddle
import com.example.ui.theme.InstaGradientStart

@Composable
fun ExtractCommentsScreen(viewModel: ExtractCommentsViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showBrowserLoginDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("extract_comments_screen")
    ) {
        Text(
            text = "استخراج و پارس کامنت‌ها",
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
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (state.hasActiveSession) "نشست فعال: @${state.sessionUsername}" else "نشست مرورگر تنظیم نشده است",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (state.hasActiveSession) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (state.hasActiveSession) "کوکی‌ها و توکن اعتبارسنجی متصل هستند" else "برای استخراج کامل و بدون محدودیت، وارد شوید",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                TextButton(
                    onClick = { showBrowserLoginDialog = true },
                    modifier = Modifier.testTag("extract_login_browser_btn")
                ) {
                    Text(if (state.hasActiveSession) "تغییر حساب" else "ورود مرورگر", maxLines = 1, softWrap = false)
                }
            }
        }

        // URL Input Card (Left-Aligned, Full URL visible, Clear Button & Paste Button)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "لینک پست یا ریلز اینستاگرام:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    // Paste button
                    TextButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = clipboard.primaryClip
                            if (clip != null && clip.itemCount > 0) {
                                val pasteText = clip.getItemAt(0).text?.toString() ?: ""
                                if (pasteText.isNotBlank()) {
                                    viewModel.onUrlChanged(pasteText.trim())
                                    Toast.makeText(context, "لینک از کلیپ‌بورد چسبانده شد", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("چسباندن", fontSize = 12.sp, maxLines = 1, softWrap = false)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Left-To-Right LTR text field for URL with clear button and multi-line support
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    OutlinedTextField(
                        value = state.postUrl,
                        onValueChange = { viewModel.onUrlChanged(it) },
                        placeholder = {
                            Text(
                                "https://www.instagram.com/p/XXXXXXXX/ or /reel/...",
                                textAlign = TextAlign.Start
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Link, contentDescription = null)
                        },
                        trailingIcon = {
                            if (state.postUrl.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.onUrlChanged("") },
                                    modifier = Modifier.testTag("clear_url_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "خالی کردن فیلد",
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            textAlign = TextAlign.Start,
                            lineHeight = 20.sp
                        ),
                        singleLine = false,
                        maxLines = 3,
                        enabled = !state.isExtracting,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("post_url_input")
                    )
                }

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
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
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

        // Post Media & Content Preview Card (If extracted or provided)
        if (state.extractedPost != null) {
            PostPreviewSection(post = state.extractedPost!!)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Live Progress & Extraction Stats Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "وضعیت استخراج:",
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

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(14.dp))

                // Stats summary lines
                StatRow(
                    label = "کل کامنت‌های پردازش‌شده:",
                    value = "${state.commentsFetched}",
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                StatRow(
                    label = "آیدی‌های یکتا (ذخیره‌شده در دیتابیس):",
                    value = "${state.uniqueCount}",
                    color = Color(0xFF10B981)
                )
                Spacer(modifier = Modifier.height(10.dp))
                StatRow(
                    label = "آیدی‌های تکراری (تفکیک و فیلتر):",
                    value = "${state.duplicateCount}",
                    color = Color(0xFFF59E0B)
                )
                Spacer(modifier = Modifier.height(10.dp))
                StatRow(
                    label = "خطاها / بازخوانی:",
                    value = "${state.errorCount}",
                    color = if (state.errorCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
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
private fun PostPreviewSection(post: InstagramPost) {
    val context = LocalContext.current
    var isCaptionExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (post.isVideo) Icons.Default.Videocam else Icons.Default.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (post.authorUsername.isNotBlank()) "@${post.authorUsername}" else "اطلاعات پست استخراج‌شده",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                if (post.isVideo) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "ویدیو / ریلز",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Media Banner / Photo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (post.thumbnailUrl.isNotBlank()) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(post.thumbnailUrl)
                            .setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
                            .setHeader("Referer", "https://www.instagram.com/")
                            .setHeader("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
                            .crossfade(true)
                            .build(),
                        contentDescription = "تصویر پست",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        loading = {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        },
                        error = {
                            PreviewBannerFallback(post)
                        }
                    )
                } else {
                    PreviewBannerFallback(post)
                }
            }

            // Caption Text
            if (post.caption.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "متن کپشن:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Caption", post.caption))
                                Toast.makeText(context, "کپشن کپی شد", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "کپی کپشن",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = post.caption,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = if (isCaptionExpanded) 20 else 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { isCaptionExpanded = !isCaptionExpanded }
                    )
                }
            }

            if (post.isVideo && (post.videoUrl.isNotBlank() || post.postUrl.isNotBlank())) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        val openUrl = if (post.videoUrl.isNotBlank()) post.videoUrl else post.postUrl
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(openUrl))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.PlayCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("مشاهده ویدیو / ریلز در مرورگر", fontSize = 12.sp, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

@Composable
private fun PreviewBannerFallback(post: InstagramPost) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    listOf(InstaGradientStart, InstaGradientMiddle, InstaGradientEnd)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = if (post.isVideo) Icons.Default.Videocam else Icons.Default.PhotoCamera,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (post.authorUsername.isNotBlank()) "@${post.authorUsername}" else "پیش‌نمایش پست",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
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
