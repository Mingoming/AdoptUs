package com.example.adoptus.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.adoptus.data.repository.AuthRepository
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val repo = AuthRepository()

    sealed class AuthState {
        object Idle                                : AuthState()
        object Loading                             : AuthState()
        data class Success(val message: String)    : AuthState()
        data class Error(val message: String)      : AuthState()
    }

    private val _state = MutableLiveData<AuthState>(AuthState.Idle)
    val state: LiveData<AuthState> = _state

    fun login(email: String, password: String) {
        _state.value = AuthState.Loading
        viewModelScope.launch {
            repo.login(email, password).fold(
                onSuccess = { _state.value = AuthState.Success("Login berhasil") },
                onFailure = { _state.value = AuthState.Error(it.message ?: "Login gagal") }
            )
        }
    }

    fun register(email: String, password: String, fullName: String, username: String) {
        _state.value = AuthState.Loading
        viewModelScope.launch {
            repo.register(email, password, fullName, username).fold(
                onSuccess = { _state.value = AuthState.Success("Register berhasil") },
                onFailure = { _state.value = AuthState.Error(it.message ?: "Register gagal") }
            )
        }
    }

    fun loginWithGoogle(idToken: String) {
        _state.value = AuthState.Loading
        viewModelScope.launch {
            repo.loginWithGoogle(idToken).fold(
                onSuccess = { _state.value = AuthState.Success("Login berhasil") },
                onFailure = { _state.value = AuthState.Error(it.message ?: "Google login gagal") }
            )
        }
    }

    fun isLoggedIn(): Boolean = repo.getCurrentUser() != null

    fun logout() { repo.logout() }
}