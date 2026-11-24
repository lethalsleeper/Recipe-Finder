package com.example.recipefinder.data.local.database

import androidx.room.TypeConverter
import com.example.recipefinder.data.local.entity.IngredientEntity
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken

/**
 * Stores the ingredient list as a JSON string column. A list of at most 20 small items is always
 * read together with its recipe, so a separate table would add joins without any benefit.
 */
class Converters {

    private val gson = Gson()
    private val ingredientListType = object : TypeToken<List<IngredientEntity?>>() {}.type

    @TypeConverter
    fun ingredientsToJson(ingredients: List<IngredientEntity>): String = gson.toJson(ingredients)

    @TypeConverter
    fun jsonToIngredients(json: String?): List<IngredientEntity> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            gson.fromJson<List<IngredientEntity?>>(json, ingredientListType).orEmpty().filterNotNull()
        } catch (e: JsonParseException) {
            // A corrupted column should not crash the detail screen; treat it as "no ingredients".
            emptyList()
        }
    }
}
