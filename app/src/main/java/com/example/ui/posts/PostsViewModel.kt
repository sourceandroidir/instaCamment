package com.example.ui.posts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.InstagramPost
import com.example.data.repository.InstagramRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PostsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = InstagramRepository(application)

    val posts: StateFlow<List<InstagramPost>> = repository.allPosts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deletePost(post: InstagramPost) {
        viewModelScope.launch {
            repository.allPosts // We can call DAO delete if needed
        }
    }
}
