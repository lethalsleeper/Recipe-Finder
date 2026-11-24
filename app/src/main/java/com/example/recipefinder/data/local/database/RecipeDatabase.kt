package com.example.recipefinder.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.recipefinder.data.local.dao.CategoryDao
import com.example.recipefinder.data.local.dao.FavoriteDao
import com.example.recipefinder.data.local.dao.RecentlyViewedDao
import com.example.recipefinder.data.local.dao.RecipeDao
import com.example.recipefinder.data.local.entity.CategoryEntity
import com.example.recipefinder.data.local.entity.FavoriteEntity
import com.example.recipefinder.data.local.entity.RecentlyViewedEntity
import com.example.recipefinder.data.local.entity.RecipeEntity

/** The single source of truth for everything the UI shows. */
@Database(
    entities = [
        RecipeEntity::class,
        FavoriteEntity::class,
        RecentlyViewedEntity::class,
        CategoryEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class RecipeDatabase : RoomDatabase() {

    abstract fun recipeDao(): RecipeDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun recentlyViewedDao(): RecentlyViewedDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        private const val DATABASE_NAME = "recipe_finder.db"

        fun build(context: Context): RecipeDatabase =
            Room.databaseBuilder(context.applicationContext, RecipeDatabase::class.java, DATABASE_NAME)
                .build()
    }
}
