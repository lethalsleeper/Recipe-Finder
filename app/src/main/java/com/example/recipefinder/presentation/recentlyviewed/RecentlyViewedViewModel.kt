package com.example.recipefinder.presentation.recentlyviewed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipefinder.domain.model.HistoryEntry
import com.example.recipefinder.domain.repository.RecentlyViewedRepository
import com.example.recipefinder.presentation.common.CollectionUiState
import com.example.recipefinder.presentation.common.toCollectionUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class RecentlyViewedViewModel(
    recentlyViewedRepository: RecentlyViewedRepository,
) : ViewModel() {

    val uiState: StateFlow<CollectionUiState<HistoryEntry>> =
        recentlyViewedRepository.observeRecentlyViewed()
            .map { it.toCollectionUiState() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CollectionUiState.Loading)
}
