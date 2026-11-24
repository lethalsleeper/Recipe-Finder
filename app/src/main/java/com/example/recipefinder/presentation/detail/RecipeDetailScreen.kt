package com.example.recipefinder.presentation.detail

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.recipefinder.R
import com.example.recipefinder.domain.model.DataError
import com.example.recipefinder.domain.model.Recipe
import com.example.recipefinder.presentation.AppViewModelProvider
import com.example.recipefinder.presentation.common.messageRes
import com.example.recipefinder.presentation.components.EmptyState
import com.example.recipefinder.presentation.components.IngredientRow
import com.example.recipefinder.presentation.components.LoadingIndicator
import com.example.recipefinder.presentation.components.RecipeImage
import com.example.recipefinder.presentation.components.RetryButton

@Composable
fun RecipeDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecipeDetailViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(userMessage) {
        val message = userMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(context.getString(message))
        viewModel.onUserMessageShown()
    }

    // Keep the display on while a recipe is open — handy while cooking. No permission needed.
    KeepScreenOn()

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            when (val state = uiState) {
                RecipeDetailUiState.Loading -> LoadingIndicator()

                is RecipeDetailUiState.Unavailable -> EmptyState(
                    icon = Icons.Outlined.CloudOff,
                    title = stringResource(R.string.recipe_not_available),
                    message = stringResource(state.error.messageRes()),
                    action = { RetryButton(onClick = viewModel::retry) },
                )

                is RecipeDetailUiState.Success -> RecipeDetailContent(
                    state = state,
                    onRetry = viewModel::retry,
                )
            }

            // Floating controls stay reachable however far the recipe is scrolled.
            DetailTopControls(
                onBack = onBack,
                isFavorite = (uiState as? RecipeDetailUiState.Success)?.isFavorite,
                onToggleFavorite = viewModel::toggleFavorite,
            )
        }
    }
}

@Composable
private fun RecipeDetailContent(
    state: RecipeDetailUiState.Success,
    onRetry: () -> Unit,
) {
    val recipe = state.recipe
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomInset + 24.dp),
    ) {
        item(key = "image") {
            HeaderImage(recipe)
        }
        item(key = "title") {
            TitleSheet(recipe)
        }

        when (val details = state.details) {
            DetailsState.Loading -> item(key = "details-loading") { DetailsLoading() }
            is DetailsState.Missing -> item(key = "details-missing") {
                DetailsMissing(error = details.error, onRetry = onRetry)
            }
            DetailsState.Loaded -> {
                item(key = "ingredients-header") {
                    SectionHeader(
                        title = stringResource(R.string.ingredients),
                        trailing = recipe.ingredients.takeIf { it.isNotEmpty() }
                            ?.let { pluralStringResource(R.plurals.ingredients_count, it.size, it.size) },
                    )
                }
                item(key = "ingredients") { IngredientsCard(recipe) }
                item(key = "instructions-header") {
                    SectionHeader(title = stringResource(R.string.instructions))
                }
                instructions(state.instructionSteps)
            }
        }
    }
}

@Composable
private fun HeaderImage(recipe: Recipe) {
    Box {
        RecipeImage(
            imageUrl = recipe.thumbnailUrl,
            contentDescription = stringResource(R.string.recipe_image, recipe.name),
            placeholderIconSize = 72.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp),
        )
        // Soft scrim so the floating buttons stay visible on bright photos.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent))),
        )
    }
}

@Composable
private fun TitleSheet(recipe: Recipe) {
    Surface(
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = MaterialTheme.colorScheme.background,
        // Overlap the photo slightly so the sheet looks like it slides over it.
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = (-28).dp),
    ) {
        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp)) {
            Text(
                text = recipe.name,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoPill(
                    icon = Icons.Outlined.RestaurantMenu,
                    text = recipe.category ?: stringResource(R.string.uncategorized),
                )
                recipe.area?.let { area -> InfoPill(icon = Icons.Outlined.Public, text = area) }
            }
        }
    }
}

@Composable
private fun InfoPill(icon: ImageVector, text: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun SectionHeader(title: String, trailing: String? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun IngredientsCard(recipe: Recipe) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            if (recipe.ingredients.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_ingredients),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
            recipe.ingredients.forEachIndexed { index, ingredient ->
                IngredientRow(ingredient)
                if (index < recipe.ingredients.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                }
            }
        }
    }
}

/** Numbered steps when the text splits reliably into lines, otherwise one readable paragraph. */
private fun LazyListScope.instructions(steps: List<String>) {
    when {
        steps.isEmpty() -> item(key = "no-instructions") {
            Text(
                text = stringResource(R.string.no_instructions),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
        steps.size == 1 -> item(key = "instructions-text") {
            Text(
                text = steps.single(),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
        else -> itemsIndexed(steps, key = { index, _ -> "step-$index" }) { index, step ->
            InstructionStep(number = index + 1, text = step)
        }
    }
}

@Composable
private fun InstructionStep(number: Int, text: String) {
    val stepLabel = stringResource(R.string.step_number, number)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .semantics(mergeDescendants = true) {},
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(34.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                // TalkBack reads "Step 1" instead of a bare "1".
                .clearAndSetSemantics { contentDescription = stepLabel },
        ) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(top = 3.dp),
        )
    }
}

@Composable
private fun DetailsLoading() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
        Spacer(Modifier.width(14.dp))
        Text(
            text = stringResource(R.string.loading_details),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Shown when only basic info is cached and the full recipe couldn't be downloaded. */
@Composable
private fun DetailsMissing(error: DataError, onRetry: () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CloudOff, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.details_not_downloaded_title),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (error == DataError.NO_CONNECTION) {
                    stringResource(R.string.details_not_downloaded_offline)
                } else {
                    stringResource(error.messageRes())
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(16.dp))
            RetryButton(onClick = onRetry)
        }
    }
}

@Composable
private fun DetailTopControls(
    onBack: () -> Unit,
    isFavorite: Boolean?,
    onToggleFavorite: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        CircleControl {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
            }
        }
        Spacer(Modifier.weight(1f))
        if (isFavorite != null) {
            val scale by animateFloatAsState(
                targetValue = if (isFavorite) 1.15f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "favoriteScale",
            )
            CircleControl {
                IconToggleButton(checked = isFavorite, onCheckedChange = { onToggleFavorite() }) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = stringResource(
                            if (isFavorite) R.string.remove_favorite else R.string.add_favorite
                        ),
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.scale(scale),
                    )
                }
            }
        }
    }
}

@Composable
private fun CircleControl(content: @Composable () -> Unit) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 3.dp,
        content = content,
    )
}

@Composable
private fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}
