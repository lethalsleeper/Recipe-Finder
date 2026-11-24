package com.example.recipefinder.data.remote.api

import com.example.recipefinder.domain.model.DataError
import com.google.gson.JsonParseException
import com.google.gson.stream.MalformedJsonException
import kotlinx.coroutines.CancellationException
import retrofit2.Response
import java.io.EOFException
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** Outcome of a single API call, with failures already classified as a [DataError]. */
sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class Error(val error: DataError) : ApiResult<Nothing>
}

/**
 * Runs [call] and converts every failure into an [ApiResult.Error], so a failed request can
 * never crash the app. Coroutine cancellation is re-thrown so structured concurrency still works
 * (e.g. an outdated search is cancelled by `flatMapLatest`).
 */
suspend fun <T : Any> safeApiCall(call: suspend () -> Response<T>): ApiResult<T> {
    return try {
        val response = call()
        val body = response.body()
        when {
            !response.isSuccessful -> ApiResult.Error(httpError(response.code()))
            body == null -> ApiResult.Error(DataError.INVALID_RESPONSE)
            else -> ApiResult.Success(body)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        ApiResult.Error(e.toDataError())
    }
}

private fun httpError(code: Int): DataError = when (code) {
    404 -> DataError.NOT_FOUND
    in 500..599 -> DataError.SERVER
    else -> DataError.INVALID_RESPONSE
}

/** Maps low-level exceptions to user-meaningful categories. Order matters: specific first. */
internal fun Throwable.toDataError(): DataError = when (this) {
    is SocketTimeoutException -> DataError.TIMEOUT
    // Gson reports truncated / malformed JSON with these IOException subclasses.
    is MalformedJsonException, is EOFException -> DataError.INVALID_RESPONSE
    is JsonParseException -> DataError.INVALID_RESPONSE
    is UnknownHostException, is ConnectException, is NoRouteToHostException -> DataError.NO_CONNECTION
    // OkHttp's overall call timeout surfaces as a plain InterruptedIOException("timeout").
    is InterruptedIOException -> DataError.TIMEOUT
    is IOException -> DataError.NO_CONNECTION
    else -> DataError.UNKNOWN
}
