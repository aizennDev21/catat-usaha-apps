package com.example.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.SyncRepository
import com.example.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoginMode: Boolean = true,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class AuthViewModel(
    private val userRepository: UserRepository,
    private val syncRepository: SyncRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun switchMode(isLogin: Boolean) {
        _uiState.update { it.copy(isLoginMode = isLogin, errorMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun login(identifier: String, password: String) {
        if (identifier.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Email wajib diisi") }
            return
        }
        if (password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Password wajib diisi") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = userRepository.login(identifier, password)
            if (result.isSuccess) {
                syncRepository.performSync()
                _uiState.update { it.copy(isLoading = false, successMessage = "Berhasil masuk!") }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Gagal masuk. Periksa data Anda."
                    )
                }
            }
        }
    }

    fun register(
        fullName: String,
        businessName: String,
        phone: String,
        email: String,
        password: String,
        confirmPassword: String,
        businessType: String
    ) {
        if (businessName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Nama Toko / Usaha wajib diisi") }
            return
        }
        if (fullName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Nama Pemilik wajib diisi") }
            return
        }
        if (email.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Email wajib diisi") }
            return
        }
        if (password.length < 6) {
            _uiState.update { it.copy(errorMessage = "Password minimal 6 karakter") }
            return
        }
        if (password != confirmPassword) {
            _uiState.update { it.copy(errorMessage = "Konfirmasi password tidak cocok") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = userRepository.register(
                fullName = fullName,
                businessName = businessName,
                phone = phone,
                email = email,
                password = password,
                businessType = businessType
            )
            if (result.isSuccess) {
                syncRepository.performSync()
                _uiState.update { it.copy(isLoading = false, successMessage = "Pendaftaran berhasil!") }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Gagal mendaftar. Silakan coba lagi."
                    )
                }
            }
        }
    }
}