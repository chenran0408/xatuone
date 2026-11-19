package com.xingheyuzhuan.shiguangschedule.ui.components

import com.itos.coui.backdrop.couiBackdrop
import com.itos.coui.backdrop.ProvideCouiBackdrop
import com.itos.coui.backdrop.rememberCouiBackdrop
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.itos.coui.effect.couiButtonFeedback
import com.itos.coui.shape.CouiCapsuleShape
import com.xingheyuzhuan.shiguangschedule.data.model.UiStyle
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import shiguangschedule.shared.generated.resources.*
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar as MiuixFloatingNavigationBar
import top.yukonga.miuix.kmp.basic.FloatingNavigationBarItem as MiuixFloatingNavigationBarItem

@OptIn(ExperimentalMaterial3Api::class)
private val LocalMaterialScroll = staticCompositionLocalOf<TopAppBarScrollBehavior?> { null }

private val LocalMiuixScroll = staticCompositionLocalOf<top.yukonga.miuix.kmp.basic.ScrollBehavior?> { null }
private val LocalCouiScroll = staticCompositionLocalOf<com.itos.coui.topappbar.CouiTopAppBarScrollBehavior?> { null }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun UiScaffold(modifier: Modifier, topBar: @Composable () -> Unit, snackbarHost: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit, floatingActionButton: @Composable () -> Unit, containerColor: Color,
    contentWindowInsets: WindowInsets, content: @Composable (PaddingValues) -> Unit) {
    val materialScroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState(initialHeightOffsetLimit = 0f))
    val backdrop = rememberCouiBackdrop()
    val miuixScroll = top.yukonga.miuix.kmp.basic.MiuixScrollBehavior(
        state = top.yukonga.miuix.kmp.basic.rememberTopAppBarState(initialHeightOffsetLimit = 0f))
    val couiScroll = com.itos.coui.topappbar.CouiTopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollModifier = when (LocalUiStyle.current) {
        UiStyle.MATERIAL -> Modifier.nestedScroll(materialScroll.nestedScrollConnection)
        UiStyle.MIUIX -> Modifier.nestedScroll(miuixScroll.nestedScrollConnection)
        UiStyle.COUI -> Modifier.nestedScroll(couiScroll.nestedScrollConnection)
    }
    CompositionLocalProvider(LocalMaterialScroll provides materialScroll, LocalMiuixScroll provides miuixScroll, LocalCouiScroll provides couiScroll) {
        // Keep the composition owner fixed: moving nested native Scaffolds between
        // subcompositions corrupts movable-content slots in the current runtime.
        // This layout only positions slots; each slot uses the selected library's UI.
        Scaffold(modifier = modifier.then(scrollModifier), topBar = { ProvideCouiBackdrop(backdrop, content = topBar) }, snackbarHost = snackbarHost,
            contentWindowInsets = contentWindowInsets, bottomBar = bottomBar, floatingActionButton = floatingActionButton,
            containerColor = if (containerColor == Color.Unspecified) uiStyleColors().background else containerColor,
            content = { padding ->
                Box(if (LocalUiStyle.current == UiStyle.COUI) Modifier.couiBackdrop(backdrop) else Modifier) { content(padding) }
            })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun UiTopAppBar(title: String, modifier: Modifier, onBack: (() -> Unit)?, navigationIcon: (@Composable () -> Unit)?, onTitleClick: (() -> Unit)?,
    large: Boolean, titleColor: Color, containerColor: Color, actions: @Composable RowScope.() -> Unit) {
    val expanded = large && LocalWindowInfo.current.containerSize.height >= with(LocalDensity.current) { 480.dp.roundToPx() }
    val back: @Composable () -> Unit = {
        if (navigationIcon != null) navigationIcon()
        else if (onBack != null) when (LocalUiStyle.current) {
            UiStyle.MATERIAL -> IconButton(onClick = onBack) { BackIcon() }
            UiStyle.MIUIX -> top.yukonga.miuix.kmp.basic.IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) { BackIcon() }
            UiStyle.COUI -> com.itos.coui.topappbar.TopAppBarButton(onClick = onBack) { BackIcon() }
        }
    }
    val miuixActions: @Composable RowScope.() -> Unit = {
        if (onTitleClick != null) top.yukonga.miuix.kmp.basic.IconButton(onClick = onTitleClick, modifier = Modifier.size(48.dp)) {
            Icon(vectorResource(Res.drawable.arrow_drop_down_24px), "选择周次")
        }
        actions()
    }
    when (LocalUiStyle.current) {
        UiStyle.MATERIAL -> {
            val colors = TopAppBarDefaults.topAppBarColors(
                containerColor = if (containerColor == Color.Unspecified) MaterialTheme.colorScheme.surface else containerColor,
                titleContentColor = if (titleColor == Color.Unspecified) MaterialTheme.colorScheme.onSurface else titleColor)
            val heading: @Composable () -> Unit = {
                Text(title, Modifier.then(if (onTitleClick != null) Modifier.clickable(onClick = onTitleClick) else Modifier))
            }
            if (expanded) LargeTopAppBar(modifier = modifier, title = heading, navigationIcon = back,
                actions = actions, colors = colors, scrollBehavior = LocalMaterialScroll.current)
            else TopAppBar(modifier = modifier, title = heading, navigationIcon = back, actions = actions, colors = colors)
        }
        UiStyle.MIUIX -> if (expanded) top.yukonga.miuix.kmp.basic.TopAppBar(
            title = title, modifier = modifier, navigationIcon = back, actions = miuixActions, scrollBehavior = LocalMiuixScroll.current,
            color = if (containerColor == Color.Unspecified) top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.background else containerColor,
            titleColor = if (titleColor == Color.Unspecified) top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.onSurface else titleColor)
        else top.yukonga.miuix.kmp.basic.SmallTopAppBar(title = title, modifier = modifier, navigationIcon = back, actions = miuixActions,
            color = if (containerColor == Color.Unspecified) top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.surface else containerColor,
            titleColor = if (titleColor == Color.Unspecified) top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.onSurface else titleColor)
        UiStyle.COUI -> if (expanded) com.itos.coui.topappbar.LargeTopAppBar(modifier = modifier, scrollBehavior = LocalCouiScroll.current,
            colors = com.itos.coui.topappbar.CouiTopAppBarDefaults.colors(backgroundColor = containerColor, titleColor = titleColor),
            title = { com.itos.coui.text.Text(text = title, kind = com.itos.coui.text.CouiTextKind.HomeTitle,
                textAlign = androidx.compose.ui.text.style.TextAlign.Start) }, navigationIcon = back, actions = actions)
        else com.itos.coui.topappbar.TopAppBar(modifier = modifier,
            colors = com.itos.coui.topappbar.CouiTopAppBarDefaults.colors(backgroundColor = containerColor, titleColor = titleColor),
            title = { com.itos.coui.text.Text(text = title, kind = com.itos.coui.text.CouiTextKind.Toolbar, color = titleColor,
                modifier = Modifier.then(if (onTitleClick != null) Modifier.clickable(onClick = onTitleClick) else Modifier)) },
            navigationIcon = back, actions = actions)
    }
}

