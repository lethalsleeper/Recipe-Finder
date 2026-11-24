package com.example.recipefinder.presentation.fakes

import com.example.recipefinder.domain.connectivity.ConnectivityObserver
import com.example.recipefinder.domain.model.Category
import com.example.recipefinder.domain.model.HistoryEntry
import com.example.recipefinder.domain.model.RecipeDetail
import com.example.recipefinder.domain.model.RecipeListing
import com.example.recipefinder.domain.model.RecipeSummary
import com.example.recipefinder.domain.model.RefreshResult
import com.example.recipefinder.domain.model.SyncStatus
import com.example.recipefinder.domain.repository.FavoritesRepository
import com.example.recipefinder.domain.repository.RecentlyViewedRepository
import com.example.recipefinder.domain.repository.RecipeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

fun summary(id: String, name: String = "Recipe $id", category: String = "Chicken") =
    RecipeSummary(id = id, name = name, category = category, thumbnailUrl = null)

/**
 * In-memory [RecipeRepository] whose listing Flows the test controls directly. It records which
 * searches and categories were actually *collected*, which is what triggers network calls in
 * the real implementation.
 */
class FakeRecipeRepository : RecipeRepository {

    val categories = MutableStateFlow<List<Category>>(emptyList())
    var refreshCategoriesResult: RefreshResult = RefreshResult.Success
    var refreshCategoriesCalls = 0

    private val searchListings = mutableMapOf<String, MutableStateFlow<RecipeListing>>()
    private val categoryListings = mutableMapOf<String, MutableStateFlow<RecipeListing>>()
    val searchCalls = mutableListOf<String>()
    val categoryCalls = mutableListOf<String>()

    val details = mutableMapOf<String, MutableStateFlow<RecipeDetail>>()

    fun searchListing(query: String) =
        searchListings.getOrPut(query) { MutableStateFlow(RecipeListing(emptyList(), SyncStatus.Syncing)) }

    fun categoryListing(category: String) =
        categoryListings.getOrPut(category) { MutableStateFlow(RecipeListing(emptyList(), SyncStatus.Syncing)) }

    override fun observeCategories(): Flow<List<Category>> = categories

    override suspend fun refreshCategories(): RefreshResult {
        refreshCategoriesCalls++
        return refreshCategoriesResult
    }

    override fun recipesInCategory(category: String): Flow<RecipeListing> = flow {
        categoryCalls += category
        emitAll(categoryListing(category))
    }

    override fun searchRecipes(query: String): Flow<RecipeListing> = flow {
        searchCalls += query
        emitAll(searchListing(query))
    }

    override fun recipeDetail(id: String): Flow<RecipeDetail> =
        details.getOrPut(id) { MutableStateFlow(RecipeDetail(null, SyncStatus.Syncing)) }
}

class FakeFavoritesRepository : FavoritesRepository {
    val favoriteIds = MutableStateFlow<Set<String>>(emptySet())

    override fun observeFavorites(): Flow<List<RecipeSummary>> =
        favoriteIds.map { ids -> ids.map { summary(it) } }

    override fun isFavorite(recipeId: String): Flow<Boolean> = favoriteIds.map { recipeId in it }

    override suspend fun toggleFavorite(recipeId: String): Boolean {
        val nowFavorite = recipeId !in favoriteIds.value
        favoriteIds.value = if (nowFavorite) favoriteIds.value + recipeId else favoriteIds.value - recipeId
        return nowFavorite
    }
}

class FakeRecentlyViewedRepository : RecentlyViewedRepository {
    val recorded = mutableListOf<String>()
    val history = MutableStateFlow<List<HistoryEntry>>(emptyList())

    override fun observeRecentlyViewed(): Flow<List<HistoryEntry>> = history

    override suspend fun recordView(recipeId: String) {
        recorded += recipeId
    }
}

class FakeConnectivityObserver(online: Boolean = true) : ConnectivityObserver {
    val online = MutableStateFlow(online)
    override val isOnline: Flow<Boolean> = this.online
}
