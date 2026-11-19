package com.xingheyuzhuan.shiguangschedule.ui.settings.coursemanagement

import com.xingheyuzhuan.shiguangschedule.ui.components.UiScaffold
import com.xingheyuzhuan.shiguangschedule.ui.components.UiTopAppBar
import com.xingheyuzhuan.shiguangschedule.ui.components.LocalUiStyle
import com.xingheyuzhuan.shiguangschedule.ui.components.UiText

import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xingheyuzhuan.shiguangschedule.Destination
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedule.data.model.DualColor
import com.xingheyuzhuan.shiguangschedule.navigation.AddEditCourseChannel
import com.xingheyuzhuan.shiguangschedule.navigation.PresetCourseData
import com.xingheyuzhuan.shiguangschedule.ui.components.UiCourseIconButton
import com.xingheyuzhuan.shiguangschedule.ui.theme.LocalIsDarkTheme
import com.itos.coui.effect.couiButtonFeedback
import androidx.compose.foundation.interaction.MutableInteractionSource
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.a11y_enter_selection_mode
import shiguangschedule.shared.generated.resources.a11y_exit_selection_mode
import shiguangschedule.shared.generated.resources.a11y_back
import shiguangschedule.shared.generated.resources.action_add
import shiguangschedule.shared.generated.resources.action_deselect_all
import shiguangschedule.shared.generated.resources.action_select_all
import shiguangschedule.shared.generated.resources.add_24px
import shiguangschedule.shared.generated.resources.arrow_back_24px
import shiguangschedule.shared.generated.resources.check_circle_24px
import shiguangschedule.shared.generated.resources.close_24px
import shiguangschedule.shared.generated.resources.course_time_day_section_details_tweak
import shiguangschedule.shared.generated.resources.course_time_day_time_details_tweak
import shiguangschedule.shared.generated.resources.delete_24px
import shiguangschedule.shared.generated.resources.done_all_24px
import shiguangschedule.shared.generated.resources.label_weeks_format
import shiguangschedule.shared.generated.resources.menu_open_24px
import shiguangschedule.shared.generated.resources.title_selected_items_count
import shiguangschedule.shared.generated.resources.a11y_delete_selected
import shiguangschedule.shared.generated.resources.week_days_full_names
import com.xingheyuzhuan.shiguangschedule.data.model.UiStyle

