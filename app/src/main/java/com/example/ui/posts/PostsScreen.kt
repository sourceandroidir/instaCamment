package com.example.ui.posts

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.ui.components.StatusChip
import com.example.ui.theme.InstaGradientEnd
import com.example.ui.theme.InstaGradientMiddle
import com.example.ui.theme.InstaGradientStart

@Composable
fun PostsScreen(
    viewModel: PostsViewModel,
    onNavigateToCampaign: (Long) -> Unit
) {
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("posts_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "پست‌های ثبت‌شده (${posts.size})",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                softWrap = false
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (posts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Article,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "هیچ پستی ثبت نشده است.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "از بخش «استخراج کاربران» لینک پست را وارد کنید تا اطلاعات و کامنت‌ها بارگذاری شوند.",
                        color = MaterialTheme.colorScheme.outline,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(posts, key = { it.id }) { post ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("post_item_${post.id}"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Header: Post ID, Author, and Status
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (post.isVideo) Icons.Default.Videocam else Icons.Default.Image,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = if (post.authorUsername.isNotEmpty()) "@${post.authorUsername}" else "پست کد ${post.instagramMediaId}",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "کد مدیا: ${post.instagramMediaId}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline,
                                            maxLines = 1
                                        )
                                    }
                                }
                                StatusChip(status = post.status)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Thumbnail & Video / Image representation with Fallback
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
                                            .setHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36")
                                            .setHeader("Referer", "https://www.instagram.com/")
                                            .setHeader("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "تصویر پست اینستاگرام",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                        loading = {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                            }
                                        },
                                        error = {
                                            // Fallback stylish gradient placeholder if Instagram CDN token expired
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
                                                        modifier = Modifier.size(40.dp)
                                                    )
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text(
                                                        text = "پست اینستاگرام (${post.instagramMediaId})",
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp
                                                    )
                                                }
                                            }
                                        }
                                    )
                                } else {
                                    // Elegant gradient placeholder
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
                                                modifier = Modifier.size(40.dp)
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "پست اینستاگرام (${post.instagramMediaId})",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }

                                if (post.isVideo) {
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.65f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "ویدیو",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "ویدیو / ریلز",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Caption
                            if (post.caption.isNotBlank()) {
                                var isExpanded by remember { mutableStateOf(false) }
                                Text(
                                    text = post.caption,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = if (isExpanded) 15 else 3,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .clickable { isExpanded = !isExpanded }
                                        .padding(vertical = 4.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            // Stats: Total Comments and Unique Users
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Comment,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "کامنت‌ها: ${post.totalComments}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.People,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "آیدی‌های یکتا: ${post.uniqueUsers}",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF10B981)
                                        ),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Action buttons (Minimal, Single-Line guaranteed)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onNavigateToCampaign(post.id) },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("ساخت کمپین", fontSize = 12.sp, maxLines = 1, softWrap = false)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(post.postUrl))
                                        context.startActivity(intent)
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("مشاهده", fontSize = 12.sp, maxLines = 1, softWrap = false)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
