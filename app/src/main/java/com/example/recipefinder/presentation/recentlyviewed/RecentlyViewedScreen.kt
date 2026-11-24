package com.example.recipefinder.presentation.recentlyviewed

import android.text.format.DateUtils
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.recipefinder.R
import com.example.recipefinder.domain.model.HistoryEntry
import com.example.recipefinder.domain.repository.RecentlyViewedRepository
import com.example.recipefinder.presentation.AppViewModelProvider
import com.example.recipefinder.presentation.common.CollectionUiState
import com.example.recipefinder.presentation.components.EmptyState
import com.example.recipefinder.presentation.components.LoadingIndicator
import com.example.recipefinder.presentation.components.RecipeGrid
import com.example.recipefinder.presentation.components.ScreenTitleBar

@Composable
fun RecentlyViewedScreen(
    onRecipeClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecentlyViewedViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    RecentlyViewedScreenContent(state = uiState, onRecipeClick = onRecipeClick, modifier = modifier)
}

@Composable
fun RecentlyViewedScreenContent(
    state: CollectionUiState<HistoryEntry>,
    onRecipeClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            ScreenTitleBar(
                title = stringResource(R.string.recent_title),
                subtitle = pluralStringResource(
                    R.plurals.recent_subtitle,
                    RecentlyViewedRepository.MAX_ENTRIES,
                    RecentlyViewedRepository.MAX_ENTRIES,
                ),
            )
        },
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)
        when (state) {
            CollectionUiState.Loading -> LoadingIndicator(contentModifier)
            CollectionUiState.Empty -> EmptyState(
                icon = Icons.Outlined.History,
                title = stringResource(R.string.recent_empty_title),
                message = stringResource(R.string.recent_empty_message),
                modifier = contentModifier,
            )
            is CollectionUiState.Items -> {
                // Captured once per list update; "5 minutes ago" style labels.
                val now = remember(state.items) { System.currentTimeMillis() }
                RecipeGrid(
                    items = state.items,
                    recipeOf = { it.recipe },
                    onRecipeClick = onRecipeClick,
                    supportingText = { entry ->
                        DateUtils.getRelativeTimeSpanString(
                            entry.viewedAt,
                            now,
                            DateUtils.MINUTE_IN_MILLIS,
                        ).toString()
                    },
                    modifier = contentModifier,
                )
            }
        }
    }
}
