package com.example.recipefinder.presentation.collections

import com.example.recipefinder.domain.model.HistoryEntry
import com.example.recipefinder.presentation.common.CollectionUiState
import com.example.recipefinder.presentation.fakes.FakeFavoritesRepository
import com.example.recipefinder.presentation.fakes.FakeRecentlyViewedRepository
import com.example.recipefinder.presentation.fakes.summary
import com.example.recipefinder.presentation.favorites.FavoritesViewModel
import com.example.recipefinder.presentation.recentlyviewed.RecentlyViewedViewModel
import com.example.recipefinder.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionViewModelsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `favorites go from empty to items`() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeFavoritesRepository()
        val viewModel = FavoritesViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }

        assertEquals(CollectionUiState.Loading, viewModel.uiState.value)
        advanceUntilIdle()
        assertEquals(CollectionUiState.Empty, viewModel.uiState.value)

        repository.toggleFavorite("7")
        advanceUntilIdle()
        assertEquals(CollectionUiState.Items(listOf(summary("7"))), viewModel.uiState.value)
    }

    @Test
    fun `recently viewed exposes entries in repository order`() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeRecentlyViewedRepository()
        val viewModel = RecentlyViewedViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        assertEquals(CollectionUiState.Empty, viewModel.uiState.value)

        val entries = listOf(HistoryEntry(summary("2"), viewedAt = 20), HistoryEntry(summary("1"), viewedAt = 10))
        repository.history.value = entries
        advanceUntilIdle()
        assertEquals(CollectionUiState.Items(entries), viewModel.uiState.value)
    }
}
