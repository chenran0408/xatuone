package com.xingheyuzhuan.shiguangschedule.ui.theme

import android.app.Activity
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.WindowCompat
import com.xingheyuzhuan.shiguangschedule.data.model.AppThemeMode
import com.xingheyuzhuan.shiguangschedule.data.model.UiStyle
import com.itos.coui.theme.CouiTheme
import com.itos.coui.theme.LocalCouiDarkTheme
import com.itos.coui.theme.CouiPresets
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme
import com.xingheyuzhuan.shiguangschedule.ui.components.LocalUiStyle

@Composable
actual fun rememberColorScheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    customLightPrimary: Color,
    customDarkPrimary: Color
): ColorScheme {
    val context = LocalContext.current
    return when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> {
            val seedColor = if (darkTheme) customDarkPrimary else customLightPrimary
            rememberMaterialKolorScheme(
                darkTheme = darkTheme,
                seedColor = seedColor
            )
        }
    }
}

@Composable
actual fun SetupPlatformThemeEffects(
    colorScheme: ColorScheme,
    darkTheme: Boolean,
    themeMode: AppThemeMode
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            val backgroundColor = colorScheme.background.toArgb()
            window.setBackgroundDrawable(backgroundColor.toDrawable())

            @Suppress("DEPRECATION")
            window.statusBarColor = Color.Transparent.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = Color.Transparent.toArgb()

            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }
}

@get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
actual val supportsDynamicColor: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Composable
actual fun ProvideUiStyle(uiStyle: UiStyle, darkTheme: Boolean, content: @Composable () -> Unit) {
    val base = MaterialTheme.colorScheme
    val miuix = miuixColors(darkTheme).copy(
        error = base.error,
        onError = base.onError,
        errorContainer = base.errorContainer,
        onErrorContainer = base.onErrorContainer,
    )
    val coui = couiColors(darkTheme)
    CompositionLocalProvider(LocalUiStyle provides uiStyle) {
        // Keep the provider tree fixed: a style change must not replace navigation or WebView owners.
        MiuixTheme(colors = miuix) {
            CompositionLocalProvider(
                top.yukonga.miuix.kmp.theme.LocalContentColor provides miuix.onBackground,
                LocalCouiDarkTheme provides darkTheme,
            ) {
                CouiTheme(darkTheme = darkTheme, colorScheme = coui) {
                    MaterialTheme(
                        colorScheme = materialColorsForStyle(uiStyle, base, darkTheme),
                        shapes = shapesForStyle(uiStyle),
                        typography = nativeTypography(uiStyle, MaterialTheme.typography),
                        content = content
                    )
                }
            }
        }
    }
}

/** Existing Material pages remain the same pages, but inherit the selected visual palette. */
internal fun materialColorsForStyle(style: UiStyle, base: ColorScheme, dark: Boolean): ColorScheme = when (style) {
    UiStyle.MATERIAL -> base
    UiStyle.MIUIX -> miuixColors(dark).let { colors ->
        base.copy(
            primary = colors.primary, onPrimary = colors.onPrimary,
            primaryContainer = colors.primaryContainer, onPrimaryContainer = colors.onPrimaryContainer,
            secondary = colors.secondary, onSecondary = colors.onSecondary,
            secondaryContainer = colors.secondaryContainer, onSecondaryContainer = colors.onSecondaryContainer,
            tertiaryContainer = colors.tertiaryContainer, onTertiaryContainer = colors.onTertiaryContainer,
            background = colors.background, onBackground = colors.onBackground,
            surface = colors.surface, onSurface = colors.onSurface,
            surfaceVariant = colors.surfaceVariant, onSurfaceVariant = colors.onSurfaceVariantSummary,
            surfaceContainer = colors.surfaceContainer,
            surfaceContainerLow = colors.surfaceContainer,
            surfaceContainerHigh = colors.surfaceContainerHigh,
            surfaceContainerHighest = colors.surfaceContainerHighest,
            surfaceContainerLowest = colors.background,
            surfaceBright = colors.surface, surfaceDim = colors.background,
            outline = colors.outline, outlineVariant = colors.dividerLine,
        )
    }
    UiStyle.COUI -> couiColors(dark).let { colors ->
        base.copy(
            primary = colors.accent, onPrimary = colors.onAccent,
            primaryContainer = colors.container, onPrimaryContainer = colors.onSurface,
            secondary = colors.accentOnSurface,
            onSecondary = if (dark) Color.Black else Color.White,
            secondaryContainer = colors.container, onSecondaryContainer = colors.onSurface,
            background = colors.background, onBackground = colors.onSurface,
            surface = colors.surface, onSurface = colors.onSurface,
            surfaceVariant = colors.container, onSurfaceVariant = colors.onSurfaceVariant,
            surfaceContainer = colors.container, surfaceContainerLow = colors.card,
            surfaceContainerHigh = colors.container, surfaceContainerHighest = colors.container,
            surfaceContainerLowest = colors.background,
            surfaceBright = colors.surface, surfaceDim = colors.background,
            outline = colors.onSurfaceVariant, outlineVariant = colors.outlineVariant,
        )
    }
}

