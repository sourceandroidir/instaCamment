package com.example.ui.accounts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.InstagramAccount
import com.example.data.repository.InstagramRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AddAccountState(
    val username: String = "",
    val displayName: String = "",
    val cookies: String = "",
    val csrfToken: String = "",
    val errorMessage: String? = null
)

class AccountsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = InstagramRepository(application)

    val accounts: StateFlow<List<InstagramAccount>> = repository.allAccounts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val sessionCookies: StateFlow<String> = repository.sessionCookies
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    val sessionCsrfToken: StateFlow<String> = repository.sessionCsrfToken
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    val sessionUser: StateFlow<String> = repository.sessionUser
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    private val _addState = MutableStateFlow(AddAccountState())
    val addState: StateFlow<AddAccountState> = _addState.asStateFlow()

    fun onUsernameChanged(value: String) {
        _addState.update { it.copy(username = value, errorMessage = null) }
    }

    fun onDisplayNameChanged(value: String) {
        _addState.update { it.copy(displayName = value) }
    }

    fun onCookiesChanged(value: String) {
        _addState.update { it.copy(cookies = value, errorMessage = null) }
    }

    fun onCsrfTokenChanged(value: String) {
        _addState.update { it.copy(csrfToken = value) }
    }

    fun saveBrowserLogin(cookies: String, csrfToken: String, username: String) {
        viewModelScope.launch {
            repository.saveBrowserSession(cookies, csrfToken, username)
        }
    }

    fun clearBrowserSession() {
        viewModelScope.launch {
            repository.clearBrowserSession()
        }
    }

    fun addAccount() {
        val u = _addState.value.username.trim()
        val c = _addState.value.cookies.trim()
        val csrf = _addState.value.csrfToken.trim()

        if (u.isEmpty()) {
            _addState.update { it.copy(errorMessage = "لطفاً نام کاربری حساب را وارد کنید.") }
            return
        }

        viewModelScope.launch {
            repository.addAccount(
                username = u,
                displayName = _addState.value.displayName.ifEmpty { u },
                rawCookies = c,
                csrfToken = csrf
            )
            _addState.value = AddAccountState()
        }
    }

    fun deleteAccount(account: InstagramAccount) {
        viewModelScope.launch {
            repository.deleteAccount(account)
        }
    }
}
