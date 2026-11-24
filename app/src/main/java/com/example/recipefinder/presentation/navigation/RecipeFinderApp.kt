package com.example.recipefinder.presentation.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.recipefinder.presentation.components.BottomNavigationBar
import com.example.recipefinder.presentation.detail.RecipeDetailScreen
import com.example.recipefinder.presentation.favorites.FavoritesScreen
import com.example.recipefinder.presentation.home.HomeScreen
import com.example.recipefinder.presentation.recentlyviewed.RecentlyViewedScreen

/** Root composable: bottom navigation for the three tabs, recipe detail as its own destination. */
@Composable
fun RecipeFinderApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val isTopLevel = TopLevelDestination.entries.any { destination ->
        currentDestination?.hierarchy?.any { it.route == destination.route } == true
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (isTopLevel) {
                BottomNavigationBar(
                    currentDestination = currentDestination,
                    onNavigate = { navController.navigateToTopLevel(it) },
                )
            }
        },
        // Each screen handles the status bar itself (app bar or floating controls).
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.HOME) { entry ->
                HomeScreen(onRecipeClick = { id -> navController.openRecipe(entry, id) })
            }
            composable(Routes.FAVORITES) { entry ->
                FavoritesScreen(onRecipeClick = { id -> navController.openRecipe(entry, id) })
            }
            composable(Routes.RECENTLY_VIEWED) { entry ->
                RecentlyViewedScreen(onRecipeClick = { id -> navController.openRecipe(entry, id) })
            }
            composable(
                route = Routes.RECIPE_DETAIL,
                arguments = listOf(navArgument(Routes.RECIPE_ID_ARG) { type = NavType.StringType }),
            ) { entry ->
                RecipeDetailScreen(
                    onBack = {
                        // Ignore a second tap while the exit animation runs, so we never pop twice.
                        if (entry.isResumed()) navController.popBackStack()
                    },
                )
            }
        }
    }
}

/** Standard tab behaviour: one copy of each tab, and each tab keeps its own state. */
private fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Opens a recipe by id; ignores double taps that would push the same screen twice. */
private fun NavHostController.openRecipe(from: NavBackStackEntry, recipeId: String) {
    if (from.isResumed()) navigate(Routes.recipeDetail(recipeId))
}

private fun NavBackStackEntry.isResumed(): Boolean =
    lifecycle.currentState == Lifecycle.State.RESUMED
