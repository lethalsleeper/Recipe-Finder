package com.example.recipefinder.data.mapper

import com.example.recipefinder.data.local.entity.IngredientEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IngredientParserTest {

    @Test
    fun `parses all 20 ingredient and measure pairs in order`() {
        val names = (1..20).map { "Ingredient $it" }
        val measures = (1..20).map { "$it g" }

        val result = IngredientParser.parse(names, measures)

        assertEquals(20, result.size)
        result.forEachIndexed { index, ingredient ->
            assertEquals("Ingredient ${index + 1}", ingredient.name)
            assertEquals("${index + 1} g", ingredient.measure)
        }
    }

    @Test
    fun `skips null, empty and whitespace-only ingredients but keeps pairing by position`() {
        val names = listOf("Flour", "", null, "   ", "Eggs", "\t\n")
        val measures = listOf("2 cups", "1 tsp", "1 tbsp", "pinch", "3", "1")

        val result = IngredientParser.parse(names, measures)

        assertEquals(
            listOf(
                IngredientEntity("Flour", "2 cups"),
                IngredientEntity("Eggs", "3"),
            ),
            result,
        )
    }

    @Test
    fun `missing measurement stays null instead of being invented`() {
        val result = IngredientParser.parse(
            ingredients = listOf("Salt", "Pepper", "Olive oil"),
            measures = listOf(null, "  ", "1 tbsp"),
        )

        assertNull(result[0].measure)
        assertNull(result[1].measure)
        assertEquals("1 tbsp", result[2].measure)
    }

    @Test
    fun `measure list shorter than ingredient list is handled`() {
        val result = IngredientParser.parse(listOf("Rice", "Water"), listOf("1 cup"))

        assertEquals(IngredientEntity("Rice", "1 cup"), result[0])
        assertEquals(IngredientEntity("Water", null), result[1])
    }

    @Test
    fun `trims and collapses whitespace`() {
        val result = IngredientParser.parse(listOf("  Olive   oil \r\n"), listOf(" 1  tbsp "))

        assertEquals(IngredientEntity("Olive oil", "1 tbsp"), result.single())
    }

    @Test
    fun `all blank fields give an empty list`() {
        val result = IngredientParser.parse(List(20) { "" }, List(20) { null })

        assertTrue(result.isEmpty())
    }
}
