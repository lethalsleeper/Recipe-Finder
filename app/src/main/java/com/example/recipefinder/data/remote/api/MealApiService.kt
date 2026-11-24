package com.example.recipefinder.data.remote.api

import com.example.recipefinder.data.remote.dto.CategoriesResponseDto
import com.example.recipefinder.data.remote.dto.MealSummariesResponseDto
import com.example.recipefinder.data.remote.dto.MealsResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * TheMealDB endpoints. Functions return [Response] so that HTTP errors and empty bodies can be
 * handled explicitly in [safeApiCall] instead of surfacing as exceptions.
 *
 * Retrofit's suspend functions are main-safe: OkHttp runs the request on its own thread pool.
 */
interface MealApiService {

    @GET("search.php")
    suspend fun searchMeals(@Query("s") query: String): Response<MealsResponseDto>

    @GET("categories.php")
    suspend fun getCategories(): Response<CategoriesResponseDto>

    @GET("filter.php")
    suspend fun getMealsByCategory(@Query("c") category: String): Response<MealSummariesResponseDto>

    @GET("lookup.php")
    suspend fun getMealDetails(@Query("i") id: String): Response<MealsResponseDto>

    companion object {
        const val BASE_URL = "https://www.themealdb.com/api/json/v1/1/"
    }
}
