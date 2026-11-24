package com.example.recipefinder.presentation.home

import com.example.recipefinder.domain.model.Category
import com.example.recipefinder.domain.model.DataError
import com.example.recipefinder.domain.model.RecipeListing
import com.example.recipefinder.domain.model.RecipeSummary
import com.example.recipefinder.domain.model.SyncStatus

/** What the recipe list on the Home screen is currently showing. */
sealed interface ListingRequest {
    data class Search(val query: String) : ListingRequest
    data class Browse(val category: String) : ListingRequest
    /** No category selected yet (categories are still loading). */
    data object None : ListingRequest
}

data class HomeUiState(
    val categories: List<Category> = emptyList(),
    val categoriesLoading: Boolean = true,
    val selectedCategory: String? = null,
    val isOffline: Boolean = false,
    val activeRequest: ListingRequest = ListingRequest.None,
    val content: HomeContent = HomeContent.Loading,
)

/**
 * One state per situation instead of a mix of booleans, so the screen can't end up in an
 * impossible combination such as "loading and error at the same time".
 */
sealed interface HomeContent {
    /** Nothing cached yet and a request is running. */
    data object Loading : HomeContent

    /**
     * Recipes from the local database. [isRefreshing] is true while a network refresh runs in
     * the background; [notice] explains when the list is cached data after a failed refresh.
     */
    data class Recipes(
        val recipes: List<RecipeSummary>,
        val isRefreshing: Boolean,
        val notice: CacheNotice?,
    ) : HomeContent

    /** The API answered and nothing matches the search. */
    data class NoResults(val query: String) : HomeContent

    /** The API answered and the category has no recipes. */
    data class EmptyCategory(val category: String) : HomeContent

    /** Offline and nothing cached for this request — not the same as "no results". */
    data class OfflineNoCache(val request: ListingRequest) : HomeContent

    /** The request failed for another reason and nothing is cached. */
    data class Error(val error: DataError) : HomeContent

    /** The category list could not be loaded and none is cached. */
    data class NoCategories(val error: DataError) : HomeContent
}

enum class CacheNotice { OFFLINE, REFRESH_FAILED }

/**
 * Pure mapping from repository output to screen state; kept outside the ViewModel so it is easy
 * to read and to test.
 */
fun RecipeListing.toHomeContent(request: ListingRequest): HomeContent {
    val currentStatus = status
    if (recipes.isNotEmpty()) {
        return HomeContent.Recipes(
            recipes = recipes,
            isRefreshing = currentStatus is SyncStatus.Syncing,
            notice = when {
                currentStatus !is SyncStatus.Failed -> null
                currentStatus.error == DataError.NO_CONNECTION -> CacheNotice.OFFLINE
                else -> CacheNotice.REFRESH_FAILED
            },
        )
    }
    return when (currentStatus) {
        SyncStatus.Syncing -> HomeContent.Loading
        SyncStatus.Synced -> when (request) {
            is ListingRequest.Search -> HomeContent.NoResults(request.query)
            is ListingRequest.Browse -> HomeContent.EmptyCategory(request.category)
            ListingRequest.None -> HomeContent.Loading
        }
        is SyncStatus.Failed ->
            if (currentStatus.error == DataError.NO_CONNECTION) HomeContent.OfflineNoCache(request)
            else HomeContent.Error(currentStatus.error)
    }
}

/** True when the content would benefit from trying the network again. */
internal val HomeContent.wouldBenefitFromRetry: Boolean
    get() = when (this) {
        is HomeContent.Recipes -> notice != null
        is HomeContent.OfflineNoCache, is HomeContent.Error, is HomeContent.NoCategories -> true
        else -> false
    }
