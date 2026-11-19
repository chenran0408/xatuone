package com.xingheyuzhuan.shiguangschedule.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Shared layout dimensions; typography continues to respect the system font scale. */
object UiMetrics {
    val PageInset = 16.dp
    val SectionGap = 16.dp
    val ContentGap = 8.dp
    val IconSize = 24.dp
    val TouchTarget = 48.dp
    val ContentWidth = 840.dp
}

fun Modifier.uiContentWidth(): Modifier = fillMaxWidth()
    .wrapContentWidth(Alignment.CenterHorizontally)
    .widthIn(max = UiMetrics.ContentWidth)

@Composable
expect fun UiListDivider()

@Composable
expect fun UiLoadingIndicator(modifier: Modifier = Modifier)

enum class UiFeedbackKind { INFO, LOADING, EMPTY, ERROR, SUCCESS }

@Composable
fun UiFeedback(text: String, kind: UiFeedbackKind = UiFeedbackKind.INFO, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(UiMetrics.ContentGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (kind == UiFeedbackKind.LOADING) UiLoadingIndicator(Modifier.size(20.dp))
        UiText(
            text, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
            color = when (kind) {
                UiFeedbackKind.ERROR -> MaterialTheme.colorScheme.error
                UiFeedbackKind.SUCCESS -> uiStyleColors().accent
                else -> uiStyleColors().secondaryText
            },
        )
    }
}
