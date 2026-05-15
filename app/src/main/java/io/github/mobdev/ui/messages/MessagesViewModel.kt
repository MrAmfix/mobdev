package io.github.mobdev.ui.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.mobdev.data.ChatRepository
import io.github.mobdev.data.model.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

class MessagesViewModel(
    private val repository: ChatRepository,
    private val channel: String
) : ViewModel() {

    data class UiState(
        val messages: List<Message> = emptyList(),
        val isLoading: Boolean = false,
        val endReached: Boolean = false,
        val errorText: String? = null,
        val unauthorized: Boolean = false,
        val sending: Boolean = false
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        loadInitial()
    }

    fun loadInitial() {
        _state.update { it.copy(isLoading = true, errorText = null, endReached = false) }
        viewModelScope.launch {
            runCatching { repository.loadLatest(channel) }
                .onSuccess { fresh ->
                    _state.update {
                        it.copy(
                            messages = fresh,
                            isLoading = false,
                            endReached = fresh.size < PAGE
                        )
                    }
                }
                .onFailure { handle(it) }
        }
    }

    fun loadMore() {
        val current = _state.value
        if (current.isLoading || current.endReached || current.messages.isEmpty()) return
        val minId = current.messages.minOfOrNull { it.id?.toLongOrNull() ?: Long.MAX_VALUE }
            ?: return
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            runCatching { repository.loadOlder(channel, beforeId = minId) }
                .onSuccess { older ->
                    val merged = (current.messages + older).distinctBy { it.id }
                    _state.update {
                        it.copy(
                            messages = merged,
                            isLoading = false,
                            endReached = older.size < PAGE
                        )
                    }
                }
                .onFailure { handle(it) }
        }
    }

    fun refresh() {
        loadInitial()
    }

    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _state.value.sending) return
        _state.update { it.copy(sending = true) }
        viewModelScope.launch {
            runCatching { repository.sendText(channel, trimmed) }
                .onSuccess {
                    _state.update { it.copy(sending = false) }
                    loadInitial()
                }
                .onFailure {
                    _state.update { it.copy(sending = false) }
                    handle(it)
                }
        }
    }

    fun sendImage(bytes: ByteArray, mimeType: String, filename: String) {
        if (_state.value.sending) return
        _state.update { it.copy(sending = true) }
        viewModelScope.launch {
            runCatching { repository.sendImage(channel, bytes, mimeType, filename) }
                .onSuccess {
                    _state.update { it.copy(sending = false) }
                    loadInitial()
                }
                .onFailure {
                    _state.update { it.copy(sending = false) }
                    handle(it)
                }
        }
    }

    fun consumeError() {
        _state.update { it.copy(errorText = null) }
    }

    private fun handle(t: Throwable) {
        if (t is HttpException && t.code() == 401) {
            repository.onUnauthorized()
            _state.update { it.copy(isLoading = false, unauthorized = true) }
        } else {
            val text = if (t is HttpException) {
                val body = runCatching { t.response()?.errorBody()?.string() }.getOrNull()
                val trimmed = body?.trim()
                if (!trimmed.isNullOrEmpty()) "HTTP ${t.code()}: $trimmed"
                else "HTTP ${t.code()}"
            } else {
                t.message ?: "Network error"
            }
            _state.update { it.copy(isLoading = false, errorText = text) }
        }
    }

    companion object {
        const val PAGE = 20
    }

    class Factory(
        private val repository: ChatRepository,
        private val channel: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MessagesViewModel(repository, channel) as T
    }
}
