package io.github.mobdev.ui.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.mobdev.data.ChatRepository
import io.github.mobdev.data.model.DisplayMessage
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
        val serverMessages: List<DisplayMessage> = emptyList(),
        val pendingMessages: List<DisplayMessage> = emptyList(),
        val isLoading: Boolean = false,
        val endReached: Boolean = false,
        val errorText: String? = null,
        val unauthorized: Boolean = false,
        val isOnline: Boolean = true,
        val scrollToBottom: Boolean = false
    ) {
        val allMessages: List<DisplayMessage>
            get() = (serverMessages + pendingMessages).sortedByDescending { it.sortKey }
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    var lastConsumedSelectGen: Long = -1L

    init {
        viewModelScope.launch {
            repository.networkMonitor.isOnline.collect { online ->
                _state.update { it.copy(isOnline = online) }
                if (online) {
                    viewModelScope.launch { onNetworkRestored() }
                }
            }
        }
    }

    private suspend fun onNetworkRestored() {
        repository.flushPending(channel)
        val pending = repository.getPendingMessages(channel)
        _state.update { it.copy(pendingMessages = pending) }

        val maxId = _state.value.serverMessages.maxOfOrNull { it.sortKey } ?: 0L
        if (maxId > 0L) {
            runCatching { repository.loadNewer(channel, maxId) }
                .onSuccess { newer ->
                    if (newer.isNotEmpty()) {
                        _state.update { s ->
                            s.copy(serverMessages = (s.serverMessages + newer).distinctBy { it.id })
                        }
                    }
                }
        }
    }

    fun loadInitial() {
        _state.update { it.copy(isLoading = true, errorText = null, endReached = false) }
        viewModelScope.launch {
            val pending = repository.getPendingMessages(channel)
            runCatching { repository.loadLatest(channel) }
                .onSuccess { fresh ->
                    _state.update {
                        it.copy(
                            serverMessages = fresh,
                            pendingMessages = pending,
                            isLoading = false,
                            endReached = fresh.size < PAGE
                        )
                    }
                }
                .onFailure { t ->
                    _state.update { it.copy(pendingMessages = pending, isLoading = false) }
                    handle(t)
                }
        }
    }

    fun loadMore() {
        val current = _state.value
        if (current.isLoading || current.endReached || current.serverMessages.isEmpty()) return
        val minId = current.serverMessages.minOfOrNull { it.sortKey } ?: return
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            runCatching { repository.loadOlder(channel, beforeId = minId) }
                .onSuccess { older ->
                    _state.update { s ->
                        val merged = (s.serverMessages + older).distinctBy { it.id }
                        s.copy(serverMessages = merged, isLoading = false, endReached = older.size < PAGE)
                    }
                }
                .onFailure { handle(it) }
        }
    }

    fun refresh() = loadInitial()

    fun pollNewer() {
        val current = _state.value
        if (current.isLoading) return
        val maxId = current.serverMessages.maxOfOrNull { it.sortKey } ?: 0L
        if (maxId <= 0L) return
        viewModelScope.launch {
            runCatching { repository.loadNewer(channel, afterId = maxId) }
                .onSuccess { newer ->
                    if (newer.isEmpty()) return@onSuccess
                    _state.update { s ->
                        if (s.isLoading) s
                        else s.copy(serverMessages = (s.serverMessages + newer).distinctBy { it.id })
                    }
                }
        }
    }

    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val result = repository.queueOrSendText(channel, trimmed)
            val pending = repository.getPendingMessages(channel)
            _state.update { it.copy(pendingMessages = pending, scrollToBottom = true) }
            if (result is ChatRepository.SendResult.Sent) {
                pollNewer()
            }
        }
    }

    fun sendImage(bytes: ByteArray, mimeType: String, filename: String) {
        viewModelScope.launch {
            val result = repository.queueOrSendImage(channel, bytes, mimeType, filename)
            val pending = repository.getPendingMessages(channel)
            _state.update { it.copy(pendingMessages = pending, scrollToBottom = true) }
            if (result is ChatRepository.SendResult.Sent) {
                pollNewer()
            }
        }
    }

    fun consumeScrollToBottom() { _state.update { it.copy(scrollToBottom = false) } }

    fun consumeError() { _state.update { it.copy(errorText = null) } }

    private fun handle(t: Throwable) {
        if (t is HttpException && t.code() == 401) {
            repository.onUnauthorized()
            _state.update { it.copy(isLoading = false, unauthorized = true) }
        } else {
            val text = when (t) {
                is HttpException -> {
                    val body = runCatching { t.response()?.errorBody()?.string() }.getOrNull()
                    val trimmed = body?.trim()
                    if (!trimmed.isNullOrEmpty()) "HTTP ${t.code()}: $trimmed" else "HTTP ${t.code()}"
                }
                else -> t.message ?: "Network error"
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
