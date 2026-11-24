package com.example.recipefinder.presentation.common

/** UI state for simple local-only lists (Favorites, Recently Viewed). */
sealed interface CollectionUiState<out T> {
    data object Loading : CollectionUiState<Nothing>
    data object Empty : CollectionUiState<Nothing>
    data class Items<T>(val items: List<T>) : CollectionUiState<T>
}

fun <T> List<T>.toCollectionUiState(): CollectionUiState<T> =
    if (isEmpty()) CollectionUiState.Empty else CollectionUiState.Items(this)
