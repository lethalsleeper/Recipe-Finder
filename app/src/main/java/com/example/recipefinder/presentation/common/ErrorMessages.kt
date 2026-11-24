package com.example.recipefinder.presentation.common

import androidx.annotation.StringRes
import com.example.recipefinder.R
import com.example.recipefinder.domain.model.DataError

/** Friendly text for each error; raw exception messages are never shown to the user. */
@StringRes
fun DataError.messageRes(): Int = when (this) {
    DataError.NO_CONNECTION -> R.string.error_no_connection
    DataError.TIMEOUT -> R.string.error_timeout
    DataError.SERVER -> R.string.error_server
    DataError.INVALID_RESPONSE -> R.string.error_invalid_response
    DataError.NOT_FOUND -> R.string.error_not_found
    DataError.UNKNOWN -> R.string.error_unknown
}
