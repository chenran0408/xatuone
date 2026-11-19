package com.xingheyuzhuan.shiguangschedule.ui.components

import android.graphics.Typeface
import com.xingheyuzhuan.shiguangschedule.data.model.UiStyle

import shiguangschedule.shared.generated.resources.chevron_right_24px
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.LoadedFontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.itos.coui.text.CouiTextKind
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.CardDefaults as MiuixCardDefaults
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.itos.coui.theme.CouiTheme

@Composable
actual fun UiText(
    text: String,
    modifier: Modifier,
    style: TextStyle,
    color: Color,
    fontSize: TextUnit,
    fontWeight: FontWeight?,
    textAlign: TextAlign?,
    textDecoration: TextDecoration?,
    lineHeight: TextUnit,
    maxLines: Int,
    overflow: TextOverflow,
) {
    val ambient = LocalTextStyle.current
    val textStyle = resolveUiTextStyle(
        ambient, style,
        TextStyle(
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            textAlign = textAlign ?: TextAlign.Unspecified,
            textDecoration = textDecoration,
            lineHeight = lineHeight,
        ),
    )
    val uiStyle = LocalUiStyle.current
    val resolvedColor = when {
        textStyle.color != Color.Unspecified -> textStyle.color
        ambient.color != Color.Unspecified -> ambient.color
        else -> when (uiStyle) {
            UiStyle.MATERIAL -> androidx.compose.material3.LocalContentColor.current
            UiStyle.MIUIX -> top.yukonga.miuix.kmp.theme.LocalContentColor.current
            UiStyle.COUI -> uiStyleColors().onSurface
        }
    }
    when (uiStyle) {
        UiStyle.MATERIAL -> Text(text, modifier, style = textStyle, color = resolvedColor, maxLines = maxLines, overflow = overflow)
        // The application theme already maps text roles to MiuixTheme.textStyles.
        UiStyle.MIUIX -> MiuixText(text, modifier, style = textStyle, color = resolvedColor, maxLines = maxLines, overflow = overflow)
        UiStyle.COUI -> {
            val typography = MaterialTheme.typography
            val roleStyle = if (style == TextStyle.Default) ambient else style
            val kind = when (roleStyle) {
                typography.displayLarge, typography.displayMedium -> CouiTextKind.HomeTitle
                typography.displaySmall, typography.headlineLarge,
                typography.headlineMedium, typography.headlineSmall -> CouiTextKind.PageTitle
                typography.titleLarge, typography.titleMedium -> CouiTextKind.SectionTitle
                typography.titleSmall -> CouiTextKind.PreferenceTitle
                typography.bodySmall -> CouiTextKind.PreferenceSummary
                typography.labelSmall -> CouiTextKind.GroupTitle
                typography.labelLarge, typography.labelMedium -> CouiTextKind.PreferenceValue
                typography.bodyLarge, typography.bodyMedium -> CouiTextKind.Body
                else -> null
            }
            if (kind != null && fontSize == TextUnit.Unspecified && fontWeight == null &&
                lineHeight == TextUnit.Unspecified && textDecoration == null
            ) {
                com.itos.coui.text.Text(
                    text = text, modifier = modifier, kind = kind, color = resolvedColor,
                    textAlign = textAlign ?: TextAlign.Start, maxLines = maxLines, overflow = overflow,
                )
            } else {
                BasicText(
                    text, modifier, style = textStyle.withCouiFontWeight().copy(color = resolvedColor),
                    maxLines = maxLines, overflow = overflow,
                )
            }
        }
    }
}

internal fun resolveUiTextStyle(ambient: TextStyle, style: TextStyle, overrides: TextStyle): TextStyle {
    // A role supplies its own metrics. MIUIX leaves line height to its font for most roles.
    val base = if (style.fontSize == TextUnit.Unspecified) ambient.merge(style) else style
    val needsSizedLineHeight = overrides.fontSize != TextUnit.Unspecified &&
        overrides.lineHeight == TextUnit.Unspecified && style.lineHeight == TextUnit.Unspecified
    return base.merge(
        if (needsSizedLineHeight) overrides.copy(lineHeight = overrides.fontSize * 1.4f) else overrides
    )
}

@Composable
private fun TextStyle.withCouiFontWeight(): TextStyle {
    val family = fontFamily as? LoadedFontFamily ?: return this
    val requestedWeight = fontWeight ?: return this
    // COUI embeds a single Android Typeface. Its Compose wrapper always returns that same face.
    val face = LocalFontFamilyResolver.current.resolve(family, requestedWeight).value as Typeface
    if (face.weight == requestedWeight.weight) return this
    val weightedFamily = remember(face, requestedWeight) {
        FontFamily(Typeface.create(face, requestedWeight.weight, face.isItalic))
    }
    return copy(fontFamily = weightedFamily)
}

