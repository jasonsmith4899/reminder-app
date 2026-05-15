package com.mason.reminder.ui.screen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mason.reminder.data.model.Category
import com.mason.reminder.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryEditUiState(
    val id: Long = 0,
    val name: String = "",
    val iconName: String = "star",
    val colorHex: String = "#2E8B57",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CategoryEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val categoryId: Long = savedStateHandle.get<String>("categoryId")?.toLongOrNull() ?: 0L

    private val _uiState = MutableStateFlow(CategoryEditUiState(isLoading = categoryId != 0L))
    val uiState: StateFlow<CategoryEditUiState> = _uiState.asStateFlow()

    init {
        if (categoryId != 0L) loadCategory()
    }

    private fun loadCategory() {
        viewModelScope.launch {
            val category = categoryRepository.getByIdOnce(categoryId)
            if (category != null) {
                _uiState.update { state ->
                    state.copy(
                        id = category.id,
                        name = category.name,
                        iconName = category.iconName,
                        colorHex = category.colorHex,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, error = "分类不存在") }
            }
        }
    }

    fun updateName(name: String) = _uiState.update { it.copy(name = name) }
    fun updateIconName(iconName: String) = _uiState.update { it.copy(iconName = iconName) }
    fun updateColorHex(colorHex: String) = _uiState.update { it.copy(colorHex = colorHex) }

    fun save() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(error = "名称不能为空") }
            return
        }

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val existing = if (state.id != 0L) categoryRepository.getByIdOnce(state.id) else null

            val category = Category(
                id = state.id,
                name = state.name.trim(),
                iconName = state.iconName,
                colorHex = state.colorHex,
                sortOrder = existing?.sortOrder ?: 0,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )

            if (state.id == 0L) {
                categoryRepository.insert(category)
            } else {
                categoryRepository.update(category)
            }
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}