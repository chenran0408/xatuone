package com.xingheyuzhuan.shiguangschedule

import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.FiniteAnimationSpec
import com.xingheyuzhuan.shiguangschedule.data.model.UiStyle
import com.xingheyuzhuan.shiguangschedule.ui.components.LocalUiStyle
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.metadata
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.xingheyuzhuan.shiguangschedule.data.model.StartScreen
import com.xingheyuzhuan.shiguangschedule.tool.ExternalFileManager
import com.xingheyuzhuan.shiguangschedule.ui.components.AdaptiveNavigationScaffold
import com.xingheyuzhuan.shiguangschedule.ui.schedule.WeeklyScheduleScreen
import com.xingheyuzhuan.shiguangschedule.ui.schoolselection.list.AdapterSelectionScreen
import com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web.WebViewScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.SettingsScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.CourseToolsScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.SettingsViewModel
import com.xingheyuzhuan.shiguangschedule.ui.settings.additional.LanguageSettingScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.additional.MoreOptionsScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.additional.OpenSourceLicensesScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.backup.BackupScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.contribution.ContributionScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.conversion.CourseTableConversionScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.xatu.XatuLoginScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.xatu.XatuCourseSyncDialog
import com.xingheyuzhuan.shiguangschedule.ui.settings.xatu.xatuCourseImport
import com.xingheyuzhuan.shiguangschedule.ui.settings.xatu.XatuHomeScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.xatu.XatuServicesScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.xatu.XatuAcademicScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.xatu.XatuSystemSettingsScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.xatu.XatuServiceScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.course.AddEditCourseScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.coursemanagement.CourseInstanceListScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.coursemanagement.CourseNameListScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.coursetables.ManageCourseTablesScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.holiday.HolidayManagementScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.notification.NotificationSettingsScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.quickactions.QuickActionsScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.quickactions.delete.QuickDeleteScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.quickactions.tweaks.TweakScheduleScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.style.StyleSettingsScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.style.widget.WidgetStyleSettingsScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.themesettings.ThemeSettingsScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.time.ComboScheduleEditScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.time.SingleScheduleEditScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.time.TimeScheduleManagementScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.update.UpdateRepoScreen
import com.xingheyuzhuan.shiguangschedule.ui.theme.ShiguangScheduleTheme
import com.xingheyuzhuan.shiguangschedule.ui.today.TodayScheduleScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    val viewModel: SettingsViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsState()

    if (state.isReady) {
        ShiguangScheduleTheme(settings = state.appSettings) {
            val startDest = remember(state.appSettings.startScreen) {
                when (state.appSettings.startScreen) {
                    StartScreen.CAMPUS_HOME -> Destination.XatuHome
                    StartScreen.COURSE_SCHEDULE -> Destination.CourseSchedule
                    StartScreen.TODAY_SCHEDULE -> Destination.TodaySchedule
                }
            }
            AppNavigation(startDestination = startDest)
        }
    } else {
        Surface(modifier = Modifier.fillMaxSize()) {}
    }
}

