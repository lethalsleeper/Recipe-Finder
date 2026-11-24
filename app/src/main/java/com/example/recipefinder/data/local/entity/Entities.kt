package com.example.recipefinder.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row per recipe, keyed by TheMealDB's `idMeal`.
 *
 * A row can be "basic" (from a category listing: id, name, thumbnail, category) or "full"
 * (from search / lookup: everything). [detailsLoaded] tells the two apart, and the DAO makes sure
 * a basic refresh never overwrites the full columns.
 */
@Entity(
    tableName = "recipes",
    indices = [Index("category"), Index("name")],
)
data class RecipeEntity(
    @PrimaryKey val idMeal: String,
    val name: String,
    val category: String?,
    val thumbnail: String?,
    val area: String?,
    val instructions: String?,
    /** Stored as JSON through [com.example.recipefinder.data.local.database.Converters]. */
    val ingredients: List<IngredientEntity>,
    val detailsLoaded: Boolean,
    val lastFetchedAt: Long,
)

/** Embedded in [RecipeEntity.ingredients]; not a table of its own. */
data class IngredientEntity(
    val name: String,
    val measure: String?,
)

/**
 * Favorites live in their own table with no foreign key to `recipes`, so nothing that happens
 * to the recipe cache (refreshes, replacements) can delete a favorite.
 */
@Entity(
    tableName = "favorites",
    indices = [Index("savedAt")],
)
data class FavoriteEntity(
    @PrimaryKey val recipeId: String,
    val savedAt: Long,
)

/** One row per recipe; revisiting a recipe updates [viewedAt] instead of adding a duplicate. */
@Entity(
    tableName = "recently_viewed",
    indices = [Index("viewedAt")],
)
data class RecentlyViewedEntity(
    @PrimaryKey val recipeId: String,
    val viewedAt: Long,
)

/** Cached category list so the chips are still available offline. */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val name: String,
    val thumbnail: String?,
    val description: String?,
    /** Keeps the order the API returned the categories in. */
    val sortOrder: Int,
)

/** Projection used by list screens, so they don't have to load ingredients and instructions. */
data class RecipeSummaryTuple(
    @ColumnInfo(name = "idMeal") val idMeal: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "category") val category: String?,
    @ColumnInfo(name = "thumbnail") val thumbnail: String?,
)

/** A recipe summary joined with its history timestamp. */
data class HistoryTuple(
    @ColumnInfo(name = "idMeal") val idMeal: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "category") val category: String?,
    @ColumnInfo(name = "thumbnail") val thumbnail: String?,
    @ColumnInfo(name = "viewedAt") val viewedAt: Long,
)
