package com.xingheyuzhuan.shiguangschedule

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.itos.coui.theme.CouiPresets
import com.xingheyuzhuan.shiguangschedule.data.model.UiStyle
import com.xingheyuzhuan.shiguangschedule.ui.components.resolveUiTextStyle
import com.xingheyuzhuan.shiguangschedule.ui.theme.couiColors
import com.xingheyuzhuan.shiguangschedule.ui.theme.materialColorsForStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class UiStyleThemeTest {
    @Test
    fun courseTextKeepsItsRelativeLineHeightWithCustomFontSizes() {
        val body = TextStyle(fontSize = 16.sp, lineHeight = 24.sp)
        for (lineHeight in listOf(1.em, 1.2.em)) {
            val style = TextStyle(lineHeight = lineHeight)
            val customSize = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            val course = resolveUiTextStyle(body, style, customSize)
            assertEquals(13.sp, course.fontSize)
            assertEquals(FontWeight.SemiBold, course.fontWeight)
            assertEquals(lineHeight, course.lineHeight)
            assertEquals(18.sp, resolveUiTextStyle(body, style, customSize.copy(lineHeight = 18.sp)).lineHeight)
        }
    }

    @Test
    fun nativeRoleKeepsItsFontMetricsAndDefaultTextInheritsTheSlotStyle() {
        val slot = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold)
        val nativeFootnote = TextStyle(fontSize = 13.sp)
        val footnote = resolveUiTextStyle(slot, nativeFootnote, TextStyle.Default)
        assertEquals(TextUnit.Unspecified, footnote.lineHeight)
        assertEquals(nativeFootnote, footnote)
        assertEquals(slot, resolveUiTextStyle(slot, TextStyle.Default, TextStyle.Default))
    }

    @Test
    fun customAccentAndErrorSemanticsArePreservedInEveryStyleAndMode() {
        for (dark in listOf(false, true)) {
            val base = (if (dark) darkColorScheme() else lightColorScheme()).copy(
                primary = Color(0xFF147A69), onPrimary = Color.White,
                primaryContainer = Color(0xFFB7EFDD), onPrimaryContainer = Color.Black,
                error = Color(0xFFBA1A1A), onError = Color.White,
            )
            for (style in UiStyle.entries) {
                val adapted = materialColorsForStyle(style, base, dark)
                if (style == UiStyle.MATERIAL) {
                    assertEquals(base.primary, adapted.primary)
                    assertEquals(base.onPrimary, adapted.onPrimary)
                    assertEquals(base.primaryContainer, adapted.primaryContainer)
                    assertEquals(base.onPrimaryContainer, adapted.onPrimaryContainer)
                } else if (style == UiStyle.MIUIX) {
                    assertEquals(if (dark) Color(0xFF78A9FF) else Color(0xFF1F63D5), adapted.primary)
                    assertEquals(if (dark) Color(0xFF1D3B70) else Color(0xFFDCE8FF), adapted.primaryContainer)
                }
                assertEquals(base.error, adapted.error)
                assertEquals(base.onError, adapted.onError)
                assertEquals(base.errorContainer, adapted.errorContainer)
                assertEquals(base.onErrorContainer, adapted.onErrorContainer)
            }
            assertSame(base, materialColorsForStyle(UiStyle.MATERIAL, base, dark))
        }
    }

    @Test
    fun sharedMaterialControlsFollowTheSelectedLibraryNeutralPalette() {
        for (dark in listOf(false, true)) {
            val base = if (dark) darkColorScheme() else lightColorScheme()
            val coui = couiColors(dark)
            val adaptedMiuix = materialColorsForStyle(UiStyle.MIUIX, base, dark)
            val adaptedCoui = materialColorsForStyle(UiStyle.COUI, base, dark)
            assertEquals(if (dark) Color(0xFF11151B) else Color(0xFFF2F4F8), adaptedMiuix.background)
            assertEquals(if (dark) Color(0xFFF2F5FA) else Color(0xFF1D2430), adaptedMiuix.onSurface)
            assertEquals(if (dark) Color(0xFF242A33) else Color.White, adaptedMiuix.surfaceContainer)
            assertEquals(if (dark) Color(0xFFAAB4C2) else Color(0xFF626E80), adaptedMiuix.onSurfaceVariant)
            assertEquals(coui.background, adaptedCoui.background)
            assertEquals(coui.onSurface, adaptedCoui.onSurface)
            assertEquals(coui.container, adaptedCoui.surfaceContainer)
            assertEquals(coui.accent, adaptedCoui.primary)
            assertEquals(Color.Black, adaptedCoui.onPrimary)
            assertEquals(coui.accentOnSurface, adaptedCoui.secondary)
            assertEquals(if (dark) Color.Black else Color.White, adaptedCoui.onSecondary)
            assertTrue(contrastRatio(adaptedCoui.onSurface, adaptedCoui.surface) >= 4.5)
            assertTrue(contrastRatio(adaptedCoui.onPrimary, adaptedCoui.primary) >= 4.5)
            assertTrue(contrastRatio(coui.accentOnSurface, coui.surface) >= 4.5)
            assertTrue(contrastRatio(coui.accentOnSurface, coui.container) >= 4.5)
            assertTrue(contrastRatio(adaptedCoui.onSecondary, adaptedCoui.secondary) >= 4.5)
            assertTrue(contrastRatio(adaptedMiuix.onPrimary, adaptedMiuix.primary) >= 4.5)
            assertTrue(contrastRatio(adaptedMiuix.onSurfaceVariant, adaptedMiuix.surfaceContainer) >= 4.5)
        }
    }

    @Test
    fun couiKeepsAlpha7PaletteExceptForReadableAccentForeground() {
        for (dark in listOf(false, true)) {
            val preset = if (dark) CouiPresets.Dark else CouiPresets.Light
            val actual = couiColors(dark)
            assertEquals(preset.accent, actual.accent)
            assertEquals(preset.surface, actual.surface)
            assertEquals(preset.container, actual.container)
            assertEquals(preset.onSurface, actual.onSurface)
            assertEquals(preset.onSurfaceVariant, actual.onSurfaceVariant)
            assertEquals(preset.background, actual.background)
            assertEquals(Color.Black, actual.onAccent)
            assertEquals(if (dark) preset.accentOnSurface else Color(0xFF005EBB), actual.accentOnSurface)
        }
    }

    private fun contrastRatio(foreground: Color, background: Color): Double {
        val opaqueForeground = foreground.compositeOver(background)
        fun luminance(color: Color): Double {
            val argb = color.toArgb()
            fun channel(shift: Int): Double {
                val value = ((argb shr shift) and 0xFF) / 255.0
                return if (value <= 0.04045) value / 12.92 else Math.pow((value + 0.055) / 1.055, 2.4)
            }
            return .2126 * channel(16) + .7152 * channel(8) + .0722 * channel(0)
        }
        val l1 = luminance(opaqueForeground)
        val l2 = luminance(background)
        return (maxOf(l1, l2) + .05) / (minOf(l1, l2) + .05)
    }
}