@Composable
fun AppNavigation(startDestination: Destination) {
    val backStack = rememberNavBackStack(
        configuration = navSavedStateConfig,
        startDestination
    )

    val currentDestination = backStack.lastOrNull() as? Destination ?: startDestination

    var navHideFraction by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(currentDestination) {
        if (currentDestination != Destination.CourseSchedule) navHideFraction = 0f
    }

    val pendingFile by ExternalFileManager.pendingFile.collectAsState()
    var showCourseSync by remember { mutableStateOf(false) }
    var syncTableId by remember { mutableStateOf<String?>(null) }

    val onNavigate: (Destination) -> Unit = remember(backStack) {
        { dest ->
            if (dest == xatuCourseImport) {
                syncTableId = null
                showCourseSync = true
            } else backStack.navigateTo(dest)
        }
    }

    LaunchedEffect(pendingFile) {
        val target = pendingFile?.targetDestination
        if (target != null && backStack.lastOrNull() != target) {
            onNavigate(target)
        }
    }

    val onBack: () -> Unit = remember(backStack) {
        {
            if (backStack.size > 1) {
                backStack.removeAt(backStack.lastIndex)
            } else if ((backStack.lastOrNull() as? Destination)?.isMainScreen == false) {
                backStack.navigateTo(Destination.XatuHome)
            }
        }
    }

    val style = LocalUiStyle.current
    val animSpec: FiniteAnimationSpec<IntOffset> = when (style) {
        UiStyle.MATERIAL -> tween(300)
        UiStyle.MIUIX -> spring(dampingRatio = .9f, stiffness = 420f)
        UiStyle.COUI -> spring(dampingRatio = 1f, stiffness = 320f)
    }
    val fadeSpec = when (style) {
        UiStyle.MATERIAL -> tween<Float>(220)
        UiStyle.MIUIX -> spring(dampingRatio = .9f, stiffness = 420f)
        UiStyle.COUI -> tween<Float>(180)
    }
    val outgoingDivisor = if (style == UiStyle.COUI) 5 else 3

    AdaptiveNavigationScaffold(
        currentDestination = currentDestination,
        onTabSelected = onNavigate,
        showNavigation = currentDestination.isMainScreen,
        navHideFractionProvider = { navHideFraction }
    ) { navigationPadding ->
        NavDisplay(
            backStack = backStack,
            onBack = onBack,
            modifier = Modifier.fillMaxSize().padding(bottom = navigationPadding.calculateBottomPadding())
                .consumeWindowInsets(navigationPadding),
            transitionSpec = {
                slideInHorizontally(initialOffsetX = { it }, animationSpec = animSpec) + fadeIn(animationSpec = fadeSpec) togetherWith
                        slideOutHorizontally(targetOffsetX = { -it / outgoingDivisor }, animationSpec = animSpec) + fadeOut(animationSpec = fadeSpec)
            },
            popTransitionSpec = {
                slideInHorizontally(initialOffsetX = { -it / outgoingDivisor }, animationSpec = animSpec) + fadeIn(animationSpec = fadeSpec) togetherWith
                        slideOutHorizontally(targetOffsetX = { it }, animationSpec = animSpec) + fadeOut(animationSpec = fadeSpec)
            },
            predictivePopTransitionSpec = {
                slideInHorizontally(initialOffsetX = { -it / outgoingDivisor }, animationSpec = animSpec) + fadeIn(animationSpec = fadeSpec) togetherWith
                        slideOutHorizontally(targetOffsetX = { it }, animationSpec = animSpec) + fadeOut(animationSpec = fadeSpec)
            },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            )
        ) { key ->
            val destination = key as Destination

            NavEntry(
                key = key,
                metadata = metadata {
                    put(ShiguangNavMetadata.IsMainScreenKey, destination.isMainScreen)
                }
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ScreenContent(
                        targetDest = destination,
                        onNavigate = onNavigate,
                        onBack = onBack,
                        onLoginComplete = {
                            if (backStack.lastOrNull() === destination) {
                                onBack()
                                (destination as? Destination.WebView)?.courseSyncTableId?.let { tableId ->
                                    if (backStack.lastOrNull() is Destination.XatuSyncLogin) onBack()
                                    syncTableId = tableId
                                    showCourseSync = true
                                }
                            }
                        },
                        onNavHideFractionChanged = { navHideFraction = it }
                    )
                }
            }
        }
    }

    if (showCourseSync) {
        XatuCourseSyncDialog(
            initialTableId = syncTableId,
            onDismiss = { showCourseSync = false },
            onLogin = { loginDestination ->
                showCourseSync = false
                onNavigate(loginDestination)
            }
        )
    }
}