/** Keep COUI alpha7's own palette; black foreground keeps its blue accent readable on buttons. */
internal fun couiColors(dark: Boolean) =
    (if (dark) CouiPresets.Dark else CouiPresets.Light).copy(
        onAccent = Color.Black,
        accentOnSurface = if (dark) CouiPresets.Dark.accentOnSurface else Color(0xFF005EBB),
    )

private fun miuixColors(dark: Boolean) = (if (dark) darkColorScheme() else lightColorScheme()).copy(
    primary = if (dark) Color(0xFF78A9FF) else Color(0xFF1F63D5),
    onPrimary = if (dark) Color(0xFF0A2D68) else Color.White,
    primaryVariant = if (dark) Color(0xFFA7C5FF) else Color(0xFF1F63D5),
    onPrimaryVariant = if (dark) Color(0xFF08204B) else Color.White,
    disabledPrimary = if (dark) Color(0xFF425A82) else Color(0xFFB9C8E5),
    disabledOnPrimary = if (dark) Color(0xFF9AAED1) else Color(0xFF7E8BA2),
    disabledPrimaryButton = if (dark) Color(0xFF334661) else Color(0xFFC7D3E9),
    disabledOnPrimaryButton = if (dark) Color(0xFF8D9CB5) else Color(0xFF8B96A8),
    disabledPrimarySlider = if (dark) Color(0xFF34445F) else Color(0xFFD6DEEC),
    primaryContainer = if (dark) Color(0xFF1D3B70) else Color(0xFFDCE8FF),
    onPrimaryContainer = if (dark) Color(0xFFD9E6FF) else Color(0xFF123B7A),
    secondary = if (dark) Color(0xFFB7C4D8) else Color(0xFF5B6B84),
    onSecondary = if (dark) Color(0xFF202A38) else Color.White,
    secondaryVariant = if (dark) Color(0xFF8F9DB2) else Color(0xFF71819A),
    onSecondaryVariant = if (dark) Color(0xFF1B2430) else Color.White,
    disabledSecondary = if (dark) Color(0xFF4B5667) else Color(0xFFCBD1DA),
    disabledOnSecondary = if (dark) Color(0xFF8792A3) else Color(0xFF8B93A0),
    disabledSecondaryVariant = if (dark) Color(0xFF3B4554) else Color(0xFFDCE0E6),
    disabledOnSecondaryVariant = if (dark) Color(0xFF7B8798) else Color(0xFF9AA2AF),
    secondaryContainer = if (dark) Color(0xFF2A3444) else Color(0xFFE8EDF5),
    onSecondaryContainer = if (dark) Color(0xFFD5DEEB) else Color(0xFF253449),
    secondaryContainerVariant = if (dark) Color(0xFF222B38) else Color(0xFFF0F3F8),
    onSecondaryContainerVariant = if (dark) Color(0xFFB7C2D1) else Color(0xFF34445B),
    tertiaryContainer = if (dark) Color(0xFF29323D) else Color(0xFFE9EEF5),
    onTertiaryContainer = if (dark) Color(0xFFD9E1EA) else Color(0xFF2B3645),
    tertiaryContainerVariant = if (dark) Color(0xFF222A34) else Color(0xFFF3F5F8),
    background = if (dark) Color(0xFF11151B) else Color(0xFFF2F4F8),
    onBackground = if (dark) Color(0xFFF2F5FA) else Color(0xFF1D2430),
    onBackgroundVariant = if (dark) Color(0xFFAAB4C2) else Color(0xFF626E80),
    surface = if (dark) Color(0xFF1B2028) else Color.White,
    onSurface = if (dark) Color(0xFFF2F5FA) else Color(0xFF1D2430),
    surfaceVariant = if (dark) Color(0xFF242A33) else Color(0xFFEFF2F6),
    onSurfaceSecondary = if (dark) Color(0xFFC0C9D6) else Color(0xFF4A5566),
    onSurfaceVariantSummary = if (dark) Color(0xFFAAB4C2) else Color(0xFF626E80),
    onSurfaceVariantActions = if (dark) Color(0xFF78A9FF) else Color(0xFF1F63D5),
    disabledOnSurface = if (dark) Color(0xFF707B8A) else Color(0xFFA0A8B5),
    outline = if (dark) Color(0xFF4B5665) else Color(0xFFCBD2DC),
    dividerLine = if (dark) Color(0xFF303741) else Color(0xFFE4E8EF),
    surfaceContainer = if (dark) Color(0xFF242A33) else Color.White,
    onSurfaceContainer = if (dark) Color(0xFFE6EBF2) else Color(0xFF253449),
    onSurfaceContainerVariant = if (dark) Color(0xFFB6C0CE) else Color(0xFF4B596B),
    surfaceContainerHigh = if (dark) Color(0xFF2B323C) else Color(0xFFE7ECF3),
    onSurfaceContainerHigh = if (dark) Color(0xFFF0F3F7) else Color(0xFF1D2430),
    surfaceContainerHighest = if (dark) Color(0xFF343C47) else Color(0xFFDDE4ED),
    onSurfaceContainerHighest = if (dark) Color(0xFFF2F5FA) else Color(0xFF1D2430),
    windowDimming = Color(0x99000000),
)

