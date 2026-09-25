package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InstagramBrowserLoginDialog(
    onDismiss: () -> Unit,
    onLoginSuccess: (cookies: String, csrfToken: String, username: String) -> Unit
) {
    val context = LocalContext.current
    var pageTitle by remember { mutableStateOf("ورود به اینستاگرام") }
    var currentUrl by remember { mutableStateOf("https://www.instagram.com/accounts/login/") }
    var isLoading by remember { mutableStateOf(true) }
    var detectedCookies by remember { mutableStateOf("") }
    var detectedCsrfToken by remember { mutableStateOf("") }
    var detectedUsername by remember { mutableStateOf("") }
    var loginCaptured by remember { mutableStateOf(false) }
    var cookieActionMessage by remember { mutableStateOf<String?>(null) }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // JavaScript to handle Meta / Instagram's "Allow the use of cookies" modal
    val solveAndDismissCookieModalScript = """
        (function() {
            try {
                document.cookie = "ig_cb=1; path=/; domain=.instagram.com; max-age=31536000";
                document.cookie = "ig_did=1; path=/; domain=.instagram.com; max-age=31536000";
            } catch(e) {}

            var clicked = false;

            // 1. Scroll dialogs to bottom
            var dialogs = document.querySelectorAll('div[role="dialog"], div[tabindex="-1"], div[class*="x1n2onr6"]');
            for (var d of dialogs) {
                try {
                    d.scrollTop = d.scrollHeight;
                    var scrollables = d.querySelectorAll('div');
                    for (var s of scrollables) {
                        if (s.scrollHeight > s.clientHeight) {
                            s.scrollTop = s.scrollHeight;
                        }
                    }
                } catch(e) {}
            }

            // 2. Find and trigger all pointer/mouse/touch events on Allow/Accept/Decline buttons
            var allButtons = Array.from(document.querySelectorAll('button, div[role="button"], a[role="button"]'));
            for (var i = 0; i < allButtons.length; i++) {
                var b = allButtons[i];
                var t = (b.innerText || b.textContent || '').trim().toLowerCase();
                if (
                    t === 'allow all cookies' ||
                    t.includes('allow all') ||
                    t.includes('allow essential') ||
                    t === 'decline optional cookies' ||
                    t.includes('accept all') ||
                    t.includes('accept') ||
                    t.includes('agree') ||
                    t.includes('پذیرش') ||
                    t.includes('تایید') ||
                    t.includes('تأیید') ||
                    t.includes('موافقم') ||
                    t.includes('ضروری')
                ) {
                    try {
                        b.scrollIntoView({behavior: 'instant', block: 'center'});
                        ['pointerdown', 'mousedown', 'pointerup', 'mouseup', 'click'].forEach(function(evt) {
                            b.dispatchEvent(new MouseEvent(evt, { bubbles: true, cancelable: true, view: window }));
                        });
                        if (typeof b.click === 'function') b.click();
                        clicked = true;
                    } catch(e) {}
                }
            }

            // 3. Search inside iframes if any
            var iframes = document.querySelectorAll('iframe');
            for (var j = 0; j < iframes.length; j++) {
                try {
                    var ifDoc = iframes[j].contentDocument || iframes[j].contentWindow.document;
                    if (ifDoc) {
                        var ifBtns = Array.from(ifDoc.querySelectorAll('button'));
                        for (var ib of ifBtns) {
                            var it = (ib.innerText || ib.textContent || '').toLowerCase();
                            if (it.includes('allow') || it.includes('accept') || it.includes('decline')) {
                                ib.click();
                                clicked = true;
                            }
                        }
                    }
                } catch(e) {}
            }

            // 4. Safely remove the modal overlay from DOM so the page underneath becomes active
            setTimeout(function() {
                var dList = document.querySelectorAll('div[role="dialog"]');
                for (var el of dList) {
                    var text = (el.innerText || el.textContent || '').toLowerCase();
                    if (text.includes('cookie') || text.includes('کوکی')) {
                        var backdrops = document.querySelectorAll('div[style*="position: fixed"], div[class*="x1bwybvy"], div[class*="_a9-z"]');
                        for (var bd of backdrops) {
                            if (bd !== el && bd.contains(el)) {
                                bd.remove();
                            }
                        }
                        if (el.parentElement && el.parentElement !== document.body) {
                            el.parentElement.remove();
                        } else {
                            el.remove();
                        }
                        document.body.style.overflow = 'auto';
                        document.documentElement.style.overflow = 'auto';
                    }
                }
            }, 300);

            return clicked ? 'button_clicked' : 'modal_cleaned';
        })();
    """.trimIndent()

    val clickContinueScript = """
        (function() {
            var allButtons = Array.from(document.querySelectorAll('button, div[role="button"]'));
            for (var i = 0; i < allButtons.length; i++) {
                var b = allButtons[i];
                var t = (b.innerText || b.textContent || '').trim().toLowerCase();
                if (t === 'continue' || t.startsWith('continue as') || t.includes('ادامه به عنوان') || t.includes('ادامه')) {
                    b.click();
                    return 'clicked: ' + t;
                }
            }
            return 'not_found';
        })();
    """.trimIndent()

    fun triggerAutoCookieAccept() {
        webViewRef?.evaluateJavascript(solveAndDismissCookieModalScript) { res ->
            cookieActionMessage = "پیام کوکی تایید و صفحه آزاد شد"
        }
    }

    fun triggerContinueLogin() {
        webViewRef?.evaluateJavascript(clickContinueScript) { res ->
            cookieActionMessage = "دکمه ادامه (Continue) فشرده شد"
        }
    }

    fun resetCookiesAndReload() {
        try {
            val cm = CookieManager.getInstance()
            cm.removeAllCookies(null)
            cm.flush()
            webViewRef?.clearCache(true)
            webViewRef?.loadUrl("https://www.instagram.com/accounts/login/")
            cookieActionMessage = "کش پاکسازی شد. مجدداً اطلاعات را وارد کنید."
        } catch (_: Exception) {}
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = null,
                                    tint = Color(0xFF4285F4),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "مرورگر کروم (ورود به اینستاگرام)",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Text(
                                        text = if (loginCaptured) "ورود موفق! در حال ذخیره..." else "در انتظار ورود به حساب...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (loginCaptured) Color(0xFF10B981) else MaterialTheme.colorScheme.outline,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_browser_dialog")) {
                                Icon(Icons.Default.Close, contentDescription = "بستن")
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { webViewRef?.reload() },
                                modifier = Modifier.testTag("refresh_browser_page")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "تازه سازی")
                            }
                            IconButton(
                                onClick = { resetCookiesAndReload() },
                                modifier = Modifier.testTag("reset_browser_cookies")
                            ) {
                                Icon(Icons.Default.CleaningServices, contentDescription = "پاکسازی نشست")
                            }
                            IconButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.testTag("open_in_chrome_app")
                            ) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = "باز کردن در کروم")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    // Chrome address bar
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "امن",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = currentUrl.take(45) + if (currentUrl.length > 45) "..." else "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            },
            bottomBar = {
                Surface(
                    tonalElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        if (cookieActionMessage != null) {
                            Text(
                                text = cookieActionMessage!!,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 6.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        if (loginCaptured) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF10B981).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "کوکی‌ها با موفقیت ذخیره شدند.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF10B981),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        } else {
                            // Row 1: Direct Cookie Solver + Continue Button (Single Line)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { triggerAutoCookieAccept() },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.weight(1f).testTag("auto_accept_cookie_btn"),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تأیید کوکی", fontSize = 12.sp, maxLines = 1, softWrap = false)
                                }

                                OutlinedButton(
                                    onClick = { triggerContinueLogin() },
                                    modifier = Modifier.weight(1f).testTag("click_continue_btn"),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("ادامه با حساب", fontSize = 12.sp, maxLines = 1, softWrap = false)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Row 2: Login Form Jump + Save Cookies (Single Line)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        webViewRef?.loadUrl("https://www.instagram.com/accounts/login/")
                                    },
                                    modifier = Modifier.weight(1f).testTag("goto_login_page_btn"),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.AccountBox, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("فرم لاگین", fontSize = 12.sp, maxLines = 1, softWrap = false)
                                }

                                Button(
                                    onClick = {
                                        val cm = CookieManager.getInstance()
                                        val cookies = cm.getCookie("https://www.instagram.com") ?: ""
                                        if (cookies.isNotBlank()) {
                                            val csrf = extractCookieValue(cookies, "csrftoken")
                                            val userId = extractCookieValue(cookies, "ds_user_id")
                                            val cleanUser = if (userId.isNotEmpty()) "user_$userId" else "instagram_user"
                                            onLoginSuccess(cookies, csrf, cleanUser)
                                        } else {
                                            cookieActionMessage = "کوکی‌های نشست هنوز ثبت نشده‌اند. ابتدا لاگین کنید."
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    modifier = Modifier.weight(1f).testTag("manual_capture_cookies_btn"),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("ذخیره نشست", fontSize = 12.sp, maxLines = 1, softWrap = false)
                                }
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )

                            isClickable = true
                            isFocusable = true
                            isFocusableInTouchMode = true
                            requestFocus()

                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                javaScriptCanOpenWindowsAutomatically = true
                                setSupportMultipleWindows(false)
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                cacheMode = WebSettings.LOAD_DEFAULT
                                allowFileAccess = true
                                allowContentAccess = true
                                userAgentString =
                                    "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                            }

                            // Remove X-Requested-With so Instagram does not flag or reject WebView login requests
                            try {
                                if (WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) {
                                    WebSettingsCompat.setRequestedWithHeaderOriginAllowList(settings, emptySet())
                                }
                            } catch (_: Exception) {}

                            val cookieManager = CookieManager.getInstance()
                            cookieManager.setAcceptCookie(true)
                            cookieManager.setAcceptThirdPartyCookies(this, true)
                            CookieManager.setAcceptFileSchemeCookies(true)

                            // Pre-inject consent cookie so Instagram cookie wall is bypassed
                            try {
                                cookieManager.setCookie("https://www.instagram.com", "ig_cb=1; Path=/; Domain=.instagram.com; Secure; SameSite=None")
                                cookieManager.setCookie("https://www.instagram.com", "ig_did=1; Path=/; Domain=.instagram.com; Secure; SameSite=None")
                                cookieManager.flush()
                            } catch (_: Exception) {}

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    isLoading = true
                                    // Remove webdriver flag
                                    view?.evaluateJavascript(
                                        "try { Object.defineProperty(navigator, 'webdriver', {get: () => undefined}); } catch(e){}",
                                        null
                                    )
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isLoading = false
                                    pageTitle = view?.title ?: "اینستاگرام"
                                    currentUrl = url ?: ""

                                    // Run auto cookie consent clicker and modal remover
                                    view?.evaluateJavascript(
                                        """
                                        setTimeout(function() {
                                            $solveAndDismissCookieModalScript
                                        }, 800);
                                        """.trimIndent(),
                                        null
                                    )

                                    val cookies = cookieManager.getCookie("https://www.instagram.com") ?: ""
                                    if (cookies.contains("sessionid") || cookies.contains("ds_user_id")) {
                                        detectedCookies = cookies
                                        val csrf = extractCookieValue(cookies, "csrftoken")
                                        val userId = extractCookieValue(cookies, "ds_user_id")
                                        detectedCsrfToken = csrf

                                        if (!loginCaptured) {
                                            loginCaptured = true

                                            view?.evaluateJavascript(
                                                "(function() { try { var el = document.querySelector('a[href*=\"/\"] span'); return el ? el.innerText : ''; } catch(e) { return ''; } })()"
                                            ) { jsUser ->
                                                val cleanJsUser = jsUser?.replace("\"", "")?.trim() ?: ""
                                                val finalUser = if (cleanJsUser.isNotBlank()) cleanJsUser else "user_$userId"
                                                detectedUsername = finalUser
                                                onLoginSuccess(cookies, csrf, finalUser)
                                            }
                                        }
                                    }
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onReceivedTitle(view: WebView?, title: String?) {
                                    super.onReceivedTitle(view, title)
                                    if (!title.isNullOrBlank()) pageTitle = title
                                }

                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    super.onProgressChanged(view, newProgress)
                                    isLoading = newProgress < 100
                                }

                                override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                                    result?.confirm()
                                    return true
                                }

                                override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                                    result?.confirm()
                                    return true
                                }

                                override fun onJsPrompt(view: WebView?, url: String?, message: String?, defaultValue: String?, result: JsPromptResult?): Boolean {
                                    result?.confirm(defaultValue ?: "")
                                    return true
                                }
                            }

                            loadUrl(currentUrl)
                            webViewRef = this
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    onRelease = { webView ->
                        try {
                            webView.stopLoading()
                            webView.clearHistory()
                            webView.destroy()
                        } catch (_: Exception) {}
                    }
                )

                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                        color = Color(0xFF4285F4)
                    )
                }
            }
        }
    }
}

private fun extractCookieValue(cookieHeader: String, key: String): String {
    return cookieHeader.split(";")
        .map { it.trim() }
        .firstOrNull { it.startsWith("$key=") }
        ?.removePrefix("$key=")
        ?: ""
}
