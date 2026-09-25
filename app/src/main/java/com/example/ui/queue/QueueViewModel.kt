package com.example.ui.queue

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.MessageQueue
import com.example.data.repository.InstagramRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class QueueSummaryUiState(
    val pendingCount: Int = 0,
    val sentCount: Int = 0,
    val failedCount: Int = 0,
    val isRunning: Boolean = false,
    val isRateLimited: Boolean = false
)

class QueueViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = InstagramRepository(application)

    val queueItems: StateFlow<List<MessageQueue>> = repository.allQueueItems
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val summaryState: StateFlow<QueueSummaryUiState> = combine(
        repository.pendingMessageCount,
        repository.sentMessageCount,
        repository.failedMessageCount
    ) { pending, sent, failed ->
        QueueSummaryUiState(
            pendingCount = pending,
            sentCount = sent,
            failedCount = failed,
            isRunning = pending > 0
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = QueueSummaryUiState()
    )

    fun pauseQueue() {
        viewModelScope.launch {
            repository.allCampaigns.firstOrNull()?.firstOrNull()?.let {
                repository.pauseCampaign(it.id)
            }
        }
    }

    fun resumeQueue() {
        viewModelScope.launch {
            repository.allCampaigns.firstOrNull()?.firstOrNull()?.let {
                repository.resumeCampaign(it.id)
            }
        }
    }

    fun retryFailed() {
        viewModelScope.launch {
            repository.allCampaigns.firstOrNull()?.firstOrNull()?.let {
                repository.retryFailedQueue(it.id)
            }
        }
    }
}
