package com.example.recipefinder.domain.repository

import com.example.recipefinder.domain.model.HistoryEntry
import kotlinx.coroutines.flow.Flow

interface RecentlyViewedRepository {

    /** Recently opened recipes, newest first. */
    fun observeRecentlyViewed(): Flow<List<HistoryEntry>>

    /** Moves [recipeId] to the top of the history, keeping at most [MAX_ENTRIES] entries. */
    suspend fun recordView(recipeId: String)

    companion object {
        const val MAX_ENTRIES = 20
    }
}
