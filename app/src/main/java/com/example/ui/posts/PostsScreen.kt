package com.example.ui.posts

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
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
import com.example.data.local.entity.InstagramPost
import com.example.ui.components.StatusChip
import com.example.ui.theme.InstaGradientEnd
import com.example.ui.theme.InstaGradientMiddle
import com.example.ui.theme.InstaGradientStart

@Composable
fun PostsScreen(
    viewModel: PostsViewModel,
    onNavigateToCampaign: (Long) -> Unit,
    onReExtract: (String) -> Unit
) {
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var postToDelete by remember { mutableStateOf<InstagramPost?>(null) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("posts_screen")
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "پست‌های ثبت‌شده",
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
                        text = "${posts.size}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            if (posts.isNotEmpty()) {
                IconButton(
                    onClick = { showDeleteAllDialog = true },
                    modifier = Modifier.testTag("delete_all_posts_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "حذف همه پست‌ها",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (posts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Article,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "هیچ پستی ثبت نشده است",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "از بخش «استخراج کاربران» لینک پست اینستاگرام را وارد کنید تا اطلاعات، عکس، ویدیو، کپشن و کامنت‌ها بارگذاری و ثبت شوند.",
                        color = MaterialTheme.colorScheme.outline,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(posts, key = { it.id }) { post ->
                    PostCardItem(
                        post = post,
                        onDeleteClick = { postToDelete = post },
                        onReExtract = { onReExtract(post.postUrl) },
                        onNavigateToCampaign = { onNavigateToCampaign(post.id) }
                    )
                }
            }
        }
    }

    // Single Delete Confirmation Dialog
    if (postToDelete != null) {
        AlertDialog(
            onDismissRequest = { postToDelete = null },
            icon = {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "حذف پست",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "آیا از حذف این پست (${if (postToDelete!!.authorUsername.isNotBlank()) "@" + postToDelete!!.authorUsername else postToDelete!!.instagramMediaId}) مطمئن هستید؟",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val p = postToDelete
                        if (p != null) {
                            viewModel.deletePost(p)
                            Toast.makeText(context, "پست با موفقیت حذف شد", Toast.LENGTH_SHORT).show()
                        }
                        postToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف", maxLines = 1, softWrap = false)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { postToDelete = null }) {
                    Text("انصراف", maxLines = 1, softWrap = false)
                }
            }
        )
    }

    // Delete All Confirmation Dialog
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            icon = {
                Icon(
                    Icons.Default.DeleteSweep,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "حذف تمام پست‌ها",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "آیا می‌خواهید تمامی پست‌های ثبت‌شده (${posts.size} مورد) را حذف کنید؟",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAllPosts()
                        Toast.makeText(context, "تمامی پست‌ها حذف شدند", Toast.LENGTH_SHORT).show()
                        showDeleteAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف همه", maxLines = 1, softWrap = false)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteAllDialog = false }) {
                    Text("انصراف", maxLines = 1, softWrap = false)
                }
            }
        )
    }
}

@Composable
private fun PostCardItem(
    post: InstagramPost,
    onDeleteClick: () -> Unit,
    onReExtract: () -> Unit,
    onNavigateToCampaign: () -> Unit
) {
    val context = LocalContext.current
    var isCaptionExpanded by remember { mutableStateOf(false) }

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
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Icon, Author / Code, Status & Delete button
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
                            text = "شناسه مدیا: ${post.instagramMediaId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusChip(status = post.status)
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("delete_post_${post.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "حذف پست",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Post Visual Thumbnail & Video badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
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
                            FallbackPostBanner(post = post)
                        }
                    )
                } else {
                    FallbackPostBanner(post = post)
                }

                // Video Badge & Play Click
                if (post.isVideo) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
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

            // Post Caption
            if (post.caption.isNotBlank()) {
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
                            text = "کپشن پست:",
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
                    if (post.caption.length > 100) {
                        Text(
                            text = if (isCaptionExpanded) "بستن متن" else "مشاهده متن کامل...",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable { isCaptionExpanded = !isCaptionExpanded }
                                .padding(top = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Stats row
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
                        modifier = Modifier.size(18.dp)
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
                        modifier = Modifier.size(18.dp)
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

            // Action buttons (Minimal single-line)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = onNavigateToCampaign,
                    modifier = Modifier.weight(1f).testTag("post_campaign_${post.id}"),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("کمپین", fontSize = 11.sp, maxLines = 1, softWrap = false)
                }

                FilledTonalButton(
                    onClick = onReExtract,
                    modifier = Modifier.weight(1f).testTag("post_reextract_${post.id}"),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("استخراج مجدد", fontSize = 11.sp, maxLines = 1, softWrap = false)
                }

                OutlinedButton(
                    onClick = {
                        val openUrl = if (post.isVideo && post.videoUrl.isNotBlank()) post.videoUrl else post.postUrl
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(openUrl))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.testTag("post_view_${post.id}"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = if (post.isVideo) Icons.Default.PlayCircleOutline else Icons.Default.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(if (post.isVideo) "ویدیو" else "مشاهده", fontSize = 11.sp, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

@Composable
private fun FallbackPostBanner(post: InstagramPost) {
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
                text = if (post.authorUsername.isNotBlank()) "@${post.authorUsername}" else "پست اینستاگرام (${post.instagramMediaId})",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}
