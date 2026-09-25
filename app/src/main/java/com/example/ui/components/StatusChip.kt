package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning

@Composable
fun StatusChip(status: String, modifier: Modifier = Modifier) {
    val (label, bgColor, textColor) = when (status.uppercase()) {
        "CONNECTED", "SENT", "COMPLETED", "SUCCESS" -> Triple("متصل / موفق", StatusSuccess.copy(alpha = 0.15f), StatusSuccess)
        "PENDING", "PROCESSING", "RUNNING" -> Triple("در حال پردازش", StatusInfo.copy(alpha = 0.15f), StatusInfo)
        "PAUSED", "DRAFT", "RETRY" -> Triple("متوقف / در انتظار", StatusWarning.copy(alpha = 0.15f), StatusWarning)
        "FAILED", "RATE_LIMITED", "DISCONNECTED", "TOKEN_EXPIRED", "PERMISSION_ERROR", "DISABLED" -> Triple("خطا / محدود شده", StatusError.copy(alpha = 0.15f), StatusError)
        "SKIPPED" -> Triple("رد شده (لیست سیاه)", Color.Gray.copy(alpha = 0.15f), Color.Gray)
        else -> Triple(status, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        ),
        color = textColor,
        modifier = modifier
            .background(bgColor, shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}