@Composable
actual fun UiCard(modifier: Modifier, containerColor: Color, content: @Composable ColumnScope.() -> Unit) {
    // Move the same content subtree between native shells without resetting its state.
    val currentContent by rememberUpdatedState(content)
    val movableContent = remember {
        movableContentOf<ColumnScope> { scope -> currentContent(scope) }
    }
    val retainedContent: @Composable ColumnScope.() -> Unit = { movableContent(this) }
    when (LocalUiStyle.current) {
        UiStyle.MATERIAL -> Card(
            modifier, shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = if (containerColor == Color.Unspecified) MaterialTheme.colorScheme.surfaceContainerLow else containerColor), content = retainedContent
        )
        UiStyle.MIUIX -> MiuixCard(
            modifier, colors = MiuixCardDefaults.defaultColors(
                color = if (containerColor == Color.Unspecified) MiuixTheme.colorScheme.surfaceContainer else containerColor
            ), content = retainedContent
        )
        UiStyle.COUI -> com.itos.coui.layout.Group(
            modifier = modifier,
            colors = com.itos.coui.layout.CouiPageDefaults.themeColors().let {
                if (containerColor == Color.Unspecified) it else it.copy(card = containerColor)
            }, content = retainedContent
        )
    }
}

@Composable
actual fun uiStyleColors(): UiStyleColors = when (LocalUiStyle.current) {
    UiStyle.MATERIAL -> MaterialTheme.colorScheme.let {
        UiStyleColors(it.background, it.surface, it.onSurface, it.onSurfaceVariant, it.primary)
    }
    UiStyle.MIUIX -> MiuixTheme.colorScheme.let {
        UiStyleColors(it.background, it.surfaceContainer, it.onSurface, it.onSurfaceVariantSummary, it.primary)
    }
    UiStyle.COUI -> CouiTheme.colorScheme.let {
        UiStyleColors(it.background, it.card, it.onSurface, it.onSurfaceVariant, it.accentOnSurface)
    }
}

@Composable
actual fun UiButton(text: String, onClick: () -> Unit, modifier: Modifier, minHeight: Dp, selected: Boolean, enabled: Boolean, contentColor: Color) {
    val labelColor = (if (contentColor == Color.Unspecified) uiStyleColors().accent else contentColor)
        .let { if (enabled) it else it.copy(alpha = .38f) }
    when (LocalUiStyle.current) {
        UiStyle.MATERIAL -> TextButton(
            onClick,
            modifier.defaultMinSize(minHeight = minHeight),
            enabled = enabled
        ) { UiText(text, style = MaterialTheme.typography.labelLarge, color = labelColor) }
        UiStyle.MIUIX -> top.yukonga.miuix.kmp.basic.TextButton(
            text = text,
            onClick = onClick,
            modifier = modifier.defaultMinSize(minHeight = minHeight),
            enabled = enabled,
            colors = top.yukonga.miuix.kmp.basic.ButtonDefaults.textButtonColors(
                color = if (selected) MiuixTheme.colorScheme.primary.copy(alpha = .14f) else Color.Transparent,
                disabledColor = Color.Transparent,
                textColor = labelColor,
                disabledTextColor = labelColor,
            ),
        )
        UiStyle.COUI -> CouiAction(text, onClick, modifier, enabled, filled = selected,
            minHeight = minHeight, contentColor = contentColor)
    }
}

@Composable
actual fun UiServiceTile(title: String, summary: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier) {
    UiCard(modifier.heightIn(min = 88.dp).clickable(role = Role.Button, onClick = onClick)) {
        Column(Modifier.fillMaxWidth().padding(UiMetrics.PageInset), verticalArrangement = Arrangement.spacedBy(UiMetrics.ContentGap)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(UiMetrics.IconSize), tint = uiStyleColors().accent)
            UiText(title, style = MaterialTheme.typography.titleSmall)
            UiText(summary, style = MaterialTheme.typography.bodySmall, color = uiStyleColors().secondaryText)
        }
    }
}

@Composable
actual fun UiServiceRow(title: String, summary: String, onClick: () -> Unit, modifier: Modifier) {
    UiSettingRow(title = title, summary = summary, onClick = onClick, modifier = modifier,
        trailingContent = { Icon(org.jetbrains.compose.resources.vectorResource(shiguangschedule.shared.generated.resources.Res.drawable.chevron_right_24px),
            contentDescription = null, modifier = Modifier.size(24.dp), tint = uiStyleColors().secondaryText) })
}
