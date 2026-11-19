package com.xingheyuzhuan.shiguangschedule.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.Destination
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import shiguangschedule.shared.generated.resources.*

@Immutable
private data class NavItemData(
    val label: String,
    val destination: Destination,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

/**
 * 自适应导航栏组件
 */
@Composable
fun AdaptiveNavigationScaffold(
    currentDestination: Destination,
    onTabSelected: (Destination) -> Unit,
    modifier: Modifier = Modifier,
    showNavigation: Boolean = true,
    navHideFractionProvider: () -> Float = { 0f },
    isTransparent: Boolean = false,
    navigationModifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit
) {
    val todayLabel = stringResource(Res.string.nav_campus_services)
    val courseLabel = stringResource(Res.string.nav_course_schedule)
    val settingsLabel = stringResource(Res.string.nav_settings)
    val xatuLabel = stringResource(Res.string.nav_xatu_home)
    val academicLabel = stringResource(Res.string.nav_academic)
    val academicIcon = vectorResource(Res.drawable.school_24px)

    val todaySelectedIcon = vectorResource(Res.drawable.view_agenda_filled_24px)
    val todayUnselectedIcon = vectorResource(Res.drawable.view_agenda_24px)
    val courseSelectedIcon = vectorResource(Res.drawable.view_week_filled_24px)
    val courseUnselectedIcon = vectorResource(Res.drawable.view_week_24px)
    val settingsSelectedIcon = vectorResource(Res.drawable.tune_24px)
    val settingsUnselectedIcon = settingsSelectedIcon
    val xatuIcon = vectorResource(Res.drawable.home_24px)

    val navItems = remember(
        todayLabel, courseLabel, settingsLabel, xatuLabel, academicLabel, academicIcon,
        todaySelectedIcon, todayUnselectedIcon,
        courseSelectedIcon, courseUnselectedIcon,
        settingsSelectedIcon, settingsUnselectedIcon, xatuIcon
    ) {
        listOf(
            NavItemData(
                label = xatuLabel,
                destination = Destination.XatuHome,
                selectedIcon = xatuIcon,
                unselectedIcon = xatuIcon
            ),
            NavItemData(
                label = courseLabel,
                destination = Destination.CourseSchedule,
                selectedIcon = courseSelectedIcon,
                unselectedIcon = courseUnselectedIcon
            ),
            NavItemData(
                label = academicLabel,
                destination = Destination.XatuAcademic,
                selectedIcon = academicIcon,
                unselectedIcon = academicIcon
            ),
            NavItemData(
                label = todayLabel,
                destination = Destination.XatuServices,
                selectedIcon = todaySelectedIcon,
                unselectedIcon = todayUnselectedIcon
            ),
            NavItemData(
                label = settingsLabel,
                destination = Destination.Settings,
                selectedIcon = settingsSelectedIcon,
                unselectedIcon = settingsUnselectedIcon
            )
        )
    }

    val layoutType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfo())
    val size = LocalWindowInfo.current.containerSize
    val shortLandscape = with(LocalDensity.current) { size.width.toDp() >= 600.dp && size.height.toDp() < 480.dp }

    UiNavigationLayout(
        items = navItems.map { UiNavItem(it.label, it.unselectedIcon, it.selectedIcon) },
        selectedIndex = navItems.indexOfFirst { currentDestination::class == it.destination::class }.coerceAtLeast(0),
        onSelected = { index -> onTabSelected(navItems[index].destination) },
        wide = shortLandscape || layoutType != NavigationSuiteType.NavigationBar, showNavigation = showNavigation,
        modifier = modifier, navigationModifier = navigationModifier,
        navHideFractionProvider = navHideFractionProvider, isTransparent = isTransparent,
        content = content,
    )
}
