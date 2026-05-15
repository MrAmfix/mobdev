package io.github.mobdev.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.mobdev.data.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

class LoginViewModel(private val repository: ChatRepository) : ViewModel() {

    sealed interface State {
        data object Idle : State
        data object Loading : State
        data object Success : State
        data class Error(val messageRes: Int) : State
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    fun login(name: String, password: String, invalidCredsMessage: Int, networkErrorMessage: Int) {
        if (_state.value is State.Loading) return
        _state.value = State.Loading
        viewModelScope.launch {
            try {
                repository.login(name.trim(), password)
                _state.value = State.Success
            } catch (e: HttpException) {
                _state.value = if (e.code() == 401) {
                    State.Error(invalidCredsMessage)
                } else {
                    State.Error(networkErrorMessage)
                }
            } catch (e: Exception) {
                _state.value = State.Error(networkErrorMessage)
            }
        }
    }

    fun consumeError() {
        if (_state.value is State.Error) _state.value = State.Idle
    }

    class Factory(private val repository: ChatRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LoginViewModel(repository) as T
    }
}
