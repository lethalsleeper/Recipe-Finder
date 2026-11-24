package com.example.recipefinder.testutil

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.recipefinder.data.local.database.RecipeDatabase
import com.example.recipefinder.data.local.entity.IngredientEntity
import com.example.recipefinder.data.local.entity.RecipeEntity
import com.example.recipefinder.data.remote.dto.MealDto
import com.example.recipefinder.data.remote.dto.MealSummaryDto

fun inMemoryDatabase(): RecipeDatabase =
    Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext<Context>(),
        RecipeDatabase::class.java,
    )
        .allowMainThreadQueries()
        .build()

fun fullRecipeEntity(
    id: String,
    name: String = "Recipe $id",
    category: String? = "Seafood",
    instructions: String? = "Cook it.\nServe it.",
    ingredients: List<IngredientEntity> = listOf(IngredientEntity("Salmon", "2 fillets")),
    fetchedAt: Long = 1L,
) = RecipeEntity(
    idMeal = id,
    name = name,
    category = category,
    thumbnail = "https://img/$id.jpg",
    area = "British",
    instructions = instructions,
    ingredients = ingredients,
    detailsLoaded = true,
    lastFetchedAt = fetchedAt,
)

fun basicRecipeEntity(
    id: String,
    name: String = "Recipe $id",
    category: String = "Seafood",
    thumbnail: String? = "https://img/$id.jpg",
    fetchedAt: Long = 2L,
) = RecipeEntity(
    idMeal = id,
    name = name,
    category = category,
    thumbnail = thumbnail,
    area = null,
    instructions = null,
    ingredients = emptyList(),
    detailsLoaded = false,
    lastFetchedAt = fetchedAt,
)

fun mealDto(
    id: String,
    name: String,
    category: String = "Seafood",
    instructions: String = "Season.\nBake.",
) = MealDto(
    idMeal = id,
    strMeal = name,
    strCategory = category,
    strArea = "British",
    strInstructions = instructions,
    strMealThumb = "https://img/$id.jpg",
    strIngredient1 = "Salmon",
    strMeasure1 = "2 fillets",
    strIngredient2 = "Lemon",
    strMeasure2 = "",
)

fun mealSummaryDto(id: String, name: String) =
    MealSummaryDto(idMeal = id, strMeal = name, strMealThumb = "https://img/$id-new.jpg")
