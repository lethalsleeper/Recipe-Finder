package com.example.recipefinder.di

import android.content.Context
import com.example.recipefinder.data.local.database.RecipeDatabase
import com.example.recipefinder.data.network.AndroidConnectivityObserver
import com.example.recipefinder.data.remote.api.MealApiService
import com.example.recipefinder.data.repository.LocalFavoritesRepository
import com.example.recipefinder.data.repository.LocalRecentlyViewedRepository
import com.example.recipefinder.data.repository.OfflineFirstRecipeRepository
import com.example.recipefinder.domain.connectivity.ConnectivityObserver
import com.example.recipefinder.domain.repository.FavoritesRepository
import com.example.recipefinder.domain.repository.RecentlyViewedRepository
import com.example.recipefinder.domain.repository.RecipeRepository
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Manual dependency injection: one place that builds every long-lived object once.
 *
 * ViewModels receive the repository *interfaces* through their constructors, so switching to
 * Hilt later means annotating these providers rather than rewriting the classes that use them.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    /** Shared by Retrofit and Coil so they reuse the same connection pool. */
    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val mealApi: MealApiService by lazy {
        Retrofit.Builder()
            .baseUrl(MealApiService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MealApiService::class.java)
    }

    private val database: RecipeDatabase by lazy { RecipeDatabase.build(appContext) }

    val recipeRepository: RecipeRepository by lazy {
        OfflineFirstRecipeRepository(
            api = mealApi,
            recipeDao = database.recipeDao(),
            categoryDao = database.categoryDao(),
        )
    }

    val favoritesRepository: FavoritesRepository by lazy {
        LocalFavoritesRepository(database.favoriteDao())
    }

    val recentlyViewedRepository: RecentlyViewedRepository by lazy {
        LocalRecentlyViewedRepository(database.recentlyViewedDao())
    }

    val connectivityObserver: ConnectivityObserver by lazy {
        AndroidConnectivityObserver(appContext)
    }
}
