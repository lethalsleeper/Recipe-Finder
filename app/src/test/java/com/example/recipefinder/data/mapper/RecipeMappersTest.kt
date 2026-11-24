package com.example.recipefinder.data.mapper

import com.example.recipefinder.data.local.entity.IngredientEntity
import com.example.recipefinder.data.remote.dto.CategoriesResponseDto
import com.example.recipefinder.data.remote.dto.MealSummariesResponseDto
import com.example.recipefinder.data.remote.dto.MealsResponseDto
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Parses real-shaped TheMealDB JSON with Gson (as Retrofit does) and checks the mapping. */
class RecipeMappersTest {

    private val gson = Gson()
    private val now = 1_700_000_000_000L

    @Test
    fun `maps a complete recipe response`() {
        val json = """
            {"meals":[{
              "idMeal":"52772","strMeal":"Teriyaki Chicken Casserole","strCategory":"Chicken",
              "strArea":"Japanese","strMealThumb":"https://www.themealdb.com/images/media/meals/wvpsxx1468256321.jpg",
              "strInstructions":"Preheat oven to 350° F.\r\nCombine soy sauce.\r\n",
              "strIngredient1":"soy sauce","strMeasure1":"3/4 cup",
              "strIngredient2":"water","strMeasure2":"1/2 cup",
              "strIngredient3":"","strMeasure3":"",
              "strIngredient4":null,"strMeasure4":null
            }]}
        """.trimIndent()

        val entity = gson.fromJson(json, MealsResponseDto::class.java).toFullEntities(now).single()

        assertEquals("52772", entity.idMeal)
        assertEquals("Teriyaki Chicken Casserole", entity.name)
        assertEquals("Chicken", entity.category)
        assertEquals("Japanese", entity.area)
        assertEquals("https://www.themealdb.com/images/media/meals/wvpsxx1468256321.jpg", entity.thumbnail)
        assertEquals("Preheat oven to 350° F.\nCombine soy sauce.", entity.instructions)
        assertEquals(
            listOf(IngredientEntity("soy sauce", "3/4 cup"), IngredientEntity("water", "1/2 cup")),
            entity.ingredients,
        )
        assertTrue(entity.detailsLoaded)
        assertEquals(now, entity.lastFetchedAt)
    }

    @Test
    fun `parses all 20 numbered ingredient fields from JSON`() {
        val fields = (1..20).joinToString(",") { "\"strIngredient$it\":\"Item $it\",\"strMeasure$it\":\"$it tsp\"" }
        val json = """{"meals":[{"idMeal":"1","strMeal":"Big Recipe",$fields}]}"""

        val ingredients = gson.fromJson(json, MealsResponseDto::class.java).toFullEntities(now).single().ingredients

        assertEquals(20, ingredients.size)
        assertEquals(IngredientEntity("Item 1", "1 tsp"), ingredients.first())
        assertEquals(IngredientEntity("Item 20", "20 tsp"), ingredients.last())
    }

    @Test
    fun `null meals in a search response maps to an empty list`() {
        val response = gson.fromJson("""{"meals":null}""", MealsResponseDto::class.java)

        assertTrue(response.toFullEntities(now).isEmpty())
    }

    @Test
    fun `missing meals key maps to an empty list`() {
        val response = gson.fromJson("{}", MealsResponseDto::class.java)

        assertTrue(response.toFullEntities(now).isEmpty())
    }

    @Test
    fun `missing optional fields are stored as null, not crashes`() {
        val json = """{"meals":[{"idMeal":"7","strMeal":"Plain Toast"}]}"""

        val entity = gson.fromJson(json, MealsResponseDto::class.java).toFullEntities(now).single()

        assertNull(entity.category)
        assertNull(entity.area)
        assertNull(entity.thumbnail)
        assertNull(entity.instructions)
        assertTrue(entity.ingredients.isEmpty())
    }

    @Test
    fun `falls back to strCountry when strArea is empty`() {
        val json = """{"meals":[{"idMeal":"8","strMeal":"Moqueca","strArea":null,"strCountry":"Brazil"}]}"""

        val entity = gson.fromJson(json, MealsResponseDto::class.java).toFullEntities(now).single()

        assertEquals("Brazil", entity.area)
    }

    @Test
    fun `entries without id or name and null entries are dropped`() {
        val json = """{"meals":[null,{"strMeal":"No id"},{"idMeal":"5","strMeal":"  "},{"idMeal":"6","strMeal":"Valid"}]}"""

        val entities = gson.fromJson(json, MealsResponseDto::class.java).toFullEntities(now)

        assertEquals(listOf("6"), entities.map { it.idMeal })
    }

    @Test
    fun `category filter response maps to basic entities with the requested category`() {
        val json = """{"meals":[{"strMeal":"Baked salmon","strMealThumb":"https://x/salmon.jpg","idMeal":"52959"}]}"""

        val entity = gson.fromJson(json, MealSummariesResponseDto::class.java)
            .toBasicEntities(category = "Seafood", fetchedAt = now)
            .single()

        assertEquals("52959", entity.idMeal)
        assertEquals("Seafood", entity.category)
        assertFalse(entity.detailsLoaded)
        assertNull(entity.instructions)
        assertTrue(entity.ingredients.isEmpty())
    }

    @Test
    fun `categories response keeps API order`() {
        val json = """{"categories":[
            {"idCategory":"1","strCategory":"Beef","strCategoryThumb":"b.png","strCategoryDescription":"Beef"},
            {"idCategory":"2","strCategory":"Chicken","strCategoryThumb":"c.png","strCategoryDescription":""},
            {"idCategory":"3","strCategory":""}
        ]}"""

        val categories = gson.fromJson(json, CategoriesResponseDto::class.java).toEntities()

        assertEquals(listOf("Beef", "Chicken"), categories.map { it.name })
        assertEquals(listOf(0, 1), categories.map { it.sortOrder })
        assertNull(categories[1].description)
    }
}
