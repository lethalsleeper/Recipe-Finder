package com.example.recipefinder.presentation.favorites

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.recipefinder.R
import com.example.recipefinder.domain.model.RecipeSummary
import com.example.recipefinder.presentation.AppViewModelProvider
import com.example.recipefinder.presentation.common.CollectionUiState
import com.example.recipefinder.presentation.components.EmptyState
import com.example.recipefinder.presentation.components.LoadingIndicator
import com.example.recipefinder.presentation.components.RecipeGrid
import com.example.recipefinder.presentation.components.ScreenTitleBar

@Composable
fun FavoritesScreen(
    onRecipeClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FavoritesViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    FavoritesScreenContent(state = uiState, onRecipeClick = onRecipeClick, modifier = modifier)
}

@Composable
fun FavoritesScreenContent(
    state: CollectionUiState<RecipeSummary>,
    onRecipeClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            ScreenTitleBar(
                title = stringResource(R.string.favorites_title),
                subtitle = (state as? CollectionUiState.Items)?.let {
                    pluralStringResource(R.plurals.favorites_count, it.items.size, it.items.size)
                },
            )
        },
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)
        when (state) {
            CollectionUiState.Loading -> LoadingIndicator(contentModifier)
            CollectionUiState.Empty -> EmptyState(
                icon = Icons.Outlined.FavoriteBorder,
                title = stringResource(R.string.favorites_empty_title),
                message = stringResource(R.string.favorites_empty_message),
                modifier = contentModifier,
            )
            is CollectionUiState.Items -> RecipeGrid(
                items = state.items,
                recipeOf = { it },
                onRecipeClick = onRecipeClick,
                modifier = contentModifier,
            )
        }
    }
}
