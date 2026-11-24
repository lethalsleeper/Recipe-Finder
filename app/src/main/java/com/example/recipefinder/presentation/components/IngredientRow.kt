package com.example.recipefinder.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.recipefinder.domain.model.Ingredient

/**
 * One ingredient line, e.g. "**2 cups** – Flour". If TheMealDB gave no measurement, only the
 * ingredient name is shown (nothing is invented).
 */
@Composable
fun IngredientRow(ingredient: Ingredient, modifier: Modifier = Modifier) {
    val measureColor = MaterialTheme.colorScheme.primary
    Row(
        verticalAlignment = Alignment.Top,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(top = 10.dp)
                .size(7.dp)
                .background(MaterialTheme.colorScheme.secondary, CircleShape),
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = buildAnnotatedString {
                ingredient.measure?.let { measure ->
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = measureColor)) {
                        append(measure)
                    }
                    append(" – ")
                }
                append(ingredient.name)
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
