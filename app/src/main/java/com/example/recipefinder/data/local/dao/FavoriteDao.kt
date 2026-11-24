package com.example.recipefinder.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.recipefinder.data.local.entity.FavoriteEntity
import com.example.recipefinder.data.local.entity.RecipeSummaryTuple
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    /** Joins with `recipes` for display; the favorite rows themselves are independent. */
    @Query(
        """
        SELECT r.idMeal, r.name, r.category, r.thumbnail
        FROM favorites f INNER JOIN recipes r ON r.idMeal = f.recipeId
        ORDER BY f.savedAt DESC
        """
    )
    fun observeFavoriteRecipes(): Flow<List<RecipeSummaryTuple>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE recipeId = :recipeId)")
    fun observeIsFavorite(recipeId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE recipeId = :recipeId)")
    suspend fun isFavorite(recipeId: String): Boolean

    @Query("SELECT * FROM favorites ORDER BY savedAt DESC")
    suspend fun getAll(): List<FavoriteEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE recipeId = :recipeId")
    suspend fun delete(recipeId: String)

    /** Check-then-write inside a transaction so rapid double taps can't race. */
    @Transaction
    suspend fun toggle(recipeId: String, now: Long): Boolean {
        return if (isFavorite(recipeId)) {
            delete(recipeId)
            false
        } else {
            insert(FavoriteEntity(recipeId = recipeId, savedAt = now))
            true
        }
    }
}
