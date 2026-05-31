package com.example.myandroidapp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myandroidapp.data.local.LoginStateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val shouldNavigateToLogin: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val loginStateManager: LoginStateManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun logout() {
        viewModelScope.launch {
            loginStateManager.clearLoginState()
            _uiState.value = HomeUiState(shouldNavigateToLogin = true)
        }
    }
}