@Composable
private fun BackIcon() { Icon(vectorResource(Res.drawable.arrow_back_24px), stringResource(Res.string.a11y_back)) }

@Composable
actual fun UiNavigationLayout(items: List<UiNavItem>, selectedIndex: Int, onSelected: (Int) -> Unit,
    wide: Boolean, showNavigation: Boolean, modifier: Modifier, navigationModifier: Modifier,
    navHideFractionProvider: () -> Float, isTransparent: Boolean, content: @Composable (PaddingValues) -> Unit) {
    val style = LocalUiStyle.current
    val backdrop = rememberCouiBackdrop()
    val bar: @Composable () -> Unit = {
        if (showNavigation && !wide) Box(navigationModifier.graphicsLayer {
            val fraction = navHideFractionProvider()
            translationY = size.height * fraction
            alpha = 1f - fraction
        }.windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = if (style == UiStyle.MATERIAL) 16.dp else 0.dp, vertical = if (style == UiStyle.COUI) 0.dp else 8.dp)) {
            when (style) {
                UiStyle.MATERIAL -> NavigationBar(
                    modifier = Modifier.clip(RoundedCornerShape(28.dp)).shadow(8.dp, RoundedCornerShape(28.dp)),
                    containerColor = if (isTransparent) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = .94f),
                    windowInsets = WindowInsets(0, 0, 0, 0),
                ) {
                    items.forEachIndexed { index, item -> NavigationBarItem(selected = index == selectedIndex,
                        onClick = { if (index != selectedIndex) onSelected(index) },
                        icon = { Icon(if (index == selectedIndex) item.selectedIcon else item.icon, null) }, label = { Text(item.label) }) }
                }
                UiStyle.MIUIX -> MiuixFloatingNavigationBar(modifier = Modifier.graphicsLayer { alpha = .96f }) {
                    val itemColors = top.yukonga.miuix.kmp.basic.NavigationBarDefaults.navigationBarItemColors(
                        selectedContentColor = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.primary,
                        unselectedContentColor = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.onSurface,
                    )
                    items.forEachIndexed { index, item -> MiuixFloatingNavigationBarItem(selected = index == selectedIndex,
                        onClick = { if (index != selectedIndex) onSelected(index) },
                        icon = if (index == selectedIndex) item.selectedIcon else item.icon, label = item.label,
                        colors = itemColors) }
                }
                UiStyle.COUI -> if (Build.VERSION.SDK_INT >= 34 && LocalDensity.current.fontScale <= 1.3f)
                    ProvideCouiBackdrop(backdrop) { CouiFloatingNavigation(items, selectedIndex, onSelected) }
                else CouiAccessibleNavigation(items, selectedIndex, onSelected)
            }
        }
    }
    val page: @Composable (PaddingValues) -> Unit = { padding ->
        Row(Modifier.fillMaxSize().padding(start = padding.calculateStartPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
            end = padding.calculateEndPadding(androidx.compose.ui.unit.LayoutDirection.Ltr))) {
            if (wide && showNavigation) when (style) {
                UiStyle.MATERIAL -> NavigationRail { items.forEachIndexed { i, item -> NavigationRailItem(
                    selected = i == selectedIndex, onClick = { onSelected(i) },
                    icon = { Icon(if (i == selectedIndex) item.selectedIcon else item.icon, null) }, label = { Text(item.label) }) } }
                UiStyle.MIUIX -> top.yukonga.miuix.kmp.basic.NavigationRail {
                    items.forEachIndexed { i, item -> top.yukonga.miuix.kmp.basic.NavigationRailItem(
                        selected = i == selectedIndex, onClick = { onSelected(i) }, icon = item.icon, label = item.label) }
                }
                UiStyle.COUI -> Column(Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start))
                    .width(100.dp).fillMaxHeight().selectableGroup()
                    .verticalScroll(rememberScrollState())) {
                    items.forEachIndexed { i, item ->
                        CouiNavigationTile(item, i == selectedIndex, Modifier.fillMaxWidth(), onRail = true) { onSelected(i) }
                    }
                }
            }
            Box(Modifier.weight(1f).then(
                if (style == UiStyle.COUI) Modifier.couiBackdrop(backdrop).background(com.itos.coui.theme.CouiTheme.colorScheme.background)
                else Modifier
            )) { content(PaddingValues(bottom = padding.calculateBottomPadding())) }
        }
    }
    // Native bars change; NavDisplay and AndroidView remain in the same slot.
    Scaffold(modifier = modifier, bottomBar = bar, containerColor = uiStyleColors().background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0), content = page)
}

