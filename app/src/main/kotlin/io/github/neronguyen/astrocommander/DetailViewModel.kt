package io.github.neronguyen.astrocommander

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.neronguyen.astrocommander.core.data.repository.PlaceholderRepository
import io.github.neronguyen.astrocommander.core.model.Placeholder
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val placeholderRepository: PlaceholderRepository,
) : ViewModel() {

    private val _item = MutableStateFlow<Placeholder?>(null)
    val item = _item.asStateFlow()

    fun setItem(item: Placeholder) {
        viewModelScope.launch {
            val localItem = placeholderRepository.getPlaceholder(item.id)
            if (localItem != null) {
                _item.update { item.copy(title = "Details-${item.title}") }
            } else {
                _item.update { item }
            }
        }
    }
}
