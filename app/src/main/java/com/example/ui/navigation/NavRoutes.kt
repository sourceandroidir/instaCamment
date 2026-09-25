package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val titleFa: String,
    val icon: ImageVector
) {
    data object Dashboard : Screen("dashboard", "داشبورد", Icons.Default.Dashboard)
    data object Extract : Screen("extract", "استخراج کاربران", Icons.Default.Download)
    data object Posts : Screen("posts", "پست‌ها", Icons.Default.Article)
    data object Users : Screen("users", "کاربران", Icons.Default.People)
    data object Accounts : Screen("accounts", "حساب‌های اینستاگرام", Icons.Default.AccountCircle)
    data object Campaigns : Screen("campaigns", "کمپین‌ها", Icons.Default.Campaign)
    data object Queue : Screen("queue", "صف ارسال", Icons.Default.FormatListNumbered)
    data object Blacklist : Screen("blacklist", "لیست سیاه", Icons.Default.Block)
    data object Settings : Screen("settings", "تنظیمات", Icons.Default.Settings)

    companion object {
        val items: List<Screen>
            get() = listOf(
                Dashboard,
                Extract,
                Posts,
                Users,
                Accounts,
                Campaigns,
                Queue,
                Blacklist,
                Settings
            )
    }
}
