package com.xingheyuzhuan.shiguangschedule.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color

/** Visual slots only. Navigation destinations and all business state remain with the caller. */
@Composable
expect fun UiScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    containerColor: Color = Color.Unspecified,
    contentWindowInsets: WindowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout),
    content: @Composable (PaddingValues) -> Unit,
)

@Composable
expect fun UiTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    onTitleClick: (() -> Unit)? = null,
    large: Boolean = false,
    titleColor: Color = Color.Unspecified,
    containerColor: Color = Color.Unspecified,
    actions: @Composable RowScope.() -> Unit = {},
)

data class UiNavItem(val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

@Composable
expect fun UiNavigationLayout(
    items: List<UiNavItem>, selectedIndex: Int, onSelected: (Int) -> Unit,
    wide: Boolean, showNavigation: Boolean, modifier: Modifier,
    navigationModifier: Modifier, navHideFractionProvider: () -> Float,
    isTransparent: Boolean, content: @Composable (PaddingValues) -> Unit,
)

@Composable
expect fun UiDatePickerDialog(
    initialDateMillis: Long?,
    onDismissRequest: () -> Unit,
    onConfirm: (Long) -> Unit,
    saving: Boolean = false,
)

/** Android-native MIUIX slots used by the shared course and settings pages. */
@Composable
expect fun MiuixCourseViewTabs(dayMode: Boolean, onDayModeChange: (Boolean) -> Unit, modifier: Modifier = Modifier)

@Composable
expect fun MiuixSettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit)

@Composable
expect fun MiuixCourseSnackbar(data: androidx.compose.material3.SnackbarData)

@Composable
expect fun CouiCourseViewTabs(dayMode: Boolean, onDayModeChange: (Boolean) -> Unit, modifier: Modifier = Modifier)

@Composable
expect fun CouiSettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit)

@Composable
expect fun CouiCourseSnackbar(data: androidx.compose.material3.SnackbarData)

@Composable
expect fun UiCourseIconButton(
    icon: ImageVector,
    description: String,
    tint: Color = Color.Unspecified,
    inToolbar: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
)
