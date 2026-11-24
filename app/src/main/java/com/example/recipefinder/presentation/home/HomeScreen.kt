package com.example.recipefinder.presentation.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.recipefinder.R
import com.example.recipefinder.domain.model.DataError
import com.example.recipefinder.presentation.AppViewModelProvider
import com.example.recipefinder.presentation.common.messageRes
import com.example.recipefinder.presentation.components.CategoryRow
import com.example.recipefinder.presentation.components.EmptyState
import com.example.recipefinder.presentation.components.ErrorState
import com.example.recipefinder.presentation.components.LoadingIndicator
import com.example.recipefinder.presentation.components.OfflineBanner
import com.example.recipefinder.presentation.components.RecipeGrid
import com.example.recipefinder.presentation.components.RecipeSearchBar
import com.example.recipefinder.presentation.components.RetryButton
import com.example.recipefinder.presentation.components.ScreenTitleBar

@Composable
fun HomeScreen(
    onRecipeClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreenContent(
        state = uiState,
        query = viewModel.searchQuery,
        onQueryChange = viewModel::onQueryChange,
        onClearQuery = viewModel::onClearQuery,
        onCategorySelected = viewModel::onCategorySelected,
        onRetry = viewModel::retry,
        onRecipeClick = onRecipeClick,
        modifier = modifier,
    )
}

/** Stateless Home UI: everything it shows comes from [state]; every action goes up as a lambda. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    state: HomeUiState,
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onCategorySelected: (String) -> Unit,
    onRetry: () -> Unit,
    onRecipeClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()
    // A new search or category starts at the top of the list.
    LaunchedEffect(state.activeRequest) { gridState.scrollToItem(0) }

    Scaffold(
        modifier = modifier,
        topBar = {
            ScreenTitleBar(
                title = stringResource(R.string.app_name),
                subtitle = stringResource(R.string.home_tagline),
            )
        },
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            OfflineBanner(visible = state.isOffline)
            RecipeSearchBar(
                query = query,
                onQueryChange = onQueryChange,
                onClear = onClearQuery,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (state.categories.isNotEmpty() || state.categoriesLoading) {
                CategoryRow(
                    categories = state.categories,
                    // In search mode no chip is highlighted, which signals that results are not
                    // filtered by category.
                    selectedCategory = state.selectedCategory.takeIf { query.isBlank() },
                    isLoading = state.categoriesLoading,
                    onCategorySelected = onCategorySelected,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }

            PullToRefreshBox(
                isRefreshing = (state.content as? HomeContent.Recipes)?.isRefreshing == true,
                onRefresh = onRetry,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                AnimatedContent(
                    targetState = state.content,
                    // Only animate when the *kind* of content changes, not on every list update.
                    contentKey = { it::class },
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "homeContent",
                ) { content ->
                    HomeListing(
                        content = content,
                        request = state.activeRequest,
                        gridState = gridState,
                        onRetry = onRetry,
                        onRecipeClick = onRecipeClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeListing(
    content: HomeContent,
    request: ListingRequest,
    gridState: LazyGridState,
    onRetry: () -> Unit,
    onRecipeClick: (String) -> Unit,
) {
    when (content) {
        HomeContent.Loading -> LoadingIndicator()

        is HomeContent.Recipes -> RecipeGrid(
            items = content.recipes,
            recipeOf = { it },
            onRecipeClick = onRecipeClick,
            state = gridState,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            header = {
                item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
                    ListingHeader(request = request, count = content.recipes.size, notice = content.notice)
                }
            },
        )

        is HomeContent.NoResults -> EmptyState(
            icon = Icons.Outlined.SearchOff,
            title = stringResource(R.string.empty_search_title),
            message = stringResource(R.string.empty_search_message, content.query),
        )

        is HomeContent.EmptyCategory -> EmptyState(
            icon = Icons.Outlined.RestaurantMenu,
            title = stringResource(R.string.empty_category_title),
            message = stringResource(R.string.empty_category_message),
        )

        is HomeContent.OfflineNoCache -> EmptyState(
            icon = Icons.Outlined.CloudOff,
            title = stringResource(R.string.offline_no_cache_title),
            message = when (val offlineRequest = content.request) {
                is ListingRequest.Search ->
                    stringResource(R.string.offline_no_cache_search_message, offlineRequest.query)
                else -> stringResource(R.string.offline_no_cache_category_message)
            },
            action = { RetryButton(onClick = onRetry) },
        )

        is HomeContent.Error -> ErrorState(
            message = stringResource(content.error.messageRes()),
            onRetry = onRetry,
        )

        is HomeContent.NoCategories -> ErrorState(
            title = stringResource(R.string.no_categories_title),
            message = if (content.error == DataError.NO_CONNECTION) {
                stringResource(R.string.no_categories_message)
            } else {
                stringResource(content.error.messageRes())
            },
            icon = Icons.Outlined.CloudOff,
            onRetry = onRetry,
        )
    }
}

@Composable
private fun ListingHeader(request: ListingRequest, count: Int, notice: CacheNotice?) {
    Column(modifier = Modifier.padding(bottom = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = when (request) {
                    is ListingRequest.Search -> stringResource(R.string.search_results_for, request.query)
                    is ListingRequest.Browse -> stringResource(R.string.category_recipes, request.category)
                    ListingRequest.None -> ""
                },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
            )
            Text(
                text = pluralStringResource(R.plurals.recipe_count, count, count),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (notice != null) {
            Spacer(Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = if (notice == CacheNotice.OFFLINE) Icons.Outlined.CloudOff else Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = stringResource(
                        if (notice == CacheNotice.OFFLINE) R.string.showing_cached_offline
                        else R.string.showing_cached_after_error
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
    }
}
