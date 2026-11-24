package com.example.recipefinder.data.repository

import com.example.recipefinder.data.local.dao.RecentlyViewedDao
import com.example.recipefinder.data.mapper.toDomain
import com.example.recipefinder.domain.model.HistoryEntry
import com.example.recipefinder.domain.repository.RecentlyViewedRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalRecentlyViewedRepository(
    private val recentlyViewedDao: RecentlyViewedDao,
    private val clock: () -> Long = System::currentTimeMillis,
) : RecentlyViewedRepository {

    override fun observeRecentlyViewed(): Flow<List<HistoryEntry>> =
        recentlyViewedDao.observeHistory().map { rows -> rows.map { it.toDomain() } }

    override suspend fun recordView(recipeId: String) {
        recentlyViewedDao.recordView(
            recipeId = recipeId,
            viewedAt = clock(),
            maxEntries = RecentlyViewedRepository.MAX_ENTRIES,
        )
    }
}
