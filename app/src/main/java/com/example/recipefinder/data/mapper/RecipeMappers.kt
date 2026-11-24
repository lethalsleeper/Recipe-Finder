package com.example.recipefinder.data.mapper

import com.example.recipefinder.data.local.entity.CategoryEntity
import com.example.recipefinder.data.local.entity.HistoryTuple
import com.example.recipefinder.data.local.entity.IngredientEntity
import com.example.recipefinder.data.local.entity.RecipeEntity
import com.example.recipefinder.data.local.entity.RecipeSummaryTuple
import com.example.recipefinder.data.remote.dto.CategoriesResponseDto
import com.example.recipefinder.data.remote.dto.CategoryDto
import com.example.recipefinder.data.remote.dto.MealDto
import com.example.recipefinder.data.remote.dto.MealSummariesResponseDto
import com.example.recipefinder.data.remote.dto.MealSummaryDto
import com.example.recipefinder.data.remote.dto.MealsResponseDto
import com.example.recipefinder.domain.model.Category
import com.example.recipefinder.domain.model.HistoryEntry
import com.example.recipefinder.domain.model.Ingredient
import com.example.recipefinder.domain.model.Recipe
import com.example.recipefinder.domain.model.RecipeSummary

// ---------------------------------------------------------------------------------------------
// DTO -> Entity. Entries without an id or a name are unusable and are dropped.
// ---------------------------------------------------------------------------------------------

/** Maps a full meal from `search.php` / `lookup.php`. Returns null for unusable entries. */
fun MealDto.toFullEntity(fetchedAt: Long): RecipeEntity? {
    val id = idMeal.cleaned() ?: return null
    val name = strMeal.cleaned() ?: return null
    return RecipeEntity(
        idMeal = id,
        name = name,
        category = strCategory.cleaned(),
        thumbnail = strMealThumb.cleaned(),
        // Newer TheMealDB records sometimes leave strArea empty and fill strCountry instead.
        area = strArea.cleaned() ?: strCountry.cleaned(),
        instructions = strInstructions.normalizedInstructions(),
        ingredients = IngredientParser.parse(ingredientFields(), measureFields()),
        // We asked for the complete recipe, so it is "loaded" even if some fields are empty.
        detailsLoaded = true,
        lastFetchedAt = fetchedAt,
    )
}

/**
 * Maps a basic entry from `filter.php`. The response does not include the category, so the
 * category that was requested is stored with it.
 */
fun MealSummaryDto.toBasicEntity(category: String, fetchedAt: Long): RecipeEntity? {
    val id = idMeal.cleaned() ?: return null
    val name = strMeal.cleaned() ?: return null
    return RecipeEntity(
        idMeal = id,
        name = name,
        category = category,
        thumbnail = strMealThumb.cleaned(),
        area = null,
        instructions = null,
        ingredients = emptyList(),
        detailsLoaded = false,
        lastFetchedAt = fetchedAt,
    )
}

fun CategoryDto.toEntity(sortOrder: Int): CategoryEntity? {
    val name = strCategory.cleaned() ?: return null
    return CategoryEntity(
        name = name,
        thumbnail = strCategoryThumb.cleaned(),
        description = strCategoryDescription?.trim()?.takeIf { it.isNotEmpty() },
        sortOrder = sortOrder,
    )
}

/** `{"meals": null}` (no matches) becomes an empty list instead of an error. */
fun MealsResponseDto.toFullEntities(fetchedAt: Long): List<RecipeEntity> =
    meals.orEmpty().filterNotNull().mapNotNull { it.toFullEntity(fetchedAt) }.distinctBy { it.idMeal }

fun MealSummariesResponseDto.toBasicEntities(category: String, fetchedAt: Long): List<RecipeEntity> =
    meals.orEmpty().filterNotNull().mapNotNull { it.toBasicEntity(category, fetchedAt) }.distinctBy { it.idMeal }

fun CategoriesResponseDto.toEntities(): List<CategoryEntity> =
    categories.orEmpty().filterNotNull()
        .mapIndexedNotNull { index, dto -> dto.toEntity(sortOrder = index) }
        .distinctBy { it.name }

/** Unifies line endings and trims; the wording itself is never changed. */
private fun String?.normalizedInstructions(): String? =
    this?.replace("\r\n", "\n")?.replace('\r', '\n')?.trim()?.takeIf { it.isNotEmpty() }

// ---------------------------------------------------------------------------------------------
// Entity -> Domain
// ---------------------------------------------------------------------------------------------

fun RecipeEntity.toDomain(): Recipe = Recipe(
    id = idMeal,
    name = name,
    category = category,
    area = area,
    thumbnailUrl = thumbnail,
    instructions = instructions,
    ingredients = ingredients.map { it.toDomain() },
    detailsLoaded = detailsLoaded,
)

fun IngredientEntity.toDomain(): Ingredient = Ingredient(name = name, measure = measure)

fun RecipeSummaryTuple.toDomain(): RecipeSummary = RecipeSummary(
    id = idMeal,
    name = name,
    category = category,
    thumbnailUrl = thumbnail,
)

fun HistoryTuple.toDomain(): HistoryEntry = HistoryEntry(
    recipe = RecipeSummary(id = idMeal, name = name, category = category, thumbnailUrl = thumbnail),
    viewedAt = viewedAt,
)

fun CategoryEntity.toDomain(): Category = Category(
    name = name,
    thumbnailUrl = thumbnail,
    description = description,
)
