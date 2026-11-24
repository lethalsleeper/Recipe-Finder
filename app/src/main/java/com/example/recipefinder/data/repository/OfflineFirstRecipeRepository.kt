package com.example.recipefinder.data.repository

import android.database.SQLException
import com.example.recipefinder.data.local.dao.CategoryDao
import com.example.recipefinder.data.local.dao.RecipeDao
import com.example.recipefinder.data.local.entity.RecipeSummaryTuple
import com.example.recipefinder.data.mapper.toBasicEntities
import com.example.recipefinder.data.mapper.toDomain
import com.example.recipefinder.data.mapper.toEntities
import com.example.recipefinder.data.mapper.toFullEntities
import com.example.recipefinder.data.remote.api.ApiResult
import com.example.recipefinder.data.remote.api.MealApiService
import com.example.recipefinder.data.remote.api.safeApiCall
import com.example.recipefinder.domain.model.Category
import com.example.recipefinder.domain.model.DataError
import com.example.recipefinder.domain.model.RecipeDetail
import com.example.recipefinder.domain.model.RecipeListing
import com.example.recipefinder.domain.model.RefreshResult
import com.example.recipefinder.domain.model.SyncStatus
import com.example.recipefinder.domain.repository.RecipeRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Offline-first implementation of [RecipeRepository].
 *
 * The pattern used by every public Flow:
 *  1. Start observing Room immediately, so cached data is shown right away (even offline).
 *  2. In parallel, ask TheMealDB for fresh data.
 *  3. Map the response to entities and write them to Room.
 *  4. Room notices the write and emits again; the UI updates by itself.
 *  5. If the request fails, nothing in Room is touched — the cached rows keep flowing and the
 *     emitted [SyncStatus] tells the UI what went wrong.
 *
 * The API response is never handed to the UI directly.
 */
