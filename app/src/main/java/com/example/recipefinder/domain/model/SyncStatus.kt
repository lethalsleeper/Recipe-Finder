package com.example.recipefinder.domain.model

/** Why talking to TheMealDB failed, in terms the UI can turn into a friendly message. */
enum class DataError {
    NO_CONNECTION,
    TIMEOUT,
    SERVER,
    INVALID_RESPONSE,
    NOT_FOUND,
    UNKNOWN,
}

/**
 * State of the background refresh that accompanies a piece of local data.
 *
 * The data itself always comes from Room; this only tells the UI whether a refresh from the
 * network is still running, finished, or failed.
 */
sealed interface SyncStatus {
    data object Syncing : SyncStatus
    data object Synced : SyncStatus
    data class Failed(val error: DataError) : SyncStatus
}

/** Result of a one-off refresh such as reloading the category list. */
sealed interface RefreshResult {
    data object Success : RefreshResult
    data class Failure(val error: DataError) : RefreshResult
}

/** Recipes read from Room plus the status of the matching network refresh. */
data class RecipeListing(
    val recipes: List<RecipeSummary>,
    val status: SyncStatus,
)

/** A recipe read from Room (null if it has never been stored) plus the details-refresh status. */
data class RecipeDetail(
    val recipe: Recipe?,
    val status: SyncStatus,
)
