package com.example.recipefinder.data.repository

import com.example.recipefinder.data.local.dao.FavoriteDao
import com.example.recipefinder.data.mapper.toDomain
import com.example.recipefinder.domain.model.RecipeSummary
import com.example.recipefinder.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Favorites are purely local data, so this repository never talks to the network. */
class LocalFavoritesRepository(
    private val favoriteDao: FavoriteDao,
    private val clock: () -> Long = System::currentTimeMillis,
) : FavoritesRepository {

    override fun observeFavorites(): Flow<List<RecipeSummary>> =
        favoriteDao.observeFavoriteRecipes().map { rows -> rows.map { it.toDomain() } }

    override fun isFavorite(recipeId: String): Flow<Boolean> =
        favoriteDao.observeIsFavorite(recipeId).distinctUntilChanged()

    override suspend fun toggleFavorite(recipeId: String): Boolean =
        favoriteDao.toggle(recipeId, clock())
}
