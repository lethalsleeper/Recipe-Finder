package com.example.recipefinder.presentation.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipefinder.domain.model.RecipeSummary
import com.example.recipefinder.domain.repository.FavoritesRepository
import com.example.recipefinder.presentation.common.CollectionUiState
import com.example.recipefinder.presentation.common.toCollectionUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class FavoritesViewModel(
    favoritesRepository: FavoritesRepository,
) : ViewModel() {

    /** Straight from Room, so it works offline and updates the moment a heart is tapped. */
    val uiState: StateFlow<CollectionUiState<RecipeSummary>> =
        favoritesRepository.observeFavorites()
            .map { it.toCollectionUiState() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CollectionUiState.Loading)
}
