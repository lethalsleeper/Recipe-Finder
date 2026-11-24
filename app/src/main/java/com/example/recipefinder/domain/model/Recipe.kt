package com.example.recipefinder.domain.model

/**
 * Domain models used by ViewModels and the UI. They are independent of Retrofit DTOs and Room
 * entities so that the network or database schema can change without touching the screens.
 */

/** One ingredient line. [measure] is null when TheMealDB did not provide a measurement. */
data class Ingredient(
    val name: String,
    val measure: String?,
)

/** The lightweight information needed to draw a recipe card. */
data class RecipeSummary(
    val id: String,
    val name: String,
    val category: String?,
    val thumbnailUrl: String?,
)

/**
 * A recipe as stored locally. When [detailsLoaded] is false only the basic information from a
 * category listing is known, so [instructions] is null and [ingredients] is empty.
 */
data class Recipe(
    val id: String,
    val name: String,
    val category: String?,
    val area: String?,
    val thumbnailUrl: String?,
    val instructions: String?,
    val ingredients: List<Ingredient>,
    val detailsLoaded: Boolean,
)

data class Category(
    val name: String,
    val thumbnailUrl: String?,
    val description: String?,
)

/** A recently viewed recipe together with the time it was last opened (epoch millis). */
data class HistoryEntry(
    val recipe: RecipeSummary,
    val viewedAt: Long,
)
