package io.github.neronguyen.astrocommander

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.neronguyen.astrocommander.core.data.repository.PlaceholderRepository
import io.github.neronguyen.astrocommander.core.model.Placeholder
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    private val placeholderRepository: PlaceholderRepository,
) : ViewModel() {

    private val _error = MutableStateFlow("")
    val error = _error.asStateFlow()

    val list: StateFlow<List<Placeholder>> = placeholderRepository
        .observePlaceholderList()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _error.update { "Loading..." }
            placeholderRepository.syncPlaceholderList()
                .onLeft { error ->
                    _error.update { "Error: $error" }
                }.onRight {
                    _error.update { "" }
                }
        }
    }
}
