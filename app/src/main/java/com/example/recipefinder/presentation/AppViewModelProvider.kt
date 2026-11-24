package com.example.recipefinder.presentation

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.recipefinder.RecipeFinderApplication
import com.example.recipefinder.di.AppContainer
import com.example.recipefinder.presentation.detail.RecipeDetailViewModel
import com.example.recipefinder.presentation.favorites.FavoritesViewModel
import com.example.recipefinder.presentation.home.HomeViewModel
import com.example.recipefinder.presentation.recentlyviewed.RecentlyViewedViewModel

/**
 * Creates every ViewModel with its dependencies from [AppContainer] (manual constructor
 * injection). Composables call `viewModel(factory = AppViewModelProvider.Factory)`.
 */
object AppViewModelProvider {

    val Factory = viewModelFactory {
        initializer {
            HomeViewModel(
                savedStateHandle = createSavedStateHandle(),
                recipeRepository = container().recipeRepository,
                connectivityObserver = container().connectivityObserver,
            )
        }
        initializer {
            RecipeDetailViewModel(
                savedStateHandle = createSavedStateHandle(),
                recipeRepository = container().recipeRepository,
                favoritesRepository = container().favoritesRepository,
                recentlyViewedRepository = container().recentlyViewedRepository,
                connectivityObserver = container().connectivityObserver,
            )
        }
        initializer {
            FavoritesViewModel(container().favoritesRepository)
        }
        initializer {
            RecentlyViewedViewModel(container().recentlyViewedRepository)
        }
    }

    private fun CreationExtras.container(): AppContainer =
        (this[APPLICATION_KEY] as RecipeFinderApplication).container
}
