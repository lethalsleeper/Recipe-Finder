package com.example.recipefinder.data.local

import app.cash.turbine.test
import com.example.recipefinder.data.local.database.RecipeDatabase
import com.example.recipefinder.data.local.entity.IngredientEntity
import com.example.recipefinder.testutil.basicRecipeEntity
import com.example.recipefinder.testutil.fullRecipeEntity
import com.example.recipefinder.testutil.inMemoryDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Real Room database (in memory) running on the JVM through Robolectric. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RecipeDatabaseTest {

    private lateinit var db: RecipeDatabase

    @Before
    fun setUp() {
        db = inMemoryDatabase()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ---- Recipes -------------------------------------------------------------------------

    @Test
    fun insertAndRetrieveRecipe_includingIngredientList() = runTest {
        val recipe = fullRecipeEntity(
            id = "1",
            ingredients = listOf(IngredientEntity("Flour", "2 cups"), IngredientEntity("Eggs", null)),
        )

        db.recipeDao().upsertFull(listOf(recipe))

        assertEquals(recipe, db.recipeDao().getRecipe("1"))
    }

    @Test
    fun observeRecipesByCategory_emitsUpdates() = runTest {
        db.recipeDao().observeSummariesByCategory("Seafood").test {
            assertTrue(awaitItem().isEmpty())

            db.recipeDao().upsertBasic(listOf(basicRecipeEntity("1", name = "Baked salmon")))
            assertEquals(listOf("Baked salmon"), awaitItem().map { it.name })

            db.recipeDao().upsertBasic(listOf(basicRecipeEntity("2", name = "Apam balik")))
            assertEquals(listOf("Apam balik", "Baked salmon"), awaitItem().map { it.name })

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun basicUpsert_preservesFullDetails() = runTest {
        val full = fullRecipeEntity(id = "52959", name = "Baked salmon", instructions = "Bake it.")
        db.recipeDao().upsertFull(listOf(full))

        // Category refresh only knows id, name and thumbnail.
        db.recipeDao().upsertBasic(
            listOf(basicRecipeEntity("52959", name = "Baked Salmon (renamed)", thumbnail = "https://new.jpg", fetchedAt = 99L))
        )

        val stored = db.recipeDao().getRecipe("52959")!!
        assertEquals("Baked Salmon (renamed)", stored.name)
        assertEquals("https://new.jpg", stored.thumbnail)
        assertEquals(99L, stored.lastFetchedAt)
        // Full details are untouched:
        assertTrue(stored.detailsLoaded)
        assertEquals("Bake it.", stored.instructions)
        assertEquals(full.ingredients, stored.ingredients)
        assertEquals(full.area, stored.area)
    }

    @Test
    fun fullUpsert_doesNotWipeFieldsTheNewResponseLacks() = runTest {
        db.recipeDao().upsertFull(listOf(fullRecipeEntity(id = "1", instructions = "Original steps.")))

        db.recipeDao().upsertFull(listOf(fullRecipeEntity(id = "1", instructions = null, ingredients = emptyList())))

        val stored = db.recipeDao().getRecipe("1")!!
        assertEquals("Original steps.", stored.instructions)
        assertEquals(1, stored.ingredients.size)
    }

    @Test
    fun searchQuery_matchesNameCaseInsensitivelyAndIncludesExtraIds() = runTest {
        db.recipeDao().upsertFull(
            listOf(
                fullRecipeEntity("1", name = "Chicken Curry"),
                fullRecipeEntity("2", name = "Beef Stew"),
                fullRecipeEntity("3", name = "Crème Brûlée"),
            )
        )

        val byName = db.recipeDao().observeSearch("%chicken%", emptyList()).first()
        assertEquals(listOf("1"), byName.map { it.idMeal })

        val withExtra = db.recipeDao().observeSearch("%creme%", listOf("3")).first()
        assertEquals(listOf("3"), withExtra.map { it.idMeal })
    }

    // ---- Favorites -----------------------------------------------------------------------

    @Test
    fun saveAndRemoveFavorite() = runTest {
        db.recipeDao().upsertFull(listOf(fullRecipeEntity("1")))
        val favorites = db.favoriteDao()

        assertTrue(favorites.toggle("1", now = 10L))
        assertTrue(favorites.observeIsFavorite("1").first())
        assertEquals(listOf("1"), favorites.observeFavoriteRecipes().first().map { it.idMeal })

        assertFalse(favorites.toggle("1", now = 20L))
        assertFalse(favorites.observeIsFavorite("1").first())
        assertTrue(favorites.observeFavoriteRecipes().first().isEmpty())
    }

    @Test
    fun favoritesSurviveRecipeRefreshes() = runTest {
        db.recipeDao().upsertFull(listOf(fullRecipeEntity("1")))
        db.favoriteDao().toggle("1", now = 10L)

        db.recipeDao().upsertBasic(listOf(basicRecipeEntity("1", name = "Updated name")))
        db.recipeDao().upsertFull(listOf(fullRecipeEntity("1", name = "Updated again")))

        val favorites = db.favoriteDao().observeFavoriteRecipes().first()
        assertEquals(listOf("Updated again"), favorites.map { it.name })
        assertEquals(10L, db.favoriteDao().getAll().single().savedAt)
    }

    @Test
    fun favoriteOfBasicOnlyRecipe_isShownWithAvailableInfo() = runTest {
        db.recipeDao().upsertBasic(listOf(basicRecipeEntity("5", name = "Basic only")))
        db.favoriteDao().toggle("5", now = 1L)

        val favorite = db.favoriteDao().observeFavoriteRecipes().first().single()
        assertEquals("Basic only", favorite.name)
        assertNotNull(favorite.thumbnail)
    }

    // ---- Recently viewed -----------------------------------------------------------------

    @Test
    fun revisitingUpdatesTimestampWithoutDuplicates() = runTest {
        val history = db.recentlyViewedDao()

        history.recordView("1", viewedAt = 100L, maxEntries = 20)
        history.recordView("2", viewedAt = 200L, maxEntries = 20)
        history.recordView("1", viewedAt = 300L, maxEntries = 20)

        val entries = history.getAll()
        assertEquals(listOf("1", "2"), entries.map { it.recipeId })
        assertEquals(300L, entries.first().viewedAt)
    }

    @Test
    fun historyKeepsOnlyLatest20() = runTest {
        val history = db.recentlyViewedDao()

        (1..25).forEach { index -> history.recordView("$index", viewedAt = index.toLong(), maxEntries = 20) }

        val ids = history.getAll().map { it.recipeId }
        assertEquals(20, ids.size)
        assertEquals("25", ids.first())
        assertEquals("6", ids.last())
        assertFalse(ids.contains("5"))
    }

    @Test
    fun historyIsOrderedNewestFirstAndJoinedWithRecipes() = runTest {
        db.recipeDao().upsertFull(listOf(fullRecipeEntity("1", name = "First"), fullRecipeEntity("2", name = "Second")))
        db.recentlyViewedDao().recordView("1", viewedAt = 1L, maxEntries = 20)
        db.recentlyViewedDao().recordView("2", viewedAt = 2L, maxEntries = 20)

        val names = db.recentlyViewedDao().observeHistory().first().map { it.name }
        assertEquals(listOf("Second", "First"), names)
    }
}
