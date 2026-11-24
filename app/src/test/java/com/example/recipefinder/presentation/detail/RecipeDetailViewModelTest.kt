package com.example.recipefinder.presentation.detail

import androidx.lifecycle.SavedStateHandle
import com.example.recipefinder.domain.model.DataError
import com.example.recipefinder.domain.model.Ingredient
import com.example.recipefinder.domain.model.Recipe
import com.example.recipefinder.domain.model.RecipeDetail
import com.example.recipefinder.domain.model.SyncStatus
import com.example.recipefinder.presentation.fakes.FakeConnectivityObserver
import com.example.recipefinder.presentation.fakes.FakeFavoritesRepository
import com.example.recipefinder.presentation.fakes.FakeRecentlyViewedRepository
import com.example.recipefinder.presentation.fakes.FakeRecipeRepository
import com.example.recipefinder.presentation.navigation.Routes
import com.example.recipefinder.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecipeDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val recipes = FakeRecipeRepository()
    private val favorites = FakeFavoritesRepository()
    private val history = FakeRecentlyViewedRepository()
    private val connectivity = FakeConnectivityObserver()

    private fun createViewModel(id: String = "1") = RecipeDetailViewModel(
        savedStateHandle = SavedStateHandle(mapOf(Routes.RECIPE_ID_ARG to id)),
        recipeRepository = recipes,
        favoritesRepository = favorites,
        recentlyViewedRepository = history,
        connectivityObserver = connectivity,
    )

    private fun recipe(detailsLoaded: Boolean) = Recipe(
        id = "1",
        name = "Kedgeree",
        category = "Seafood",
        area = "British",
        thumbnailUrl = null,
        instructions = if (detailsLoaded) "Boil rice.\nFlake fish." else null,
        ingredients = if (detailsLoaded) listOf(Ingredient("Rice", "1 cup")) else emptyList(),
        detailsLoaded = detailsLoaded,
    )

    @Test
    fun `opening a recipe records it in history once`() = runTest(mainDispatcherRule.dispatcher) {
        createViewModel()
        advanceUntilIdle()

        assertEquals(listOf("1"), history.recorded)
    }

    @Test
    fun `full recipe is shown with numbered steps`() = runTest(mainDispatcherRule.dispatcher) {
        recipes.details["1"] = MutableStateFlow(RecipeDetail(recipe(detailsLoaded = true), SyncStatus.Synced))
        val viewModel = createViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value as RecipeDetailUiState.Success
        assertEquals(DetailsState.Loaded, state.details)
        assertEquals(listOf("Boil rice.", "Flake fish."), state.instructionSteps)
    }

    @Test
    fun `basic recipe offline shows available info and a missing-details state`() = runTest(mainDispatcherRule.dispatcher) {
        recipes.details["1"] = MutableStateFlow(
            RecipeDetail(recipe(detailsLoaded = false), SyncStatus.Failed(DataError.NO_CONNECTION))
        )
        val viewModel = createViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value as RecipeDetailUiState.Success
        assertEquals("Kedgeree", state.recipe.name)
        assertEquals(DetailsState.Missing(DataError.NO_CONNECTION), state.details)
    }

    @Test
    fun `nothing cached and download failed shows unavailable`() = runTest(mainDispatcherRule.dispatcher) {
        recipes.details["1"] = MutableStateFlow(RecipeDetail(null, SyncStatus.Failed(DataError.TIMEOUT)))
        val viewModel = createViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(RecipeDetailUiState.Unavailable(DataError.TIMEOUT), viewModel.uiState.value)
    }

    @Test
    fun `toggling the heart saves and removes the favorite`() = runTest(mainDispatcherRule.dispatcher) {
        recipes.details["1"] = MutableStateFlow(RecipeDetail(recipe(detailsLoaded = true), SyncStatus.Synced))
        val viewModel = createViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.toggleFavorite()
        advanceUntilIdle()
        assertTrue((viewModel.uiState.value as RecipeDetailUiState.Success).isFavorite)

        viewModel.toggleFavorite()
        advanceUntilIdle()
        assertEquals(false, (viewModel.uiState.value as RecipeDetailUiState.Success).isFavorite)
    }
}
