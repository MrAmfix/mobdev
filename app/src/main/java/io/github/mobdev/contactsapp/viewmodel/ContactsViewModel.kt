package io.github.mobdev.contactsapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.mobdev.contactsapp.R
import io.github.mobdev.contactsapp.data.model.Contact
import io.github.mobdev.contactsapp.data.repository.fetchAllContacts
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ContactsUiState {
    data object Loading : ContactsUiState
    data class Success(val contacts: List<Contact>) : ContactsUiState
    data class Error(val message: String) : ContactsUiState
}

class ContactsViewModel(application: Application) : AndroidViewModel(application) {

    private val _allContacts = MutableStateFlow<List<Contact>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    private var hasLoaded = false

    val searchQuery: StateFlow<String> = _searchQuery

    val uiState: StateFlow<ContactsUiState> = combine(
        _allContacts, _searchQuery, _isLoading, _error
    ) { contacts, query, loading, error ->
        when {
            loading -> ContactsUiState.Loading
            error != null -> ContactsUiState.Error(error)
            else -> {
                val filtered = if (query.isBlank()) {
                    contacts
                } else {
                    contacts.filter { contact ->
                        contact.name?.contains(query, ignoreCase = true) == true ||
                            contact.phoneNumber?.contains(query) == true
                    }
                }
                ContactsUiState.Success(filtered)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ContactsUiState.Loading
    )

    fun loadContactsIfNeeded() {
        if (hasLoaded) return
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _allContacts.value = getApplication<Application>().fetchAllContacts()
                hasLoaded = true
            } catch (e: Exception) {
                _error.value = e.localizedMessage
                    ?: getApplication<Application>().getString(R.string.unknown_error)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun getContactById(id: Long): Contact? = _allContacts.value.find { it.id == id }

    fun resetAndReload() {
        hasLoaded = false
        _allContacts.value = emptyList()
        _error.value = null
        _searchQuery.value = ""
        loadContactsIfNeeded()
    }
}
