package com.example.recipefinder.domain.repository

import com.example.recipefinder.domain.model.Category
import com.example.recipefinder.domain.model.RecipeDetail
import com.example.recipefinder.domain.model.RecipeListing
import com.example.recipefinder.domain.model.RefreshResult
import kotlinx.coroutines.flow.Flow

/**
 * Offline-first access to recipes and categories.
 *
 * Every Flow here is backed by Room. Collecting a listing or detail Flow also starts a network
 * refresh in the background; when the refresh saves new rows, Room emits again and the UI
 * updates. If the refresh fails, the cached rows keep flowing and the status reports the error.
 */
interface RecipeRepository {

    fun observeCategories(): Flow<List<Category>>

    suspend fun refreshCategories(): RefreshResult

    /** Cached recipes of [category]; collecting it also refreshes the category from the API. */
    fun recipesInCategory(category: String): Flow<RecipeListing>

    /** Cached recipes whose name contains [query]; collecting it also searches the API. */
    fun searchRecipes(query: String): Flow<RecipeListing>

    /**
     * The stored recipe [id]. Full details are downloaded only when they are not cached yet,
     * so opening an already-downloaded recipe never touches the network.
     */
    fun recipeDetail(id: String): Flow<RecipeDetail>
}
