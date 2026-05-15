package io.github.mobdev.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NavigationViewModel : ViewModel() {

    data class State(
        val selectedChannel: String? = null,
        val openImage: String? = null
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    fun selectChannel(name: String?) {
        _state.value = _state.value.copy(selectedChannel = name, openImage = null)
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
