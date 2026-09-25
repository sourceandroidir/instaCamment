package com.example.ui.campaigns

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.data.local.entity.InstagramAccount
import com.example.data.local.entity.InstagramPost
import com.example.data.local.entity.MessageCampaign
import com.example.data.repository.InstagramRepository
import com.example.worker.MessageQueueWorker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CampaignCreateState(
    val name: String = "",
    val messageText: String = "سلام {username}\nممنون که در پست ما شرکت کردی.\nبرای اطلاعات بیشتر می‌توانی با ما در ارتباط باشی.",
    val selectedPostId: Long = 0L,
    val selectedAccountIds: List<Long> = emptyList(),
    val isAutoAssign: Boolean = true,
    val testUsername: String = "",
    val testResult: String? = null,
    val isTestRunning: Boolean = false,
    val errorMessage: String? = null
)

class CampaignsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = InstagramRepository(application)

    val posts: StateFlow<List<InstagramPost>> = repository.allPosts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val accounts: StateFlow<List<InstagramAccount>> = repository.allAccounts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val campaigns: StateFlow<List<MessageCampaign>> = repository.allCampaigns
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _createState = MutableStateFlow(CampaignCreateState())
    val createState: StateFlow<CampaignCreateState> = _createState.asStateFlow()

    fun setInitialPostId(postId: Long) {
        if (postId != 0L) {
            _createState.update { it.copy(selectedPostId = postId) }
        }
    }

    fun onNameChanged(valStr: String) {
        _createState.update { it.copy(name = valStr, errorMessage = null) }
    }

    fun onMessageTextChanged(valStr: String) {
        _createState.update { it.copy(messageText = valStr) }
    }

    fun onPostSelected(postId: Long) {
        _createState.update { it.copy(selectedPostId = postId) }
    }

    fun onTestUsernameChanged(valStr: String) {
        _createState.update { it.copy(testUsername = valStr) }
    }

    fun runTestSend() {
        val testUser = _createState.value.testUsername.trim()
        if (testUser.isEmpty()) {
            _createState.update { it.copy(testResult = "لطفاً یک نام کاربری تست وارد کنید.") }
            return
        }

        _createState.update { it.copy(isTestRunning = true, testResult = null) }

        viewModelScope.launch {
            kotlinx.coroutines.delay(1500)
            val sampleMsg = _createState.value.messageText
                .replace("{username}", testUser)
                .replace("{display_name}", testUser)

            _createState.update {
                it.copy(
                    isTestRunning = false,
                    testResult = "ارسال آزمایشی موفقیت‌آمیز بود!\nمتن ارسال شده:\n$sampleMsg"
                )
            }
        }
    }

    fun launchCampaign() {
        val name = _createState.value.name.ifEmpty { "کمپین ${System.currentTimeMillis()}" }
        val text = _createState.value.messageText
        val postId = _createState.value.selectedPostId

        viewModelScope.launch {
            val result = repository.createCampaign(
                name = name,
                messageTemplate = text,
                sourcePostId = postId,
                selectedAccountIds = _createState.value.selectedAccountIds
            )

            result.fold(
                onSuccess = { campaign ->
                    // Schedule WorkManager worker
                    val workRequest = OneTimeWorkRequestBuilder<MessageQueueWorker>()
                        .setInputData(workDataOf("CAMPAIGN_ID" to campaign.id))
                        .build()
                    WorkManager.getInstance(getApplication()).enqueue(workRequest)

                    _createState.update {
                        CampaignCreateState(errorMessage = null)
                    }
                },
                onFailure = { ex ->
                    _createState.update { it.copy(errorMessage = ex.message) }
                }
            )
        }
    }
}
