package com.umkt.ucopy.ui.common

import androidx.compose.runtime.mutableStateOf
import com.umkt.ucopy.model.Clip

class MultiSelectState {
    private val _isActive = mutableStateOf(false)
    private val _selectedIds = mutableStateOf<Set<String>>(emptySet())

    val isActive: Boolean get() = _isActive.value
    val selectedIds: Set<String> get() = _selectedIds.value
    val selectedCount: Int get() = selectedIds.size

    fun enterSelectionMode(initialId: String? = null) {
        _isActive.value = true
        if (initialId != null) {
            _selectedIds.value = setOf(initialId)
        }
    }

    fun exitSelectionMode() {
        _isActive.value = false
        _selectedIds.value = emptySet()
    }

    fun toggleSelection(id: String) {
        _selectedIds.value = if (id in _selectedIds.value) {
            _selectedIds.value - id
        } else {
            _selectedIds.value + id
        }
        if (_selectedIds.value.isEmpty()) {
            _isActive.value = false
        }
    }

    fun selectAll(itemIds: List<String>) {
        _selectedIds.value = itemIds.toSet()
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun isSelected(id: String): Boolean = id in _selectedIds.value

    fun getSelectedItems(allItems: List<Clip>): List<Clip> {
        return allItems.filter { it.id in _selectedIds.value }
    }
}