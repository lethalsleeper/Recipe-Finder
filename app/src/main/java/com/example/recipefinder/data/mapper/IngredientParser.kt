package com.example.recipefinder.data.mapper

import com.example.recipefinder.data.local.entity.IngredientEntity

/**
 * Turns TheMealDB's numbered `strIngredientN` / `strMeasureN` fields into an ingredient list.
 *
 * Rules:
 * 1. Pairs are read by position, so ingredient N always gets measure N (order is preserved).
 * 2. Null, empty or whitespace-only ingredients are skipped — the API pads unused slots with
 *    "" or null, and those must not become empty rows.
 * 3. Text is trimmed and runs of whitespace are collapsed to a single space.
 * 4. A missing measure stays null; we never invent one.
 */
object IngredientParser {

    const val MAX_INGREDIENTS = 20

    fun parse(ingredients: List<String?>, measures: List<String?>): List<IngredientEntity> =
        ingredients
            .take(MAX_INGREDIENTS)
            .mapIndexedNotNull { index, rawName ->
                val name = rawName.cleaned() ?: return@mapIndexedNotNull null
                IngredientEntity(name = name, measure = measures.getOrNull(index).cleaned())
            }
}

private val whitespaceRun = Regex("\\s+")

/** Trims, collapses inner whitespace and converts blank strings to null. */
internal fun String?.cleaned(): String? =
    this?.trim()
        ?.replace(whitespaceRun, " ")
        ?.takeIf { it.isNotEmpty() }