@RequiresApi(34)
@Composable
private fun CouiFloatingNavigation(items: List<UiNavItem>, selected: Int, onSelected: (Int) -> Unit) {
    // alpha7 reserves both accessory slots even when hidden. Allow those empty slots
    // outside the viewport so the centered capsule still has 48 dp targets.
    val capsuleWidth = 24.dp + 8.dp * (items.size - 1) + 48.dp * items.size
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        if (maxWidth < capsuleWidth) {
            CouiAccessibleNavigation(items, selected, onSelected)
        } else {
            val renderer = remember { com.itos.coui.effect.CouiFloatingRenderer() }
            com.itos.coui.navigation.NavigationBar(
                modifier = Modifier.requiredWidth(maxOf(maxWidth, capsuleWidth + 32.dp + 136.dp)),
                renderer = renderer,
                items = items.map { com.itos.coui.navigation.CouiNavigationItem(it.label, it.icon, it.selectedIcon) },
                selectedIndex = selected,
                onSelection = onSelected,
            )
        }
    }
}

/** COUI's floating navigation has a fixed text viewport. Give all destinations room at large font scales. */
@Composable
private fun CouiAccessibleNavigation(items: List<UiNavItem>, selected: Int, onSelected: (Int) -> Unit) {
    val colors = com.itos.coui.theme.CouiTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(CouiCapsuleShape).background(colors.surface)
            .border(1.dp, colors.outlineVariant, CouiCapsuleShape)
            .padding(horizontal = 4.dp).selectableGroup(),
    ) {
        items.forEachIndexed { i, item ->
            CouiNavigationTile(item, i == selected, Modifier.weight(1f)) { onSelected(i) }
        }
    }
}

