package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.accounts.AccountsScreen
import com.example.ui.accounts.AccountsViewModel
import com.example.ui.blacklist.BlacklistScreen
import com.example.ui.blacklist.BlacklistViewModel
import com.example.ui.campaigns.CampaignsScreen
import com.example.ui.campaigns.CampaignsViewModel
import com.example.ui.components.AppDrawerContent
import com.example.ui.components.AppTopBar
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.dashboard.DashboardViewModel
import com.example.ui.extraction.ExtractCommentsScreen
import com.example.ui.extraction.ExtractCommentsViewModel
import com.example.ui.navigation.Screen
import com.example.ui.posts.PostsScreen
import com.example.ui.posts.PostsViewModel
import com.example.ui.queue.QueueScreen
import com.example.ui.queue.QueueViewModel
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel
import com.example.ui.theme.InstagramManagerTheme
import com.example.ui.users.UsersScreen
import com.example.ui.users.UsersViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val dashboardViewModel: DashboardViewModel by viewModels()
    private val extractCommentsViewModel: ExtractCommentsViewModel by viewModels()
    private val postsViewModel: PostsViewModel by viewModels()
    private val usersViewModel: UsersViewModel by viewModels()
    private val accountsViewModel: AccountsViewModel by viewModels()
    private val campaignsViewModel: CampaignsViewModel by viewModels()
    private val queueViewModel: QueueViewModel by viewModels()
    private val blacklistViewModel: BlacklistViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val isDark = when (settingsState.themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> systemDark
            }

            InstagramManagerTheme(darkTheme = isDark) {
                // Enforce RTL Layout Direction for Persian UI
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
                    var activeCampaignPostId by remember { mutableLongStateOf(0L) }

                    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                    val scope = rememberCoroutineScope()

                    BackHandler(enabled = currentScreen != Screen.Dashboard || drawerState.isOpen) {
                        if (drawerState.isOpen) {
                            scope.launch { drawerState.close() }
                        } else if (currentScreen != Screen.Dashboard) {
                            currentScreen = Screen.Dashboard
                        }
                    }

                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                            AppDrawerContent(
                                currentScreen = currentScreen,
                                onScreenSelected = { screen ->
                                    currentScreen = screen
                                },
                                onCloseDrawer = {
                                    scope.launch { drawerState.close() }
                                }
                            )
                        }
                    ) {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            topBar = {
                                AppTopBar(
                                    title = currentScreen.titleFa,
                                    onOpenDrawer = {
                                        scope.launch { drawerState.open() }
                                    },
                                    isDarkTheme = isDark,
                                    onToggleTheme = {
                                        val nextMode = if (isDark) "LIGHT" else "DARK"
                                        settingsViewModel.onThemeModeChanged(nextMode)
                                    }
                                )
                            }
                        ) { innerPadding ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                when (currentScreen) {
                                    Screen.Dashboard -> DashboardScreen(
                                        viewModel = dashboardViewModel,
                                        onNavigate = { screen -> currentScreen = screen }
                                    )
                                    Screen.Extract -> ExtractCommentsScreen(
                                        viewModel = extractCommentsViewModel
                                    )
                                    Screen.Posts -> PostsScreen(
                                        viewModel = postsViewModel,
                                        onNavigateToCampaign = { postId ->
                                            activeCampaignPostId = postId
                                            currentScreen = Screen.Campaigns
                                        }
                                    )
                                    Screen.Users -> UsersScreen(
                                        viewModel = usersViewModel
                                    )
                                    Screen.Accounts -> AccountsScreen(
                                        viewModel = accountsViewModel
                                    )
                                    Screen.Campaigns -> CampaignsScreen(
                                        viewModel = campaignsViewModel,
                                        initialPostId = activeCampaignPostId
                                    )
                                    Screen.Queue -> QueueScreen(
                                        viewModel = queueViewModel
                                    )
                                    Screen.Blacklist -> BlacklistScreen(
                                        viewModel = blacklistViewModel
                                    )
                                    Screen.Settings -> SettingsScreen(
                                        viewModel = settingsViewModel
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
