package com.example.recipefinder.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.recipefinder.data.local.entity.RecipeEntity
import com.example.recipefinder.data.local.entity.RecipeSummaryTuple
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {

    @Query(
        """
        SELECT idMeal, name, category, thumbnail FROM recipes
        WHERE category = :category
        ORDER BY name COLLATE NOCASE
        """
    )
    fun observeSummariesByCategory(category: String): Flow<List<RecipeSummaryTuple>>

    /**
     * Local name search. [pattern] is a LIKE pattern built by the repository (with `\` as the
     * escape character). [extraIds] are the ids the API just returned for this query; including
     * them guarantees API matches are shown even when SQLite's LIKE would not match them, e.g.
     * accented names ("creme" vs "Crème").
     */
    @Query(
        """
        SELECT idMeal, name, category, thumbnail FROM recipes
        WHERE name LIKE :pattern ESCAPE '\' OR idMeal IN (:extraIds)
        ORDER BY name COLLATE NOCASE
        """
    )
    fun observeSearch(pattern: String, extraIds: List<String>): Flow<List<RecipeSummaryTuple>>

    @Query("SELECT * FROM recipes WHERE idMeal = :id")
    fun observeRecipe(id: String): Flow<RecipeEntity?>

    @Query("SELECT * FROM recipes WHERE idMeal = :id")
    suspend fun getRecipe(id: String): RecipeEntity?

    @Query("SELECT * FROM recipes WHERE idMeal IN (:ids)")
    suspend fun getRecipes(ids: List<String>): List<RecipeEntity>

    @Upsert
    suspend fun upsert(recipes: List<RecipeEntity>)

    /** Returns the row id for inserted rows and -1 for rows that already existed. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(recipes: List<RecipeEntity>): List<Long>

    /**
     * Partial update used for basic (category) data: only touches name, thumbnail, category and
     * the fetch time. Area, instructions, ingredients and detailsLoaded are left untouched.
     */
    @Query(
        """
        UPDATE recipes SET
            name = :name,
            thumbnail = COALESCE(:thumbnail, thumbnail),
            category = COALESCE(:category, category),
            lastFetchedAt = :fetchedAt
        WHERE idMeal = :id
        """
    )
    suspend fun updateBasicInfo(
        id: String,
        name: String,
        thumbnail: String?,
        category: String?,
        fetchedAt: Long,
    )

    /**
     * Saves basic recipe rows from `filter.php` without erasing full details.
     *
     * New recipes are inserted as basic rows; recipes that already exist only get their basic
     * columns updated. Runs in one transaction so observers see a single consistent change.
     */
    @Transaction
    suspend fun upsertBasic(recipes: List<RecipeEntity>) {
        if (recipes.isEmpty()) return
        val insertResults = insertIfAbsent(recipes)
        recipes.zip(insertResults)
            .filter { (_, rowId) -> rowId == -1L }
            .forEach { (recipe, _) ->
                updateBasicInfo(
                    id = recipe.idMeal,
                    name = recipe.name,
                    thumbnail = recipe.thumbnail,
                    category = recipe.category,
                    fetchedAt = recipe.lastFetchedAt,
                )
            }
    }

    /**
     * Saves full recipes from `search.php` / `lookup.php`. A field that the new response leaves
     * empty keeps its previously stored value, so a sparse response cannot wipe good data.
     */
    @Transaction
    suspend fun upsertFull(recipes: List<RecipeEntity>) {
        if (recipes.isEmpty()) return
        val existing = getRecipes(recipes.map { it.idMeal }).associateBy { it.idMeal }
        upsert(recipes.map { fresh -> existing[fresh.idMeal]?.let { fresh.mergedOnto(it) } ?: fresh })
    }
}

/** Keeps [old] values wherever this freshly downloaded full recipe has no value. */
internal fun RecipeEntity.mergedOnto(old: RecipeEntity): RecipeEntity = copy(
    category = category ?: old.category,
    thumbnail = thumbnail ?: old.thumbnail,
    area = area ?: old.area,
    instructions = instructions ?: old.instructions,
    ingredients = ingredients.ifEmpty { old.ingredients },
)
