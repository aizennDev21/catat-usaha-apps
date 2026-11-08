package com.example.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.UserProfileEntity
import com.example.data.repository.SyncRepository
import com.example.data.repository.SyncState
import com.example.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val userProfile: UserProfileEntity? = null,
    val syncState: SyncState = SyncState.Synced,
    val pendingSyncCount: Int = 0,
    val lastSyncTime: Long = 0L
)

class ProfileViewModel(
    private val userRepository: UserRepository,
    private val syncRepository: SyncRepository
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = combine(
        userRepository.getUserProfile(),
        syncRepository.syncState,
        syncRepository.getPendingCount(),
        syncRepository.lastSyncTime
    ) { user, syncState, pendingCount, lastSync ->
        ProfileUiState(
            userProfile = user,
            syncState = syncState,
            pendingSyncCount = pendingCount,
            lastSyncTime = lastSync
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProfileUiState()
    )

    fun updateProfile(fullName: String, businessName: String, phone: String) {
        viewModelScope.launch {
            userRepository.updateProfile(fullName, businessName, phone)
        }
    }

    fun updateSettings(
        themeMode: String? = null,
        deductStockDefault: Boolean? = null,
        addStockDefault: Boolean? = null,
        lowStockNotification: Boolean? = null,
        pinLock: String? = null
    ) {
        viewModelScope.launch {
            userRepository.updateSettings(
                themeMode = themeMode,
                deductStockDefault = deductStockDefault,
                addStockDefault = addStockDefault,
                lowStockNotification = lowStockNotification,
                pinLock = pinLock
            )
        }
    }

    fun clearPinLock() {
        viewModelScope.launch {
            userRepository.clearPinLock()
        }
    }

    fun triggerSync() {
        viewModelScope.launch {
            syncRepository.performSync()
        }
    }

    fun logout() {
        viewModelScope.launch {
            userRepository.logout()
        }
    }
}
