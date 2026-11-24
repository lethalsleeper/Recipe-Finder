package com.example.recipefinder.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.recipefinder.data.local.entity.HistoryTuple
import com.example.recipefinder.data.local.entity.RecentlyViewedEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentlyViewedDao {

    @Query(
        """
        SELECT r.idMeal, r.name, r.category, r.thumbnail, h.viewedAt
        FROM recently_viewed h INNER JOIN recipes r ON r.idMeal = h.recipeId
        ORDER BY h.viewedAt DESC
        """
    )
    fun observeHistory(): Flow<List<HistoryTuple>>

    @Query("SELECT * FROM recently_viewed ORDER BY viewedAt DESC")
    suspend fun getAll(): List<RecentlyViewedEntity>

    /** Primary key is the recipe id, so a revisit updates the timestamp instead of duplicating. */
    @Upsert
    suspend fun upsert(entry: RecentlyViewedEntity)

    /** Deletes everything except the [keep] most recent entries. */
    @Query(
        """
        DELETE FROM recently_viewed WHERE recipeId NOT IN (
            SELECT recipeId FROM recently_viewed ORDER BY viewedAt DESC LIMIT :keep
        )
        """
    )
    suspend fun trimTo(keep: Int)

    @Transaction
    suspend fun recordView(recipeId: String, viewedAt: Long, maxEntries: Int) {
        upsert(RecentlyViewedEntity(recipeId = recipeId, viewedAt = viewedAt))
        trimTo(maxEntries)
    }
}
