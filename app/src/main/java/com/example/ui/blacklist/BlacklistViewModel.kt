package com.example.ui.blacklist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.BlacklistUser
import com.example.data.repository.InstagramRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BlacklistViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = InstagramRepository(application)

    val blacklist: StateFlow<List<BlacklistUser>> = repository.allBlacklist
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _inputUsername = MutableStateFlow("")
    val inputUsername: StateFlow<String> = _inputUsername.asStateFlow()

    fun onUsernameChanged(valStr: String) {
        _inputUsername.value = valStr
    }

    fun addUsername() {
        val u = _inputUsername.value.trim()
        if (u.isNotEmpty()) {
            viewModelScope.launch {
                repository.addToBlacklist(u)
                _inputUsername.value = ""
            }
        }
    }

    fun removeUsername(user: BlacklistUser) {
        viewModelScope.launch {
            repository.removeFromBlacklist(user.username)
        }
    }
}
