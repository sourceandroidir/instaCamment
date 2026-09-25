package com.example.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SettingsDataStore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val rateLimitDelay: Int = 12,
    val maxDailyMessages: Int = 50,
    val metaAppId: String = "",
    val metaAppSecret: String = "",
    val autoAssignAccounts: Boolean = true,
    val themeMode: String = "SYSTEM",
    val isSaved: Boolean = false
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsDataStore = SettingsDataStore(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        val flow1 = combine(
            settingsDataStore.rateLimitDelay,
            settingsDataStore.maxDailyMessages,
            settingsDataStore.metaAppId
        ) { delay, maxDaily, appId -> Triple(delay, maxDaily, appId) }

        val flow2 = combine(
            settingsDataStore.metaAppSecret,
            settingsDataStore.autoAssignAccounts,
            settingsDataStore.themeMode
        ) { appSecret, autoAssign, theme -> Triple(appSecret, autoAssign, theme) }

        viewModelScope.launch {
            combine(flow1, flow2) { (delay, maxDaily, appId), (appSecret, autoAssign, theme) ->
                SettingsUiState(
                    rateLimitDelay = delay,
                    maxDailyMessages = maxDaily,
                    metaAppId = appId,
                    metaAppSecret = appSecret,
                    autoAssignAccounts = autoAssign,
                    themeMode = theme
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun onThemeModeChanged(mode: String) {
        viewModelScope.launch {
            settingsDataStore.setThemeMode(mode)
            _uiState.update { it.copy(themeMode = mode) }
        }
    }

    fun onDelayChanged(valInt: Int) {
        _uiState.update { it.copy(rateLimitDelay = valInt, isSaved = false) }
    }

    fun onMaxDailyChanged(valInt: Int) {
        _uiState.update { it.copy(maxDailyMessages = valInt, isSaved = false) }
    }

    fun onAppIdChanged(valStr: String) {
        _uiState.update { it.copy(metaAppId = valStr, isSaved = false) }
    }

    fun onAppSecretChanged(valStr: String) {
        _uiState.update { it.copy(metaAppSecret = valStr, isSaved = false) }
    }

    fun onAutoAssignChanged(valBool: Boolean) {
        _uiState.update { it.copy(autoAssignAccounts = valBool, isSaved = false) }
    }

    fun saveSettings() {
        viewModelScope.launch {
            val s = _uiState.value
            settingsDataStore.updateSettings(
                delaySec = s.rateLimitDelay,
                maxDaily = s.maxDailyMessages,
                appId = s.metaAppId,
                appSecret = s.metaAppSecret,
                autoAssign = s.autoAssignAccounts
            )
            _uiState.update { it.copy(isSaved = true) }
        }
    }
}
