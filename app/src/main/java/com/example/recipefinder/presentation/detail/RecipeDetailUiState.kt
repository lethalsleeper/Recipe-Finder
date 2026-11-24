package com.example.recipefinder.presentation.detail

import com.example.recipefinder.domain.model.DataError
import com.example.recipefinder.domain.model.Recipe
import com.example.recipefinder.domain.model.RecipeDetail
import com.example.recipefinder.domain.model.SyncStatus
import com.example.recipefinder.domain.util.InstructionSteps

sealed interface RecipeDetailUiState {
    /** Nothing stored for this id yet and the download is running. */
    data object Loading : RecipeDetailUiState

    /** Nothing stored for this id and it could not be downloaded. */
    data class Unavailable(val error: DataError) : RecipeDetailUiState

    /** At least the basic information is stored; [details] says whether the rest is available. */
    data class Success(
        val recipe: Recipe,
        val isFavorite: Boolean,
        val instructionSteps: List<String>,
        val details: DetailsState,
    ) : RecipeDetailUiState
}

sealed interface DetailsState {
    data object Loaded : DetailsState
    data object Loading : DetailsState
    /** Only basic info is cached and downloading the rest failed (e.g. offline). */
    data class Missing(val error: DataError) : DetailsState
}

fun RecipeDetail.toUiState(isFavorite: Boolean): RecipeDetailUiState {
    val currentStatus = status
    val storedRecipe = recipe ?: return when (currentStatus) {
        is SyncStatus.Failed -> RecipeDetailUiState.Unavailable(currentStatus.error)
        else -> RecipeDetailUiState.Loading
    }
    return RecipeDetailUiState.Success(
        recipe = storedRecipe,
        isFavorite = isFavorite,
        instructionSteps = InstructionSteps.split(storedRecipe.instructions),
        details = when {
            storedRecipe.detailsLoaded -> DetailsState.Loaded
            currentStatus is SyncStatus.Failed -> DetailsState.Missing(currentStatus.error)
            // Syncing, or synced but Room hasn't re-emitted the updated row yet.
            else -> DetailsState.Loading
        },
    )
}