@Composable
private fun CouiNavigationTile(item: UiNavItem, isSelected: Boolean, modifier: Modifier, onRail: Boolean = false, onClick: () -> Unit) {
    val colors = com.itos.coui.theme.CouiTheme.colorScheme
    val foreground = if (isSelected) colors.accentOnSurface else colors.onSurface
    val interaction = remember { MutableInteractionSource() }
    val target = modifier.heightIn(min = 48.dp)
        .clip(CouiCapsuleShape)
        .background(if (isSelected) colors.accent.copy(alpha = .12f) else Color.Transparent)
        .couiButtonFeedback(com.itos.coui.theme.CouiTheme.isDark, interaction)
        .selectable(
            selected = isSelected, interactionSource = interaction, indication = null,
            role = Role.Tab, onClick = { if (!isSelected) onClick() },
        )
    val icon: @Composable () -> Unit = {
        Icon(if (isSelected) item.selectedIcon else item.icon, null, tint = foreground, modifier = Modifier.size(24.dp))
    }
    val label: @Composable () -> Unit = {
        com.itos.coui.text.Text(text = item.label, kind = com.itos.coui.text.CouiTextKind.PreferenceTitle,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = foreground)
    }
    if (onRail) {
        Row(target.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
            icon()
            label()
        }
    } else {
        Column(target.padding(horizontal = 2.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)) {
            icon()
            label()
        }
    }
}


@Composable
actual fun MiuixCourseViewTabs(dayMode: Boolean, onDayModeChange: (Boolean) -> Unit, modifier: Modifier) {
    val scale = LocalDensity.current.fontScale
    val palette = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
    top.yukonga.miuix.kmp.basic.TabRowWithContour(
        tabs = listOf("日视图", "周视图"), selectedTabIndex = if (dayMode) 0 else 1,
        onTabSelected = { onDayModeChange(it == 0) }, modifier = modifier,
        height = 48.dp * scale, minWidth = 62.dp * scale, maxWidth = 240.dp * scale,
        colors = top.yukonga.miuix.kmp.basic.TabRowDefaults.tabRowColors(
            selectedBackgroundColor = palette.primaryContainer,
            selectedContentColor = palette.onPrimaryContainer,
        ),
    )
}

