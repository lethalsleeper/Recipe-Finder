package com.example.recipefinder.presentation.home

import androidx.lifecycle.SavedStateHandle
import com.example.recipefinder.domain.model.Category
import com.example.recipefinder.domain.model.DataError
import com.example.recipefinder.domain.model.RecipeListing
import com.example.recipefinder.domain.model.RefreshResult
import com.example.recipefinder.domain.model.SyncStatus
import com.example.recipefinder.presentation.fakes.FakeConnectivityObserver
import com.example.recipefinder.presentation.fakes.FakeRecipeRepository
import com.example.recipefinder.presentation.fakes.summary
import com.example.recipefinder.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeRecipeRepository()
    private val connectivity = FakeConnectivityObserver()

    private fun createViewModel() = HomeViewModel(SavedStateHandle(), repository, connectivity)

    private fun givenCategories(vararg names: String) {
        repository.categories.value = names.map { Category(it, null, null) }
    }

    @Test
    fun `first launch selects Chicken and shows loading until recipes arrive`() = runTest(mainDispatcherRule.dispatcher) {
        givenCategories("Beef", "Chicken", "Dessert")
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals("Chicken", viewModel.uiState.value.selectedCategory)
        assertEquals(ListingRequest.Browse("Chicken"), viewModel.uiState.value.activeRequest)
        assertEquals(HomeContent.Loading, viewModel.uiState.value.content)

        repository.categoryListing("Chicken").value =
            RecipeListing(listOf(summary("1"), summary("2")), SyncStatus.Synced)
        advanceUntilIdle()

        val content = viewModel.uiState.value.content as HomeContent.Recipes
        assertEquals(listOf("1", "2"), content.recipes.map { it.id })
        assertEquals(false, content.isRefreshing)
        assertEquals(null, content.notice)
    }

    @Test
    fun `falls back to the first category when Chicken does not exist`() = runTest(mainDispatcherRule.dispatcher) {
        givenCategories("Seafood", "Dessert")
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals("Seafood", viewModel.uiState.value.selectedCategory)
    }

    @Test
    fun `search is debounced and only the final query is requested`() = runTest(mainDispatcherRule.dispatcher) {
        givenCategories("Chicken")
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onQueryChange("p")
        advanceTimeBy(100)
        viewModel.onQueryChange("pa")
        advanceTimeBy(100)
        viewModel.onQueryChange("pas")
        advanceTimeBy(100)
        viewModel.onQueryChange("pasta")
        advanceTimeBy(HomeViewModel.SEARCH_DEBOUNCE_MS - 50)
        runCurrent()
        assertTrue("No search before the pause", repository.searchCalls.isEmpty())

        advanceTimeBy(100)
        runCurrent()
        assertEquals(listOf("pasta"), repository.searchCalls)
        assertEquals(ListingRequest.Search("pasta"), viewModel.uiState.value.activeRequest)
    }

    @Test
    fun `search results replace the category list`() = runTest(mainDispatcherRule.dispatcher) {
        givenCategories("Chicken")
        val viewModel = createViewModel()
        repository.searchListing("cake").value =
            RecipeListing(listOf(summary("9", "Carrot cake", "Dessert")), SyncStatus.Synced)

        viewModel.onQueryChange("cake")
        advanceUntilIdle()

        val content = viewModel.uiState.value.content as HomeContent.Recipes
        assertEquals(listOf("Carrot cake"), content.recipes.map { it.name })
    }

    @Test
    fun `search with no matches shows no results`() = runTest(mainDispatcherRule.dispatcher) {
        givenCategories("Chicken")
        val viewModel = createViewModel()
        repository.searchListing("zzz").value = RecipeListing(emptyList(), SyncStatus.Synced)

        viewModel.onQueryChange("zzz")
        advanceUntilIdle()

        assertEquals(HomeContent.NoResults("zzz"), viewModel.uiState.value.content)
    }

    @Test
    fun `offline search with cached matches shows them with an offline notice`() = runTest(mainDispatcherRule.dispatcher) {
        givenCategories("Chicken")
        val viewModel = createViewModel()
        repository.searchListing("soup").value =
            RecipeListing(listOf(summary("3", "Fish soup")), SyncStatus.Failed(DataError.NO_CONNECTION))

        viewModel.onQueryChange("soup")
        advanceUntilIdle()

        val content = viewModel.uiState.value.content as HomeContent.Recipes
        assertEquals(listOf("Fish soup"), content.recipes.map { it.name })
        assertEquals(CacheNotice.OFFLINE, content.notice)
    }

    @Test
    fun `offline search without cache explains it is offline instead of claiming no results`() =
        runTest(mainDispatcherRule.dispatcher) {
            givenCategories("Chicken")
            val viewModel = createViewModel()
            repository.searchListing("soup").value =
                RecipeListing(emptyList(), SyncStatus.Failed(DataError.NO_CONNECTION))

            viewModel.onQueryChange("soup")
            advanceUntilIdle()

            assertEquals(
                HomeContent.OfflineNoCache(ListingRequest.Search("soup")),
                viewModel.uiState.value.content,
            )
        }

    @Test
    fun `server failure without cache shows an error`() = runTest(mainDispatcherRule.dispatcher) {
        givenCategories("Chicken")
        val viewModel = createViewModel()
        repository.searchListing("soup").value = RecipeListing(emptyList(), SyncStatus.Failed(DataError.SERVER))

        viewModel.onQueryChange("soup")
        advanceUntilIdle()

        assertEquals(HomeContent.Error(DataError.SERVER), viewModel.uiState.value.content)
    }

    @Test
    fun `clearing the search returns to the selected category`() = runTest(mainDispatcherRule.dispatcher) {
        givenCategories("Chicken")
        val viewModel = createViewModel()
        repository.categoryListing("Chicken").value = RecipeListing(listOf(summary("1")), SyncStatus.Synced)
        viewModel.onQueryChange("cake")
        advanceUntilIdle()

        viewModel.onClearQuery()
        advanceUntilIdle()

        assertEquals(ListingRequest.Browse("Chicken"), viewModel.uiState.value.activeRequest)
        assertTrue(viewModel.uiState.value.content is HomeContent.Recipes)
        assertEquals("", viewModel.searchQuery)
    }

    @Test
    fun `selecting a category leaves search mode`() = runTest(mainDispatcherRule.dispatcher) {
        givenCategories("Chicken", "Dessert")
        val viewModel = createViewModel()
        viewModel.onQueryChange("cake")
        advanceUntilIdle()

        viewModel.onCategorySelected("Dessert")
        advanceUntilIdle()

        assertEquals("", viewModel.searchQuery)
        assertEquals(ListingRequest.Browse("Dessert"), viewModel.uiState.value.activeRequest)
    }

    @Test
    fun `refresh in progress keeps cached recipes visible`() = runTest(mainDispatcherRule.dispatcher) {
        givenCategories("Chicken")
        val viewModel = createViewModel()
        repository.categoryListing("Chicken").value = RecipeListing(listOf(summary("1")), SyncStatus.Syncing)
        advanceUntilIdle()

        val content = viewModel.uiState.value.content as HomeContent.Recipes
        assertTrue(content.isRefreshing)
    }

    @Test
    fun `no cached categories and offline shows the no-categories state`() = runTest(mainDispatcherRule.dispatcher) {
        repository.refreshCategoriesResult = RefreshResult.Failure(DataError.NO_CONNECTION)
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(HomeContent.NoCategories(DataError.NO_CONNECTION), viewModel.uiState.value.content)
    }

    @Test
    fun `reconnecting retries a failed request automatically`() = runTest(mainDispatcherRule.dispatcher) {
        connectivity.online.value = false
        givenCategories("Chicken")
        repository.categoryListing("Chicken").value =
            RecipeListing(emptyList(), SyncStatus.Failed(DataError.NO_CONNECTION))
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isOffline)
        assertEquals(1, repository.categoryCalls.size)

        connectivity.online.value = true
        advanceUntilIdle()

        assertEquals(2, repository.categoryCalls.size)
        assertEquals(false, viewModel.uiState.value.isOffline)
    }
}