/** Bridge legacy text roles to library metrics; native controls retain their own typography. */
@Composable
private fun nativeTypography(style: UiStyle, base: androidx.compose.material3.Typography): androidx.compose.material3.Typography {
    if (style == UiStyle.MATERIAL) return base
    if (style == UiStyle.MIUIX) {
        val t = MiuixTheme.textStyles
        return base.copy(displayLarge = t.title1, displayMedium = t.title1, displaySmall = t.title2,
            headlineLarge = t.title2, headlineMedium = t.title3, headlineSmall = t.title4,
            titleLarge = t.title4, titleMedium = t.headline1, titleSmall = t.headline2,
            bodyLarge = t.body1, bodyMedium = t.body2, bodySmall = t.footnote1,
            labelLarge = t.button, labelMedium = t.footnote1, labelSmall = t.footnote2)
    }
    val home = com.itos.coui.text.rememberCouiTextStyle(com.itos.coui.text.CouiTextKind.HomeTitle)
    val page = com.itos.coui.text.rememberCouiTextStyle(com.itos.coui.text.CouiTextKind.PageTitle)
    val section = com.itos.coui.text.rememberCouiTextStyle(com.itos.coui.text.CouiTextKind.SectionTitle)
    val title = com.itos.coui.text.rememberCouiTextStyle(com.itos.coui.text.CouiTextKind.PreferenceTitle)
    val body = com.itos.coui.text.rememberCouiTextStyle(com.itos.coui.text.CouiTextKind.Body)
    val summary = com.itos.coui.text.rememberCouiTextStyle(com.itos.coui.text.CouiTextKind.PreferenceSummary)
    val value = com.itos.coui.text.rememberCouiTextStyle(com.itos.coui.text.CouiTextKind.PreferenceValue)
    val group = com.itos.coui.text.rememberCouiTextStyle(com.itos.coui.text.CouiTextKind.GroupTitle)
    return base.copy(displayLarge = home, displayMedium = home, displaySmall = page,
        headlineLarge = page, headlineMedium = page, headlineSmall = page,
        titleLarge = section, titleMedium = section, titleSmall = title,
        bodyLarge = body, bodyMedium = body, bodySmall = summary,
        labelLarge = value, labelMedium = value, labelSmall = group)
}
