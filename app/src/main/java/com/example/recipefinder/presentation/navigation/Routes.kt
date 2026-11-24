package com.example.recipefinder.presentation.navigation

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.recipefinder.R

object Routes {
    const val HOME = "home"
    const val FAVORITES = "favorites"
    const val RECENTLY_VIEWED = "recently_viewed"

    const val RECIPE_ID_ARG = "recipeId"
    const val RECIPE_DETAIL = "recipe/{$RECIPE_ID_ARG}"

    /** Only the id is passed; the detail screen loads the recipe from Room. */
    fun recipeDetail(recipeId: String): String = "recipe/${Uri.encode(recipeId)}"
}

/** The three bottom-navigation tabs. */
enum class TopLevelDestination(
    val route: String,
    @StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    HOME(Routes.HOME, R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home),
    FAVORITES(Routes.FAVORITES, R.string.nav_favorites, Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder),
    RECENTLY_VIEWED(Routes.RECENTLY_VIEWED, R.string.nav_recent, Icons.Filled.History, Icons.Outlined.History),
}
