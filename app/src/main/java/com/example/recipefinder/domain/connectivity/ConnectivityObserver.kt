package com.example.recipefinder.domain.connectivity

import kotlinx.coroutines.flow.Flow

/** Reports whether the device currently has a network connection that can reach the internet. */
interface ConnectivityObserver {
    val isOnline: Flow<Boolean>
}
