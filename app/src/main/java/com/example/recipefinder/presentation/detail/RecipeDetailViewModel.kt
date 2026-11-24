package com.example.recipefinder.presentation.detail

import android.database.SQLException
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipefinder.R
import com.example.recipefinder.domain.connectivity.ConnectivityObserver
import com.example.recipefinder.domain.repository.FavoritesRepository
import com.example.recipefinder.domain.repository.RecentlyViewedRepository
import com.example.recipefinder.domain.repository.RecipeRepository
import com.example.recipefinder.presentation.navigation.Routes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class RecipeDetailViewModel(
    savedStateHandle: SavedStateHandle,
    recipeRepository: RecipeRepository,
    private val favoritesRepository: FavoritesRepository,
    recentlyViewedRepository: RecentlyViewedRepository,
    connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    /** Only the id travels through navigation; everything else is read from Room. */
    private val recipeId: String = checkNotNull(savedStateHandle[Routes.RECIPE_ID_ARG]) {
        "RecipeDetailViewModel needs a recipe id"
    }

    private val retryCount = MutableStateFlow(0)

    private val _userMessage = MutableStateFlow<Int?>(null)
    /** A one-off message (string resource) for a snackbar, or null. */
    val userMessage: StateFlow<Int?> = _userMessage.asStateFlow()

    val uiState: StateFlow<RecipeDetailUiState> = combine(
        // Re-collecting the repository Flow re-checks the cache and downloads details if missing.
        retryCount.flatMapLatest { recipeRepository.recipeDetail(recipeId) },
        favoritesRepository.isFavorite(recipeId),
    ) { detail, isFavorite ->
        detail.toUiState(isFavorite)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecipeDetailUiState.Loading)

    init {
        // The ViewModel is created once per visit (it survives rotation), so this records
        // exactly one history entry per time the user opens the recipe.
        viewModelScope.launch {
            try {
                recentlyViewedRepository.recordView(recipeId)
            } catch (e: SQLException) {
                // History is a convenience; failing to record it must not break the screen.
            }
        }

        viewModelScope.launch {
            var wasOnline = true
            connectivityObserver.isOnline.collect { online ->
                if (online && !wasOnline && needsDownload()) retry()
                wasOnline = online
            }
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            try {
                favoritesRepository.toggleFavorite(recipeId)
            } catch (e: SQLException) {
                showMessage(R.string.favorite_update_failed)
            }
        }
    }

    fun retry() {
        retryCount.update { it + 1 }
    }

    fun onUserMessageShown() {
        _userMessage.value = null
    }

    private fun showMessage(@StringRes message: Int) {
        _userMessage.value = message
    }

    private fun needsDownload(): Boolean = when (val state = uiState.value) {
        is RecipeDetailUiState.Unavailable -> true
        is RecipeDetailUiState.Success -> state.details is DetailsState.Missing
        RecipeDetailUiState.Loading -> false
    }
}
