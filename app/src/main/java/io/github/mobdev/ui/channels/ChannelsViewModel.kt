package io.github.mobdev.ui.channels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.mobdev.data.ChatRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

class ChannelsViewModel(private val repository: ChatRepository) : ViewModel() {

    sealed interface State {
        data object Loading : State
        data class Loaded(val channels: List<String>) : State
        data object Unauthorized : State
        data class Error(val text: String) : State
    }

    sealed interface Event {
        data class ChannelCreated(val name: String) : Event
        data class Message(val text: String) : Event
    }

    private val _state = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 1)
    val events: SharedFlow<Event> = _events.asSharedFlow()

    private val _refreshComplete = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val refreshComplete: SharedFlow<Unit> = _refreshComplete.asSharedFlow()

    private val recentlyCreated = linkedSetOf<String>()
    private var creating = false

    init {
        load()
    }

    fun load() {
        if (_state.value !is State.Loaded) _state.value = State.Loading
        viewModelScope.launch {
            try {
                val channels = repository.loadChannels()
                _state.value = State.Loaded(merge(channels))
            } catch (e: HttpException) {
                if (e.code() == 401) {
                    repository.onUnauthorized()
                    _state.value = State.Unauthorized
                } else if (_state.value !is State.Loaded) {
                    _state.value = State.Error("HTTP ${e.code()}")
                } else {
                    _events.emit(Event.Message("HTTP ${e.code()}"))
                }
            } catch (e: Exception) {
                if (_state.value !is State.Loaded) {
                    _state.value = State.Error(e.message ?: "Network error")
                } else {
                    _events.emit(Event.Message(e.message ?: "Network error"))
                }
            } finally {
                _refreshComplete.tryEmit(Unit)
            }
        }
    }

    fun createChannel(name: String, firstMessage: String) {
        if (creating) return
        creating = true
        viewModelScope.launch {
            runCatching { repository.sendText(name, firstMessage) }
                .onSuccess {
                    creating = false
                    recentlyCreated.remove(name)
                    recentlyCreated.add(name)
                    val current = (_state.value as? State.Loaded)?.channels.orEmpty()
                    _state.value = State.Loaded(merge(current))
                    _events.emit(Event.ChannelCreated(name))
                    load()
                }
                .onFailure {
                    creating = false
                    _events.emit(Event.Message(it.message ?: "Network error"))
                }
        }
    }

    /**
     * Pin recently created channels to the top in creation order
     * (newest first), then append the rest of the server response.
     */
    private fun merge(serverChannels: List<String>): List<String> {
        if (recentlyCreated.isEmpty()) return serverChannels
        val pinned = recentlyCreated.toList().asReversed()
        val rest = serverChannels.filter { it !in recentlyCreated }
        return pinned + rest
    }

    class Factory(private val repository: ChatRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ChannelsViewModel(repository) as T
    }
}
