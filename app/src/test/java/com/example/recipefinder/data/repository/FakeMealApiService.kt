package com.example.recipefinder.data.repository

import com.example.recipefinder.data.remote.api.MealApiService
import com.example.recipefinder.data.remote.dto.CategoriesResponseDto
import com.example.recipefinder.data.remote.dto.MealSummariesResponseDto
import com.example.recipefinder.data.remote.dto.MealsResponseDto
import retrofit2.Response
import java.net.UnknownHostException

/**
 * Programmable stand-in for Retrofit. Each endpoint is a lambda so a test can return data,
 * return `meals: null`, or throw (e.g. to simulate being offline).
 */
class FakeMealApiService : MealApiService {

    var onSearch: (String) -> MealsResponseDto = { MealsResponseDto(meals = null) }
    var onCategories: () -> CategoriesResponseDto = { CategoriesResponseDto(categories = null) }
    var onFilter: (String) -> MealSummariesResponseDto = { MealSummariesResponseDto(meals = null) }
    var onLookup: (String) -> MealsResponseDto = { MealsResponseDto(meals = null) }

    val lookupCalls = mutableListOf<String>()

    /** Every endpoint fails the way OkHttp does without a connection. */
    fun goOffline() {
        onSearch = { throw UnknownHostException("offline") }
        onCategories = { throw UnknownHostException("offline") }
        onFilter = { throw UnknownHostException("offline") }
        onLookup = { throw UnknownHostException("offline") }
    }

    override suspend fun searchMeals(query: String): Response<MealsResponseDto> =
        Response.success(onSearch(query))

    override suspend fun getCategories(): Response<CategoriesResponseDto> =
        Response.success(onCategories())

    override suspend fun getMealsByCategory(category: String): Response<MealSummariesResponseDto> =
        Response.success(onFilter(category))

    override suspend fun getMealDetails(id: String): Response<MealsResponseDto> {
        lookupCalls += id
        return Response.success(onLookup(id))
    }
}
