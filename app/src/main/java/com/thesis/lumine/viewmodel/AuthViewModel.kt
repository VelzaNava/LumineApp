package com.thesis.lumine.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.thesis.lumine.data.model.AuthResponse
import com.thesis.lumine.data.repository.JewelryRepository
import com.thesis.lumine.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = JewelryRepository()
    private val sessionManager = SessionManager(application.applicationContext)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private val _otpSent = MutableStateFlow(false)
    val otpSent: StateFlow<Boolean> = _otpSent

    init {
        checkExistingSession()
    }

    private fun checkExistingSession() {
        if (sessionManager.isLoggedIn()) {
            val email = sessionManager.getUserEmail() ?: ""
            val userId = sessionManager.getUserId() ?: ""
            val accessToken = sessionManager.getAccessToken() ?: ""
            val refreshToken = sessionManager.getRefreshToken() ?: ""

            if (email.isNotEmpty() && accessToken.isNotEmpty()) {
                _authState.value = AuthState.Success(
                    AuthResponse(
                        accessToken = accessToken,
                        refreshToken = refreshToken,
                        email = email,
                        userId = userId
                    )
                )
            }
        }
    }

    fun sendOtp(email: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                val response = repository.sendOtp(email)

                if (response.isSuccessful) {
                    _otpSent.value = true
                    _authState.value = AuthState.OtpSent
                } else if (response.code() == 429) {
                    _authState.value = AuthState.Error("Too many requests. Please wait 60 seconds.")
                } else {
                    _authState.value = AuthState.Error("Failed to send OTP")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Network error")
            }
        }
    }

    fun verifyOtp(email: String, token: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                val response = repository.verifyOtp(email, token)

                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!

                    sessionManager.saveSession(
                        accessToken = authResponse.accessToken,
                        refreshToken = authResponse.refreshToken,
                        email = authResponse.email,
                        userId = authResponse.userId
                    )

                    _authState.value = AuthState.Success(authResponse)
                } else {
                    _authState.value = AuthState.Error("Invalid OTP code")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Network error")
            }
        }
    }

    fun logout() {
        sessionManager.clearSession()
        _authState.value = AuthState.Idle
        _otpSent.value = false
    }

    fun resetOtpSent() {
        _otpSent.value = false
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object OtpSent : AuthState()
    data class Success(val authResponse: AuthResponse) : AuthState()
    data class Error(val message: String) : AuthState()
}