package io.github.mobdev.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NavigationViewModel : ViewModel() {

    data class State(
        val selectedChannel: String? = null,
        val openImage: String? = null,
        val selectGeneration: Long = 0L
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    fun selectChannel(name: String?) {
        val current = _state.value
        val nextGen = if (name != null) current.selectGeneration + 1 else current.selectGeneration
        _state.value = current.copy(
            selectedChannel = name,
            openImage = null,
            selectGeneration = nextGen
        )
    }

    fun openImage(link: String) {
        _state.value = _state.value.copy(openImage = link)
    }

    fun closeImage() {
        _state.value = _state.value.copy(openImage = null)
    }

    fun reset() {
        _state.value = State()
    }
}