@Composable
actual fun MiuixSettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        top.yukonga.miuix.kmp.basic.SmallTitle(title, insideMargin = PaddingValues(horizontal = 12.dp, vertical = 8.dp))
        top.yukonga.miuix.kmp.basic.Card(Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
actual fun MiuixCourseSnackbar(data: androidx.compose.material3.SnackbarData) {
    val native = remember(data) {
        object : top.yukonga.miuix.kmp.basic.SnackbarData {
            override val visuals = top.yukonga.miuix.kmp.basic.SnackbarVisuals(
                message = data.visuals.message, actionLabel = data.visuals.actionLabel,
                withDismissAction = data.visuals.withDismissAction,
                duration = when (data.visuals.duration) {
                    androidx.compose.material3.SnackbarDuration.Short -> top.yukonga.miuix.kmp.basic.SnackbarDuration.Short
                    androidx.compose.material3.SnackbarDuration.Long -> top.yukonga.miuix.kmp.basic.SnackbarDuration.Long
                    androidx.compose.material3.SnackbarDuration.Indefinite -> top.yukonga.miuix.kmp.basic.SnackbarDuration.Indefinite
                })
            override suspend fun dismiss() = data.dismiss()
            override suspend fun performAction() = data.performAction()
        }
    }
    top.yukonga.miuix.kmp.basic.Snackbar(native)
}

@Composable
actual fun CouiCourseViewTabs(dayMode: Boolean, onDayModeChange: (Boolean) -> Unit, modifier: Modifier) {
    val backdrop = rememberCouiBackdrop()
    Box(modifier) {
        // A backdrop source must not sample itself. This plane is behind the native chips.
        Box(Modifier.matchParentSize().couiBackdrop(backdrop).background(com.itos.coui.theme.CouiTheme.colorScheme.background))
        com.itos.coui.chip.ChipTabRow(items = listOf(
            com.itos.coui.chip.CouiChipTabItem(true, "日视图"), com.itos.coui.chip.CouiChipTabItem(false, "周视图")),
            selectedKey = dayMode, onSelectedKeyChange = { onDayModeChange(it as Boolean) }, backdrop = backdrop)
    }
}

@Composable
actual fun CouiSettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    com.itos.coui.layout.Group(title = title, content = content)
}

@Composable
actual fun CouiCourseSnackbar(data: androidx.compose.material3.SnackbarData) {
    // alpha7 has no public Snackbar: retain the host's timeout/action contract in a COUI row.
    com.itos.coui.layout.Group(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        com.itos.coui.layout.Item(modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            title = { UiText(data.visuals.message) })
        data.visuals.actionLabel?.let { label -> UiButton(label, data::performAction) }
        if (data.visuals.withDismissAction) UiButton("关闭", data::dismiss)
    }
}

@Composable
actual fun UiCourseIconButton(icon: ImageVector, description: String, tint: Color, inToolbar: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val iconColor = if (tint == Color.Unspecified) uiStyleColors().onSurface else tint
    val resolvedTint = if (enabled) iconColor else iconColor.copy(alpha = iconColor.alpha * .38f)
    when (LocalUiStyle.current) {
        UiStyle.MATERIAL -> IconButton(onClick, enabled = enabled) { Icon(icon, description, tint = resolvedTint) }
        UiStyle.MIUIX -> top.yukonga.miuix.kmp.basic.IconButton(onClick = onClick, modifier = Modifier.size(48.dp), enabled = enabled) {
            top.yukonga.miuix.kmp.basic.Icon(icon, description, tint = resolvedTint)
        }
        UiStyle.COUI -> if (inToolbar) {
            com.itos.coui.topappbar.TopAppBarButton(onClick = onClick, enabled = enabled) { Icon(icon, description, tint = resolvedTint) }
        } else {
            val interaction = remember { MutableInteractionSource() }
            Box(
                Modifier.size(48.dp).clip(CircleShape).couiButtonFeedback(interactionSource = interaction, enabled = enabled)
                    .clickable(interaction, null, enabled = enabled, role = Role.Button, onClick = onClick),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, description, tint = resolvedTint) }
        }
    }
}
