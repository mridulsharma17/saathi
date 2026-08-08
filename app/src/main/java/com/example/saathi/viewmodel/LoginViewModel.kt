package com.example.saathi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.saathi.data.OtpRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class OtpSent(val message: String) : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}

class LoginViewModel : ViewModel() {
    private val repository = OtpRepository()

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    private val _timerValue = MutableStateFlow(0)
    val timerValue: StateFlow<Int> = _timerValue.asStateFlow()

    private var timerJob: Job? = null

    fun sendOtp(email: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val response = repository.sendOtp(email)
                if (response.isSuccessful && response.body()?.success == true) {
                    _loginState.value = LoginState.OtpSent(response.body()?.message ?: "OTP sent successfully")
                    startTimer()
                } else {
                    _loginState.value = LoginState.Error(response.body()?.message ?: "Failed to send OTP")
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }

    fun verifyOtp(email: String, otp: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val response = repository.verifyOtp(email, otp)
                if (response.isSuccessful && response.body()?.success == true) {
                    _loginState.value = LoginState.Success
                } else {
                    _loginState.value = LoginState.Error(response.body()?.message ?: "Invalid OTP")
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Verification failed")
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            _timerValue.value = 60
            while (_timerValue.value > 0) {
                delay(1000)
                _timerValue.value -= 1
            }
        }
    }

    fun resetState() {
        _loginState.value = LoginState.Idle
    }
}