class OfflineFirstRecipeRepository(
    private val api: MealApiService,
    private val recipeDao: RecipeDao,
    private val categoryDao: CategoryDao,
    private val clock: () -> Long = System::currentTimeMillis,
) : RecipeRepository {

    override fun observeCategories(): Flow<List<Category>> =
        categoryDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun refreshCategories(): RefreshResult =
        when (val result = safeApiCall { api.getCategories() }) {
            is ApiResult.Error -> RefreshResult.Failure(result.error)
            is ApiResult.Success -> {
                val categories = result.data.toEntities()
                if (categories.isEmpty()) {
                    // An empty category list is not plausible; keep what we have.
                    RefreshResult.Failure(DataError.INVALID_RESPONSE)
                } else {
                    storeSafely { categoryDao.upsertAll(categories) }
                        ?.let { RefreshResult.Failure(it) }
                        ?: RefreshResult.Success
                }
            }
        }

    override fun recipesInCategory(category: String): Flow<RecipeListing> =
        offlineFirstListing(
            observeLocal = { recipeDao.observeSummariesByCategory(category) },
            fetchAndStore = { fetchCategory(category) },
        )

    override fun searchRecipes(query: String): Flow<RecipeListing> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return flowOf(RecipeListing(emptyList(), SyncStatus.Synced))
        val pattern = likePattern(trimmed)
        return offlineFirstListing(
            observeLocal = { remoteIds -> recipeDao.observeSearch(pattern, remoteIds.toList()) },
            fetchAndStore = { fetchSearch(trimmed) },
        )
    }

    override fun recipeDetail(id: String): Flow<RecipeDetail> = channelFlow {
        val cached = recipeDao.getRecipe(id)
        val status = MutableStateFlow<SyncStatus>(SyncStatus.Synced)

        // Only hit the network when the full recipe is not cached yet.
        if (cached?.detailsLoaded != true) {
            status.value = SyncStatus.Syncing
            launch { status.value = fetchDetails(id) }
        }

        combine(recipeDao.observeRecipe(id), status) { entity, syncStatus ->
            RecipeDetail(recipe = entity?.toDomain(), status = syncStatus)
        }.collect { send(it) }
    }

    // -----------------------------------------------------------------------------------------
    // Network fetches. Each one writes to Room and reports the outcome; none of them return data
    // to the UI directly.
    // -----------------------------------------------------------------------------------------

    private suspend fun fetchCategory(category: String): FetchOutcome =
        when (val result = safeApiCall { api.getMealsByCategory(category) }) {
            is ApiResult.Error -> FetchOutcome.Failed(result.error)
            is ApiResult.Success -> {
                val recipes = result.data.toBasicEntities(category, clock())
                // Partial update: existing full recipes keep their ingredients and instructions.
                storeSafely { recipeDao.upsertBasic(recipes) }
                    ?.let { FetchOutcome.Failed(it) }
                    ?: FetchOutcome.Stored(recipes.mapTo(mutableSetOf()) { it.idMeal })
            }
        }

    private suspend fun fetchSearch(query: String): FetchOutcome =
        when (val result = safeApiCall { api.searchMeals(query) }) {
            is ApiResult.Error -> FetchOutcome.Failed(result.error)
            is ApiResult.Success -> {
                // `meals: null` maps to an empty list. Nothing is deleted: recipes missing from
                // this response simply stay in the cache.
                val recipes = result.data.toFullEntities(clock())
                storeSafely { recipeDao.upsertFull(recipes) }
                    ?.let { FetchOutcome.Failed(it) }
                    ?: FetchOutcome.Stored(recipes.mapTo(mutableSetOf()) { it.idMeal })
            }
        }

    private suspend fun fetchDetails(id: String): SyncStatus =
        when (val result = safeApiCall { api.getMealDetails(id) }) {
            is ApiResult.Error -> SyncStatus.Failed(result.error)
            is ApiResult.Success -> {
                val recipe = result.data.toFullEntities(clock()).firstOrNull { it.idMeal == id }
                if (recipe == null) {
                    SyncStatus.Failed(DataError.NOT_FOUND)
                } else {
                    storeSafely { recipeDao.upsertFull(listOf(recipe)) }
                        ?.let { SyncStatus.Failed(it) }
                        ?: SyncStatus.Synced
                }
            }
        }

    /**
     * Shared offline-first plumbing for recipe lists.
     *
     * The status only becomes [SyncStatus.Synced] once Room has re-run the query *after* the
     * write. Without this, the UI could briefly combine "fetch finished" with the stale (empty)
     * pre-write query result and flash a "no results" message before the results appear.
     * This is done by restarting the Room query (via `flatMapLatest`) when the fetch stores data
     * and tagging each Room emission with the fetch it belongs to.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun offlineFirstListing(
        observeLocal: (remoteIds: Set<String>) -> Flow<List<RecipeSummaryTuple>>,
        fetchAndStore: suspend () -> FetchOutcome,
    ): Flow<RecipeListing> = channelFlow {
        val outcome = MutableStateFlow<FetchOutcome>(FetchOutcome.InProgress)
        launch { outcome.value = fetchAndStore() }

        outcome
            .map { (it as? FetchOutcome.Stored)?.ids ?: NO_IDS }
            .distinctUntilChanged()
            .flatMapLatest { ids -> observeLocal(ids).map { rows -> LocalSnapshot(ids, rows) } }
            .combine(outcome) { local, fetch ->
                RecipeListing(
                    recipes = local.rows.map { it.toDomain() },
                    status = fetch.toSyncStatus(local),
                )
            }
            .collect { send(it) }
    }

    private fun FetchOutcome.toSyncStatus(local: LocalSnapshot): SyncStatus = when (this) {
        FetchOutcome.InProgress -> SyncStatus.Syncing
        is FetchOutcome.Failed -> SyncStatus.Failed(error)
        // Still the pre-write snapshot? Then Room has not caught up yet.
        is FetchOutcome.Stored -> if (local.ids == ids) SyncStatus.Synced else SyncStatus.Syncing
    }

    /** Runs a database write; returns an error instead of throwing if SQLite fails. */
    private suspend fun storeSafely(write: suspend () -> Unit): DataError? = try {
        write()
        null
    } catch (e: SQLException) {
        DataError.UNKNOWN
    }

    private sealed interface FetchOutcome {
        data object InProgress : FetchOutcome
        data class Stored(val ids: Set<String>) : FetchOutcome
        data class Failed(val error: DataError) : FetchOutcome
    }

    private data class LocalSnapshot(val ids: Set<String>, val rows: List<RecipeSummaryTuple>)

    private companion object {
        val NO_IDS = emptySet<String>()

        /** Builds a "contains" LIKE pattern, escaping the user's `%`, `_` and `\` characters. */
        fun likePattern(query: String): String {
            val escaped = query
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_")
            return "%$escaped%"
        }
    }
}
