package com.example.ui.users

import android.app.Application
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.InstagramUser
import com.example.data.repository.InstagramRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class UsersViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = InstagramRepository(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val users: StateFlow<List<InstagramUser>> = combine(
        repository.allUsers,
        _searchQuery
    ) { all, query ->
        if (query.trim().isEmpty()) {
            all
        } else {
            val q = query.trim().lowercase()
            all.filter { it.username.contains(q) || it.displayName.lowercase().contains(q) }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun addToBlacklist(user: InstagramUser) {
        viewModelScope.launch {
            repository.addToBlacklist(user.username, "اضافه شده از لیست کاربران")
        }
    }

    fun exportUsers(format: String, onFileReady: (Intent) -> Unit) {
        viewModelScope.launch {
            val currentUsers = users.value
            val file = repository.exportUsers(currentUsers, format)

            val uri = FileProvider.getUriForFile(
                getApplication(),
                "${getApplication<Application>().packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = when (format.lowercase()) {
                    "json" -> "application/json"
                    "txt" -> "text/plain"
                    else -> "text/csv"
                }
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "اشتراک‌گذاری خروجی کاربران")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            onFileReady(chooser)
        }
    }
}
