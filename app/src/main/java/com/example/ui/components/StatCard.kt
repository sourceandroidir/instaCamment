package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

@Composable
fun StatCard(
    title: String,
    count: Int,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    subText: String? = null,
    onClick: (() -> Unit)? = null
) {
    val cardModifier = if (onClick != null) {
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("stat_card_$title")
    } else {
        modifier
            .fillMaxWidth()
            .testTag("stat_card_$title")
    }

    Card(
        modifier = cardModifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = accentColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(18.dp)
                )
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.08f),
                            accentColor.copy(alpha = 0.01f),
                            Color.Transparent
                        )
                    )
                )
                .padding(14.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    listOf(accentColor.copy(alpha = 0.25f), accentColor.copy(alpha = 0.10f))
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    if (onClick != null) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = accentColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val formattedCount = NumberFormat.getNumberInstance(Locale("fa", "IR")).format(count)
                Text(
                    text = formattedCount,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (subText != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subText,
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
