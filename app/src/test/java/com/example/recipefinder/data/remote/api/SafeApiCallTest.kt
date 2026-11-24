package com.example.recipefinder.data.remote.api

import com.example.recipefinder.data.remote.dto.MealsResponseDto
import com.example.recipefinder.domain.model.DataError
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response
import java.io.EOFException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class SafeApiCallTest {

    @Test
    fun `successful response with body is a success`() = runTest {
        val body = MealsResponseDto(meals = null)
        val result = safeApiCall { Response.success(body) }
        assertEquals(ApiResult.Success(body), result)
    }

    @Test
    fun `empty body is an invalid response`() = runTest {
        val result = safeApiCall<MealsResponseDto> { Response.success(null) }
        assertEquals(ApiResult.Error(DataError.INVALID_RESPONSE), result)
    }

    @Test
    fun `server errors are classified`() = runTest {
        val result = safeApiCall<MealsResponseDto> { Response.error(503, "".toResponseBody()) }
        assertEquals(ApiResult.Error(DataError.SERVER), result)
    }

    @Test
    fun `no internet is classified`() = runTest {
        val result = safeApiCall<MealsResponseDto> { throw UnknownHostException("www.themealdb.com") }
        assertEquals(ApiResult.Error(DataError.NO_CONNECTION), result)
    }

    @Test
    fun `timeouts are classified`() = runTest {
        val result = safeApiCall<MealsResponseDto> { throw SocketTimeoutException() }
        assertEquals(ApiResult.Error(DataError.TIMEOUT), result)
    }

    @Test
    fun `json parsing failures are classified as invalid response`() = runTest {
        assertEquals(
            ApiResult.Error(DataError.INVALID_RESPONSE),
            safeApiCall<MealsResponseDto> { throw JsonSyntaxException("Expected BEGIN_OBJECT") },
        )
        assertEquals(
            ApiResult.Error(DataError.INVALID_RESPONSE),
            safeApiCall<MealsResponseDto> { throw EOFException("End of input") },
        )
    }

    @Test
    fun `other IO failures count as connection problems`() = runTest {
        val result = safeApiCall<MealsResponseDto> { throw IOException("reset") }
        assertEquals(ApiResult.Error(DataError.NO_CONNECTION), result)
    }

    @Test
    fun `unexpected exceptions do not escape`() = runTest {
        val result = safeApiCall<MealsResponseDto> { throw IllegalStateException("boom") }
        assertEquals(ApiResult.Error(DataError.UNKNOWN), result)
    }
}
