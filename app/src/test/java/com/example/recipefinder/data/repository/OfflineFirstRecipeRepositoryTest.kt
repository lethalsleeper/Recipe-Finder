package com.example.recipefinder.data.repository

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.example.recipefinder.data.local.database.RecipeDatabase
import com.example.recipefinder.data.remote.dto.CategoriesResponseDto
import com.example.recipefinder.data.remote.dto.CategoryDto
import com.example.recipefinder.data.remote.dto.MealSummariesResponseDto
import com.example.recipefinder.data.remote.dto.MealsResponseDto
import com.example.recipefinder.domain.model.DataError
import com.example.recipefinder.domain.model.RecipeDetail
import com.example.recipefinder.domain.model.RecipeListing
import com.example.recipefinder.domain.model.RefreshResult
import com.example.recipefinder.domain.model.SyncStatus
import com.example.recipefinder.testutil.fullRecipeEntity
import com.example.recipefinder.testutil.inMemoryDatabase
import com.example.recipefinder.testutil.mealDto
import com.example.recipefinder.testutil.mealSummaryDto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Repository + real in-memory Room + fake API: verifies the offline-first contract end to end. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OfflineFirstRecipeRepositoryTest {

    private lateinit var db: RecipeDatabase
    private lateinit var api: FakeMealApiService
    private lateinit var repository: OfflineFirstRecipeRepository

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        api = FakeMealApiService()
        repository = OfflineFirstRecipeRepository(
            api = api,
            recipeDao = db.recipeDao(),
            categoryDao = db.categoryDao(),
            clock = { 1_000L },
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun searchResultsAreSavedToRoomAndEmittedFromIt() = runTest {
        api.onSearch = { MealsResponseDto(listOf(mealDto("1", "Fish pie"), mealDto("2", "Fish soup"))) }

        repository.searchRecipes("fish").test {
            val synced = awaitStatus<SyncStatus.Synced>()
            assertEquals(listOf("Fish pie", "Fish soup"), synced.recipes.map { it.name })
            cancelAndIgnoreRemainingEvents()
        }

        val stored = db.recipeDao().getRecipe("1")!!
        assertTrue(stored.detailsLoaded)
        assertEquals("Season.\nBake.", stored.instructions)
    }

    @Test
    fun searchWithNullMealsIsSyncedAndEmpty() = runTest {
        api.onSearch = { MealsResponseDto(meals = null) }

        repository.searchRecipes("zzz").test {
            val synced = awaitStatus<SyncStatus.Synced>()
            assertTrue(synced.recipes.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun cachedSearchResultsAreReturnedWhenTheNetworkFails() = runTest {
        db.recipeDao().upsertFull(listOf(fullRecipeEntity("1", name = "Fish pie")))
        api.goOffline()

        repository.searchRecipes("fish").test {
            val failed = awaitStatus<SyncStatus.Failed>()
            assertEquals(DataError.NO_CONNECTION, (failed.status as SyncStatus.Failed).error)
            assertEquals(listOf("Fish pie"), failed.recipes.map { it.name })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun recipesMissingFromANewSearchResponseAreNotDeleted() = runTest {
        db.recipeDao().upsertFull(listOf(fullRecipeEntity("1", name = "Fish pie")))
        api.onSearch = { MealsResponseDto(listOf(mealDto("2", "Fish soup"))) }

        repository.searchRecipes("fish").test {
            val synced = awaitStatus<SyncStatus.Synced>()
            assertEquals(listOf("Fish pie", "Fish soup"), synced.recipes.map { it.name })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun categoryRefreshPreservesFullRecipeDetails() = runTest {
        db.recipeDao().upsertFull(listOf(fullRecipeEntity("52959", name = "Baked salmon", instructions = "Bake.")))
        api.onFilter = { MealSummariesResponseDto(listOf(mealSummaryDto("52959", "Baked salmon"), mealSummaryDto("3", "Kedgeree"))) }

        repository.recipesInCategory("Seafood").test {
            val synced = awaitStatus<SyncStatus.Synced>()
            assertEquals(listOf("Baked salmon", "Kedgeree"), synced.recipes.map { it.name })
            cancelAndIgnoreRemainingEvents()
        }

        val salmon = db.recipeDao().getRecipe("52959")!!
        assertTrue(salmon.detailsLoaded)
        assertEquals("Bake.", salmon.instructions)
        assertEquals(1, salmon.ingredients.size)
        assertEquals("https://img/52959-new.jpg", salmon.thumbnail)
        val kedgeree = db.recipeDao().getRecipe("3")!!
        assertEquals(false, kedgeree.detailsLoaded)
    }

    @Test
    fun cachedCategoryIsStillAvailableAfterAnApiError() = runTest {
        api.onFilter = { MealSummariesResponseDto(listOf(mealSummaryDto("1", "Kedgeree"))) }
        repository.recipesInCategory("Seafood").first { it.status == SyncStatus.Synced }

        api.goOffline()
        repository.recipesInCategory("Seafood").test {
            val failed = awaitStatus<SyncStatus.Failed>()
            assertEquals(listOf("Kedgeree"), failed.recipes.map { it.name })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun detailsAreDownloadedOnlyWhenMissing() = runTest {
        api.onFilter = { MealSummariesResponseDto(listOf(mealSummaryDto("1", "Kedgeree"))) }
        repository.recipesInCategory("Seafood").first { it.status == SyncStatus.Synced }
        api.onLookup = { id -> MealsResponseDto(listOf(mealDto(id, "Kedgeree", instructions = "Boil rice."))) }

        // Basic row cached -> lookup.php is called and the row becomes full.
        repository.recipeDetail("1").test {
            val loaded = awaitDetail { it.recipe?.detailsLoaded == true }
            assertEquals("Boil rice.", loaded.recipe!!.instructions)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(listOf("1"), api.lookupCalls)

        // Full row cached -> no second network request.
        repository.recipeDetail("1").test {
            val cached = awaitItem()
            assertEquals(SyncStatus.Synced, cached.status)
            assertTrue(cached.recipe!!.detailsLoaded)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(listOf("1"), api.lookupCalls)
    }

    @Test
    fun offlineDetailShowsBasicInfoWithFailedStatus() = runTest {
        api.onFilter = { MealSummariesResponseDto(listOf(mealSummaryDto("1", "Kedgeree"))) }
        repository.recipesInCategory("Seafood").first { it.status == SyncStatus.Synced }
        api.goOffline()

        repository.recipeDetail("1").test {
            val detail = awaitDetail { it.status is SyncStatus.Failed }
            assertEquals("Kedgeree", detail.recipe!!.name)
            assertEquals(false, detail.recipe!!.detailsLoaded)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun lookupReturningNullMealsIsReportedAsNotFound() = runTest {
        api.onLookup = { MealsResponseDto(meals = null) }

        repository.recipeDetail("404").test {
            val detail = awaitDetail { it.status is SyncStatus.Failed }
            assertEquals(SyncStatus.Failed(DataError.NOT_FOUND), detail.status)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun categoriesAreCachedAndKeptWhenRefreshFails() = runTest {
        api.onCategories = {
            CategoriesResponseDto(listOf(CategoryDto(strCategory = "Beef"), CategoryDto(strCategory = "Chicken")))
        }
        assertEquals(RefreshResult.Success, repository.refreshCategories())

        api.goOffline()
        assertEquals(RefreshResult.Failure(DataError.NO_CONNECTION), repository.refreshCategories())
        assertEquals(listOf("Beef", "Chicken"), repository.observeCategories().first().map { it.name })
    }

    private suspend inline fun <reified T : SyncStatus> ReceiveTurbine<RecipeListing>.awaitStatus(): RecipeListing {
        while (true) {
            val item = awaitItem()
            if (item.status is T) return item
        }
    }

    private suspend fun ReceiveTurbine<RecipeDetail>.awaitDetail(predicate: (RecipeDetail) -> Boolean): RecipeDetail {
        while (true) {
            val item = awaitItem()
            if (predicate(item)) return item
        }
    }
}
