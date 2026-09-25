package com.example.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.InstagramRepository
import kotlinx.coroutines.flow.*

data class DashboardUiState(
    val totalPosts: Int = 0,
    val totalUsers: Int = 0,
    val sentMessages: Int = 0,
    val queuedMessages: Int = 0,
    val failedMessages: Int = 0,
    val connectedAccounts: Int = 0,
    val serviceStatus: String = "آماده به کار"
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = InstagramRepository(application)

    private val flowGroup1 = combine(
        repository.totalPostCount,
        repository.totalUserCount,
        repository.sentMessageCount
    ) { posts, users, sent -> Triple(posts, users, sent) }

    private val flowGroup2 = combine(
        repository.pendingMessageCount,
        repository.failedMessageCount,
        repository.connectedAccountCount
    ) { queued, failed, accounts -> Triple(queued, failed, accounts) }

    val uiState: StateFlow<DashboardUiState> = combine(
        flowGroup1,
        flowGroup2
    ) { (posts, users, sent), (queued, failed, accounts) ->
        DashboardUiState(
            totalPosts = posts,
            totalUsers = users,
            sentMessages = sent,
            queuedMessages = queued,
            failedMessages = failed,
            connectedAccounts = accounts,
            serviceStatus = if (accounts > 0) "فعال و متصل" else "نیازمند حساب متصل"
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )
}
