package com.example.ui.accounts

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.InstagramBrowserLoginDialog
import com.example.ui.components.StatusChip

@Composable
fun AccountsScreen(viewModel: AccountsViewModel) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val addState by viewModel.addState.collectAsStateWithLifecycle()
    val sessionCookies by viewModel.sessionCookies.collectAsStateWithLifecycle()
    val sessionCsrfToken by viewModel.sessionCsrfToken.collectAsStateWithLifecycle()
    val sessionUser by viewModel.sessionUser.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var showBrowserLoginDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("accounts_screen")
    ) {
        // Browser Login & Cookie Status Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "نشست مرورگر و کوکی‌های اینستاگرام",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    if (sessionCookies.isNotBlank()) {
                        Text(
                            text = "کوکی‌ها فعال",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF10B981),
                            modifier = Modifier
                                .background(Color(0xFF10B981).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (sessionCookies.isNotBlank()) {
                    Text(
                        text = "حساب متصل: @$sessionUser",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "CSRF Token: ${if (sessionCsrfToken.isNotEmpty()) sessionCsrfToken.take(12) + "..." else "ثبت شده"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { showBrowserLoginDialog = true },
                            modifier = Modifier.weight(1f).testTag("relogin_browser_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ورود مجدد")
                        }
                        OutlinedButton(
                            onClick = { viewModel.clearBrowserSession() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f).testTag("clear_session_button")
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("خروج از نشست")
                        }
                    }
                } else {
                    Text(
                        text = "برای استخراج کامل کامنت‌ها بدون وب‌سرویس رسمی، وارد مرورگر اینستاگرام شوید تا کوکی‌ها و csrftoken به صورت خودکار دریافت و رمزنگاری شوند.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { showBrowserLoginDialog = true },
                        modifier = Modifier.fillMaxWidth().testTag("open_browser_login_button")
                    ) {
                        Icon(Icons.Default.Login, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ورود به اینستاگرام از طریق مرورگر داخلی")
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "حساب‌های ذخیره‌شده (${accounts.size})",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            OutlinedButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("add_account_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("افزودن دستی")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (accounts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "هیچ حسابی ثبت نشده است. روی دکمه ورود با مرورگر کلیک کنید.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(accounts, key = { it.id }) { account ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("account_item_${account.username}"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "@${account.username}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    if (account.csrfToken.isNotEmpty()) {
                                        Text(
                                            text = "دارای CSRF Token",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF10B981)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    StatusChip(status = account.status)
                                }
                            }

                            IconButton(
                                onClick = { viewModel.deleteAccount(account) },
                                modifier = Modifier.testTag("delete_account_${account.username}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "حذف حساب",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
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

        // Add Account Dialog (Manual)
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("افزودن دستی حساب یا کوکی") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = addState.username,
                            onValueChange = { viewModel.onUsernameChanged(it) },
                            label = { Text("نام کاربری (@username)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("add_account_username_input")
                        )
                        OutlinedTextField(
                            value = addState.displayName,
                            onValueChange = { viewModel.onDisplayNameChanged(it) },
                            label = { Text("نام عمومی / عنوان") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("add_account_display_name_input")
                        )
                        OutlinedTextField(
                            value = addState.csrfToken,
                            onValueChange = { viewModel.onCsrfTokenChanged(it) },
                            label = { Text("CSRF Token (اختیاری)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("add_account_csrf_input")
                        )
                        OutlinedTextField(
                            value = addState.cookies,
                            onValueChange = { viewModel.onCookiesChanged(it) },
                            label = { Text("رشته Cookie کامل (اختیاری)") },
                            placeholder = { Text("sessionid=...; csrftoken=...") },
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth().testTag("add_account_cookies_input")
                        )
                        if (addState.errorMessage != null) {
                            Text(
                                text = addState.errorMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.addAccount()
                            showAddDialog = false
                        },
                        modifier = Modifier.testTag("confirm_add_account_btn")
                    ) {
                        Text("ثبت و ذخیره")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("انصراف")
                    }
                }
            )
        }
    }
}
