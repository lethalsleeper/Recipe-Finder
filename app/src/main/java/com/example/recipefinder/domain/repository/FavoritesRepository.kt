package com.example.recipefinder.domain.repository

import com.example.recipefinder.domain.model.RecipeSummary
import kotlinx.coroutines.flow.Flow

/** Favorites are stored in their own table, so refreshing recipes never removes them. */
interface FavoritesRepository {

    /** Saved recipes, most recently saved first. */
    fun observeFavorites(): Flow<List<RecipeSummary>>

    fun isFavorite(recipeId: String): Flow<Boolean>

    /** Saves the recipe if it is not a favorite yet, otherwise removes it. Returns the new state. */
    suspend fun toggleFavorite(recipeId: String): Boolean
}