/**
 * 二级页面：展示特定课程名称下的所有实例，使用两列网格 (Detail View)。
 * @param courseName 从导航参数中接收，用于 ViewModel 过滤。
 * @param onNavigateBack 导航回上一级
 * @param onNavigate 用于在 Composable 内部处理 FAB 和卡片点击时的导航。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseInstanceListScreen(
    courseName: String,
    onNavigateBack: () -> Unit,
    onNavigate: (Destination) -> Unit,
    viewModel: CourseInstanceListViewModel = koinViewModel()
) {
    LaunchedEffect(courseName) {
        viewModel.initCourseName(courseName)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val courseInstances by viewModel.courseInstances.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedCourseIds by viewModel.selectedCourseIds.collectAsState()
    val scope = rememberCoroutineScope()

    val onNavigateToAddNewCourse: () -> Unit = {
        scope.launch {
            val presetData = PresetCourseData(
                name = courseName,
                startSection = 1,
                endSection = 2
            )
            AddEditCourseChannel.sendEvent(presetData)
            onNavigate(Destination.AddEditCourse(courseId = null))
        }
    }

    // 编辑课程逻辑
    val onNavigateToEditCourse: (courseId: String) -> Unit = { courseId ->
        onNavigate(Destination.AddEditCourse(courseId = courseId))
    }

    UiScaffold(
        topBar = {
            UiTopAppBar(title = if (isSelectionMode) {
                            stringResource(Res.string.title_selected_items_count, selectedCourseIds.size)
                        } else {
                            courseName
                        },
                navigationIcon = {
                    UiCourseIconButton(
                        icon = if (isSelectionMode) vectorResource(Res.drawable.close_24px) else vectorResource(Res.drawable.arrow_back_24px),
                        description = stringResource(
                                if (isSelectionMode) Res.string.a11y_exit_selection_mode else Res.string.a11y_back
                            ),
                        inToolbar = true,
                        onClick = if (isSelectionMode) viewModel::toggleSelectionMode else onNavigateBack,
                    )
                },
                actions = {
                    if (isSelectionMode) {
                        val totalCount = courseInstances.size
                        val selectedCount = selectedCourseIds.size
                        val isAllSelected = totalCount > 0 && selectedCount == totalCount

                        UiCourseIconButton(
                            icon = vectorResource(Res.drawable.done_all_24px),
                            description = stringResource(
                                if (isAllSelected) Res.string.action_deselect_all else Res.string.action_select_all
                            ),
                            inToolbar = true,
                            enabled = totalCount > 0,
                            onClick = viewModel::toggleSelectAll,
                        )

                        UiCourseIconButton(
                            icon = vectorResource(Res.drawable.delete_24px),
                            description = stringResource(Res.string.a11y_delete_selected),
                            inToolbar = true,
                            enabled = selectedCount > 0,
                            onClick = { scope.launch { viewModel.deleteSelectedCourses() } },
                        )
                    }

                    UiCourseIconButton(
                        icon = vectorResource(Res.drawable.menu_open_24px),
                        description = stringResource(
                            if (isSelectionMode) Res.string.a11y_exit_selection_mode else Res.string.a11y_enter_selection_mode
                        ),
                        inToolbar = true,
                        enabled = courseInstances.isNotEmpty() || isSelectionMode,
                        onClick = viewModel::toggleSelectionMode,
                    )

                    if (!isSelectionMode && LocalUiStyle.current == UiStyle.COUI) {
                        UiCourseIconButton(
                            icon = vectorResource(Res.drawable.add_24px),
                            description = stringResource(Res.string.action_add),
                            inToolbar = true,
                            onClick = onNavigateToAddNewCourse,
                        )
                    }
                })
        },
        floatingActionButton = {
            if (!isSelectionMode && LocalUiStyle.current != UiStyle.COUI) {
                FloatingActionButton(onClick = onNavigateToAddNewCourse) {
                    Icon(vectorResource(Res.drawable.add_24px), contentDescription = stringResource(Res.string.action_add))
                }
            }
        }
    ) { paddingValues ->
        if (courseInstances.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                UiText("当前课表中没有该课程的实例", style = MaterialTheme.typography.bodyLarge)
            }
        } else if (LocalUiStyle.current == UiStyle.COUI) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(courseInstances, key = { it.course.id }) { courseWithWeeks ->
                    CourseInstanceCard(
                        courseWithWeeks = courseWithWeeks,
                        isSelected = selectedCourseIds.contains(courseWithWeeks.course.id),
                        colorMaps = uiState.courseColorMaps,
                        onCourseClick = { courseId ->
                            if (isSelectionMode) viewModel.toggleCourseSelection(courseId)
                            else onNavigateToEditCourse(courseId)
                        },
                        onCourseLongClick = { courseId ->
                            if (!isSelectionMode) viewModel.toggleSelectionMode()
                            viewModel.toggleCourseSelection(courseId)
                        }
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(courseInstances, key = { it.course.id }) { courseWithWeeks ->
                    CourseInstanceCard(
                        courseWithWeeks = courseWithWeeks,
                        isSelected = selectedCourseIds.contains(courseWithWeeks.course.id),
                        colorMaps = uiState.courseColorMaps,
                        onCourseClick = { courseId ->
                            if (isSelectionMode) {
                                viewModel.toggleCourseSelection(courseId)
                            } else {
                                onNavigateToEditCourse(courseId)
                            }
                        },
                        onCourseLongClick = { courseId ->
                            if (!isSelectionMode) viewModel.toggleSelectionMode()
                            viewModel.toggleCourseSelection(courseId)
                        }
                    )
                }
            }
        }
    }
}

/**
 * 课程实例卡片 Composable (两列网格中的单个卡片)。
 */
