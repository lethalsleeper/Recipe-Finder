package com.example.recipefinder.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.recipefinder.domain.model.Category

/** A selectable category chip with the category's small photo. */
@Composable
fun CategoryChip(
    category: Category,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = category.name, style = MaterialTheme.typography.labelLarge) },
        leadingIcon = {
            AsyncImage(
                model = category.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest),
            )
        },
        shape = CircleShape,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
            selectedBorderColor = Color.Transparent,
        ),
        modifier = modifier.height(40.dp),
    )
}

/**
 * Horizontally scrolling category selector. Shows placeholder pills while the first category
 * list is loading, and scrolls the selected chip into view when it is off-screen.
 */
@Composable
fun CategoryRow(
    categories: List<Category>,
    selectedCategory: String?,
    isLoading: Boolean,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(selectedCategory, categories) {
        val index = categories.indexOfFirst { it.name == selectedCategory }
        if (index < 0) return@LaunchedEffect
        val visible = listState.layoutInfo.visibleItemsInfo
        val fullyVisible = visible.any { it.index == index } &&
            index != visible.firstOrNull()?.index && index != visible.lastOrNull()?.index
        if (!fullyVisible) listState.animateScrollToItem((index - 1).coerceAtLeast(0))
    }

    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        if (categories.isEmpty() && isLoading) {
            items(PLACEHOLDER_COUNT) { CategoryChipPlaceholder() }
        } else {
            items(categories, key = { it.name }) { category ->
                CategoryChip(
                    category = category,
                    selected = category.name == selectedCategory,
                    onClick = { onCategorySelected(category.name) },
                )
            }
        }
    }
}

@Composable
private fun CategoryChipPlaceholder() {
    Box(
        modifier = Modifier
            .width(96.dp)
            .height(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    )
}

private const val PLACEHOLDER_COUNT = 5
