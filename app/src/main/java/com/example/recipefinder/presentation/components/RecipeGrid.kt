package com.example.recipefinder.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.recipefinder.domain.model.RecipeSummary

/**
 * Adaptive grid of [RecipeCard]s shared by Home, Favorites and Recently Viewed.
 * Two columns on a typical phone, more on wider screens.
 */
@Composable
fun <T> RecipeGrid(
    items: List<T>,
    recipeOf: (T) -> RecipeSummary,
    onRecipeClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(16.dp),
    supportingText: (T) -> String? = { null },
    header: LazyGridScope.() -> Unit = {},
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        state = state,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        header()
        items(items = items, key = { recipeOf(it).id }) { item ->
            val recipe = recipeOf(item)
            RecipeCard(
                recipe = recipe,
                onClick = { onRecipeClick(recipe.id) },
                supportingText = supportingText(item),
                modifier = Modifier.animateItem(),
            )
        }
    }
}
