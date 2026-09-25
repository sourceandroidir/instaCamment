package com.example.ui.extraction

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.InstagramRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ExtractCommentsUiState(
    val postUrl: String = "",
    val isExtracting: Boolean = false,
    val commentsFetched: Int = 0,
    val uniqueCount: Int = 0,
    val duplicateCount: Int = 0,
    val errorCount: Int = 0,
    val hasActiveSession: Boolean = false,
    val sessionUsername: String = "",
    val statusMessage: String = "برای شروع، لینک پست اینستاگرام را وارد کنید.",
    val errorMessage: String? = null,
    val extractedPost: com.example.data.local.entity.InstagramPost? = null
)

class ExtractCommentsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = InstagramRepository(application)

    private val _uiState = MutableStateFlow(ExtractCommentsUiState())
    val uiState: StateFlow<ExtractCommentsUiState> = _uiState.asStateFlow()

    private var extractionJob: Job? = null

    init {
        viewModelScope.launch {
            combine(
                repository.sessionCookies,
                repository.sessionUser
            ) { cookies, user ->
                Pair(cookies.isNotBlank(), user)
            }.collect { (hasSession, user) ->
                _uiState.update {
                    it.copy(
                        hasActiveSession = hasSession,
                        sessionUsername = user
                    )
                }
            }
        }
    }

    fun onUrlChanged(newUrl: String) {
        _uiState.update { it.copy(postUrl = newUrl, errorMessage = null) }
    }

    fun saveBrowserLogin(cookies: String, csrfToken: String, username: String) {
        viewModelScope.launch {
            repository.saveBrowserSession(cookies, csrfToken, username)
        }
    }

    fun startExtraction() {
        val url = _uiState.value.postUrl.trim()
        if (url.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "لطفاً لینک پست اینستاگرام را وارد کنید.") }
            return
        }

        _uiState.update {
            it.copy(
                isExtracting = true,
                statusMessage = "در حال دریافت HTML، پارس کامنت‌ها و تفکیک آیدی‌های یکتا...",
                errorMessage = null,
                commentsFetched = 0,
                uniqueCount = 0,
                duplicateCount = 0,
                errorCount = 0
            )
        }

        extractionJob = viewModelScope.launch {
            val result = repository.extractCommentsFromPost(url) { fetched, unique, duplicates, errors ->
                _uiState.update {
                    it.copy(
                        commentsFetched = fetched,
                        uniqueCount = unique,
                        duplicateCount = duplicates,
                        errorCount = errors
                    )
                }
            }

            result.fold(
                onSuccess = { post ->
                    _uiState.update {
                        it.copy(
                            isExtracting = false,
                            extractedPost = post,
                            statusMessage = "استخراج با موفقیت پایان یافت. ${post.uniqueUsers} آیدی یکتا ذخیره شد (${it.duplicateCount} تکراری تفکیک شد)."
                        )
                    }
                },
                onFailure = { ex ->
                    _uiState.update {
                        it.copy(
                            isExtracting = false,
                            statusMessage = "عملیات متوقف شد.",
                            errorMessage = ex.message ?: "خطا در استخراج کامنت‌ها از HTML"
                        )
                    }
                }
            )
        }
    }

    fun stopExtraction() {
        extractionJob?.cancel()
        _uiState.update {
            it.copy(
                isExtracting = false,
                statusMessage = "استخراج توسط کاربر متوقف شد."
            )
        }
    }
}