@Composable
fun CourseInstanceCard(
    courseWithWeeks: CourseWithWeeks,
    isSelected: Boolean,
    colorMaps: List<DualColor>,
    onCourseClick: (courseId: String) -> Unit,
    onCourseLongClick: (courseId: String) -> Unit
) {
    val course = courseWithWeeks.course
    val courseId = course.id

    val isDarkTheme = LocalIsDarkTheme.current

    // 如果索引不存在，则取列表第一项；如果列表为空，则使用 MaterialTheme 的 SurfaceVariant 颜色兜底
    val fallbackColor = DualColor(
        light = if (LocalUiStyle.current == UiStyle.MIUIX) top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surfaceVariant,
        dark = if (LocalUiStyle.current == UiStyle.MIUIX) top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surfaceVariant
    )
    val courseColorDual = colorMaps.getOrNull(course.colorInt) ?: colorMaps.firstOrNull() ?: fallbackColor

    // 根据主题获取课程背景色
    val courseBackgroundColor = if (isDarkTheme) courseColorDual.dark else courseColorDual.light

    val weekDays = stringArrayResource(Res.array.week_days_full_names)
    val dayName = weekDays.getOrElse(course.day - 1) { "?" }

    if (LocalUiStyle.current == UiStyle.COUI) {
        val colors = com.itos.coui.layout.CouiPageDefaults.themeColors()
        val interactionSource = androidx.compose.runtime.remember { MutableInteractionSource() }
        val timeText = if (course.isCustomTime) {
            stringResource(
                Res.string.course_time_day_time_details_tweak,
                dayName,
                course.customStartTime ?: "?",
                course.customEndTime ?: "?"
            )
        } else {
            stringResource(
                Res.string.course_time_day_section_details_tweak,
                dayName,
                course.startSection ?: "?",
                course.endSection ?: "?"
            )
        }
        val formattedWeeks = courseWithWeeks.weeks.map { it.weekNumber }.joinToString(", ")

        com.itos.coui.layout.Group(
            modifier = Modifier.fillMaxWidth(),
            colors = colors.copy(card = courseBackgroundColor),
        ) {
            com.itos.coui.layout.Item(
                modifier = Modifier
                    .semantics { selected = isSelected }
                    .couiButtonFeedback(interactionSource = interactionSource, list = true)
                    .combinedClickable(
                        interactionSource = interactionSource,
                        indication = null,
                        role = Role.Button,
                        onClick = { onCourseClick(courseId) },
                        onLongClick = { onCourseLongClick(courseId) },
                    ),
                icon = if (isSelected) {
                    {
                        Icon(
                            vectorResource(Res.drawable.check_circle_24px),
                            contentDescription = null,
                            tint = colors.accent,
                        )
                    }
                } else null,
                title = {
                    com.itos.coui.text.Text(
                        text = timeText,
                        kind = com.itos.coui.text.CouiTextKind.PreferenceTitle,
                    )
                },
                summary = {
                    Column {
                        listOf(
                            course.teacher,
                            course.position,
                            stringResource(Res.string.label_weeks_format, formattedWeeks),
                        ).filter { it.isNotBlank() }.forEach { line ->
                            com.itos.coui.text.Text(
                                text = line,
                                kind = com.itos.coui.text.CouiTextKind.PreferenceSummary,
                            )
                        }
                    }
                },
            )
        }
        return
    }

    // 卡片颜色：始终使用课程颜色作为背景
    val cardShape = MaterialTheme.shapes.medium
    val cardContent: @Composable ColumnScope.() -> Unit = {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // 教师信息
            UiText(
                text = course.teacher,
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            // 地点信息
            UiText(
                text = course.position,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 上课时间/节次
            val timeText = if (course.isCustomTime) {
                stringResource(
                    Res.string.course_time_day_time_details_tweak,
                    dayName,
                    course.customStartTime ?: "?",
                    course.customEndTime ?: "?"
                )
            } else {
                stringResource(
                    Res.string.course_time_day_section_details_tweak,
                    dayName,
                    course.startSection ?: "?",
                    course.endSection ?: "?"
                )
            }

            UiText(
                text = timeText,
                style = MaterialTheme.typography.bodyMedium
            )

            // 周次信息
            val formattedWeeks = courseWithWeeks.weeks.map { it.weekNumber }.joinToString(", ")
            UiText(
                text = stringResource(
                    Res.string.label_weeks_format,
                    formattedWeeks
                ),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }

    if (LocalUiStyle.current == UiStyle.MIUIX) {
        top.yukonga.miuix.kmp.basic.Card(
            modifier = Modifier
                .height(IntrinsicSize.Max)
                .then(if (isSelected) Modifier.border(2.dp, top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.primary, RoundedCornerShape(16.dp)) else Modifier),
            colors = top.yukonga.miuix.kmp.basic.CardDefaults.defaultColors(
                color = courseBackgroundColor,
                contentColor = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.onSurface,
            ),
            onClick = { onCourseClick(courseId) },
            onLongPress = { onCourseLongClick(courseId) },
            pressFeedbackType = top.yukonga.miuix.kmp.utils.PressFeedbackType.Sink,
        ) {
            cardContent()
        }
    } else {
        Card(
            shape = cardShape,
            colors = CardDefaults.cardColors(
                containerColor = courseBackgroundColor,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier
                .height(IntrinsicSize.Max)
                .clip(cardShape)
                .combinedClickable(
                    onClick = { onCourseClick(courseId) },
                    onLongClick = { onCourseLongClick(courseId) }
                )
                .then(
                    if (isSelected) {
                        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, cardShape)
                    } else Modifier
                )
        ) {
            cardContent()
        }
    }
}
