package com.example.recipefinder.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * A recipe photo loaded with Coil (memory + disk cache, so it also works offline once cached).
 *
 * A tinted background with a dish icon acts as the placeholder while loading, and stays as the
 * fallback when there is no URL or the image can't be loaded.
 */
@Composable
fun RecipeImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    placeholderIconSize: Dp = 40.dp,
) {
    var showPlaceholder by remember(imageUrl) { mutableStateOf(true) }
    val context = LocalContext.current
    val request = remember(imageUrl) {
        imageUrl?.let { ImageRequest.Builder(context).data(it).crossfade(true).build() }
    }

    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (showPlaceholder) {
            Icon(
                imageVector = Icons.Outlined.RestaurantMenu,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.size(placeholderIconSize),
            )
        }
        if (request != null) {
            AsyncImage(
                model = request,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                onSuccess = { showPlaceholder = false },
                onError = { showPlaceholder = true },
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}