@Composable
fun ScreenContent(
    targetDest: Destination,
    onNavigate: (Destination) -> Unit,
    onBack: () -> Unit,
    onLoginComplete: () -> Unit,
    onNavHideFractionChanged: (Float) -> Unit
) {
    when (targetDest) {
        Destination.CourseSchedule -> WeeklyScheduleScreen(
            onNavigate = onNavigate,
            onBack = onBack,
            onNavHideFractionChanged = onNavHideFractionChanged
        )
        Destination.Settings -> com.xingheyuzhuan.shiguangschedule.ui.settings.SettingsHomeScreen(onNavigate)
        Destination.CourseSettings -> SettingsScreen(onBack)
        Destination.CourseStartDateSettings -> SettingsScreen(onBack, openStartDatePicker = true)
        Destination.CourseTools -> CourseToolsScreen(onNavigate, onBack)
        Destination.AcademicSettings -> com.xingheyuzhuan.shiguangschedule.ui.settings.xatu.XatuPreferencesScreen(false, onNavigate, onBack)
        Destination.CampusSettings -> com.xingheyuzhuan.shiguangschedule.ui.settings.xatu.XatuPreferencesScreen(true, onNavigate, onBack)
        Destination.PrivacySettings -> XatuLoginScreen(onNavigate, onBack)
        Destination.WebSettings -> XatuSystemSettingsScreen(onBack = onBack)
        Destination.TodaySchedule -> TodayScheduleScreen(onNavigate, onBack)
        Destination.XatuHome -> XatuHomeScreen(onNavigate, onBack, PaddingValues(0.dp))
        Destination.XatuServices -> XatuServicesScreen(onNavigate, onBack)
        Destination.XatuAcademic -> XatuAcademicScreen(onNavigate)
        is Destination.XatuService -> XatuServiceScreen(targetDest.page, onNavigate, onBack)
        Destination.ManageCourseTables -> ManageCourseTablesScreen(onBack)
        Destination.CourseTableConversion -> CourseTableConversionScreen(onNavigate, onBack)
        Destination.XatuLogin -> XatuLoginScreen(onNavigate, onBack)
        is Destination.XatuSyncLogin -> XatuLoginScreen(onNavigate, onBack, targetDest.tableId)
        Destination.NotificationSettings -> NotificationSettingsScreen(onBack)
        Destination.MoreOptions -> MoreOptionsScreen(onNavigate, onBack)
        Destination.OpenSourceLicenses -> OpenSourceLicensesScreen(onBack)
        Destination.UpdateRepo -> UpdateRepoScreen(onBack)
        Destination.QuickActions -> QuickActionsScreen(onNavigate, onBack)
        Destination.TweakSchedule -> TweakScheduleScreen(onBack)
        Destination.ContributionList -> ContributionScreen(onBack)
        Destination.CourseManagementList -> CourseNameListScreen(onNavigate, onBack)
        Destination.StyleSettings -> StyleSettingsScreen(onNavigate,onBack)
        Destination.QuickDelete -> QuickDeleteScreen(onBack)
        Destination.ThemeSettings -> ThemeSettingsScreen(onBack)
        Destination.BackupAndRestore -> BackupScreen(onBack)
        Destination.LanguageSettings -> LanguageSettingScreen(onBack)
        Destination.WidgetStyleSettings -> WidgetStyleSettingsScreen(onBack)
        Destination.HolidayManagement -> HolidayManagementScreen(onBack = onBack)

        Destination.TimeScheduleManagement -> TimeScheduleManagementScreen(
            onBack = onBack,
            onEditSingleSchedule = { tableId, isPublic, copyFromId ->
                onNavigate(Destination.SingleScheduleEdit(tableId, isPublic, copyFromId))
            },
            onEditComboSchedule = { comboId, copyFromId ->
                onNavigate(Destination.ComboScheduleEdit(comboId, copyFromId))
            }
        )

        // 单一/公共作息编辑页面路由
        is Destination.SingleScheduleEdit -> SingleScheduleEditScreen(
            tableId = targetDest.tableId,
            isPublic = targetDest.isPublic,
            copyFromId = targetDest.copyFromId,
            onBack = onBack
        )

        // 组合作息编辑页面路由
        is Destination.ComboScheduleEdit -> ComboScheduleEditScreen(
            comboId = targetDest.comboId,
            copyFromId = targetDest.copyFromId,
            onBack = onBack
        )

        is Destination.AdapterSelection -> AdapterSelectionScreen(
            onNavigate, onBack, targetDest.schoolId, targetDest.schoolName, targetDest.categoryNumber, targetDest.resourceFolder
        )
        is Destination.WebView -> WebViewScreen(
            onNavigate, onBack, targetDest.initialUrl, targetDest.assetJsPath, targetDest.xatuLogin,
            serviceTitle = targetDest.serviceTitle,
            onLoginComplete = onLoginComplete,
        )
        is Destination.AddEditCourse -> AddEditCourseScreen(
            onBack, targetDest.courseId
        )
        is Destination.CourseManagementDetail -> CourseInstanceListScreen(
            targetDest.courseName, onBack, onNavigate
        )
    }
}
