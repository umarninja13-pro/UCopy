package com.umkt.ucopy.ui.state

import com.umkt.ucopy.model.Clip

enum class SortOption {
    NEWEST, OLDEST, AZ, ZA
}

data class UiState(
    val items: List<Clip> = emptyList(),
    val isLoading: Boolean = true,
    val hasCompletedInitialLoad: Boolean = false,
    val searchQuery: String = "",
    val snackbarMessage: String? = null,
    val selectedCategory: String? = null,
    val sortOption: SortOption = SortOption.AZ
)

@kotlinx.serialization.Serializable
data class DeletedItem(
    val item: Clip,
    val deletedAt: Long = System.currentTimeMillis()
)
