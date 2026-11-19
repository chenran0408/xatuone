package com.xingheyuzhuan.shiguangschedule.ui.schedule.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.xingheyuzhuan.shiguangschedule.ui.components.UiSheet
import com.xingheyuzhuan.shiguangschedule.ui.components.LocalUiStyle
import com.xingheyuzhuan.shiguangschedule.data.model.UiStyle
import com.xingheyuzhuan.shiguangschedule.ui.components.UiCourseIconButton
import com.xingheyuzhuan.shiguangschedule.ui.components.UiSettingRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import com.xingheyuzhuan.shiguangschedule.ui.components.UiText
import com.xingheyuzhuan.shiguangschedule.ui.components.uiStyleColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.xingheyuzhuan.shiguangschedule.ui.schedule.model.MergedCourseBlock
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.a11y_edit
import shiguangschedule.shared.generated.resources.action_double_week
import shiguangschedule.shared.generated.resources.action_single_week
import shiguangschedule.shared.generated.resources.award_star_24px
import shiguangschedule.shared.generated.resources.calendar_today_24px
import shiguangschedule.shared.generated.resources.class_24px
import shiguangschedule.shared.generated.resources.edit_24px
import shiguangschedule.shared.generated.resources.label_section_range_suffix
import shiguangschedule.shared.generated.resources.label_credit
import shiguangschedule.shared.generated.resources.location_on_24px
import shiguangschedule.shared.generated.resources.person_24px
import shiguangschedule.shared.generated.resources.schedule_24px
import shiguangschedule.shared.generated.resources.sticky_note_2_24px
import shiguangschedule.shared.generated.resources.week_days_full_names
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 课程详情 UI 数据模型
 */
@Immutable
private data class CourseDetailUIModel(
    val id: String,
    val name: String,
    val number: String?,
    val teacher: String,
    val position: String,
    val weeksDisplayStr: String,
    val dayStr: String,
    val timeStr: String,
    val credit: Float?,
    val remark: String?
)

/**
 * 课程详情底部弹窗组件
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CourseDetailBottomSheet(
    block: MergedCourseBlock,
    onDismissRequest: () -> Unit,
    onEditClick: (String) -> Unit
) {
    val rawCoursesList = remember(block) {
        block.clusterCourses.ifEmpty { block.courses }
    }
    if (rawCoursesList.isEmpty()) return

    val weekDaysFullNames = stringArrayResource(Res.array.week_days_full_names)
    val sectionSuffix = stringResource(Res.string.label_section_range_suffix)
    val singleLabel = stringResource(Res.string.action_single_week)
    val doubleLabel = stringResource(Res.string.action_double_week)

    val uiModels = remember(block) {
        rawCoursesList.map { wrapper ->
            val course = wrapper.course
            val dayStr = weekDaysFullNames.getOrNull(course.day - 1) ?: ""
            val timeStr = if (course.isCustomTime) {
                "${course.customStartTime} - ${course.customEndTime}"
            } else {
                "${course.startSection ?: 0}-${course.endSection ?: 0} $sectionSuffix"
            }
            CourseDetailUIModel(
                id = course.id,
                name = com.xingheyuzhuan.shiguangschedule.ui.schedule.courseDisplayName(course.name).title,
                number = com.xingheyuzhuan.shiguangschedule.ui.schedule.courseDisplayName(course.name).number,
                teacher = course.teacher,
                position = course.position,
                weeksDisplayStr = formatWeeks(wrapper.weeks.map { it.weekNumber }, singleLabel, doubleLabel),
                dayStr = dayStr,
                timeStr = timeStr,
                credit = course.credit,
                remark = course.remark
            )
        }
    }

    val clickedCourseId = block.courses.firstOrNull()?.course?.id
    val initialPageIndex = remember(block) {
        val index = uiModels.indexOfFirst { it.id == clickedCourseId }
        if (index >= 0) index else 0
    }

    val pagerState = key(block) {
        rememberPagerState(
            initialPage = initialPageIndex,
            pageCount = { uiModels.size }
        )
    }

    val density = LocalDensity.current
    val pageHeights = remember(block, density, LocalUiStyle.current) { mutableStateMapOf<Int, Dp>() }

    val dynamicHeightModifier = Modifier
        .fillMaxWidth()
        .layout { measurable, constraints ->
            val currentPage = pagerState.currentPage
            val offset = pagerState.currentPageOffsetFraction
            val targetPage = when {
                offset > 0f -> currentPage + 1
                offset < 0f -> currentPage - 1
                else -> currentPage
            }.coerceIn(0, uiModels.size - 1)

            val currentHeight = pageHeights[currentPage]
            val targetHeight = pageHeights[targetPage] ?: currentHeight

            if (currentHeight != null && targetHeight != null) {
                val height = lerp(currentHeight, targetHeight, abs(offset))
                val heightPx = with(density) { height.roundToPx() }
                    .coerceIn(constraints.minHeight, constraints.maxHeight)
                val placeable = measurable.measure(constraints.copy(minHeight = heightPx, maxHeight = heightPx))
                layout(placeable.width, placeable.height) {
                    placeable.placeRelative(0, 0)
                }
            } else {
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    placeable.placeRelative(0, 0)
                }
            }
        }

    val classIcon = vectorResource(Res.drawable.class_24px)
    val personIcon = vectorResource(Res.drawable.person_24px)
    val locationIcon = vectorResource(Res.drawable.location_on_24px)
    val calendarIcon = vectorResource(Res.drawable.calendar_today_24px)
    val scheduleIcon = vectorResource(Res.drawable.schedule_24px)
    val creditIcon = vectorResource(Res.drawable.award_star_24px)
    val noteIcon = vectorResource(Res.drawable.sticky_note_2_24px)
    val editIcon = vectorResource(Res.drawable.edit_24px)
    val editA11yText = stringResource(Res.string.a11y_edit)
    val isCoui = LocalUiStyle.current == UiStyle.COUI

    UiSheet(
        onDismissRequest = onDismissRequest,
        skipPartiallyExpanded = true,
        sheetGesturesEnabled = false
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = if (isCoui) 12.dp else 36.dp)
        ) {
            Box(modifier = dynamicHeightModifier) {
                HorizontalPager(
                    state = pagerState,
                    key = { page -> uiModels[page].id },
                    beyondViewportPageCount = 1,
                    modifier = Modifier.fillMaxWidth()
                ) { page ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coordinates ->
                                val heightDp = with(density) { coordinates.size.height.toDp() }
                                if (pageHeights[page] != heightDp) {
                                    pageHeights[page] = heightDp
                                }
                            }
                            .verticalScroll(rememberScrollState())
                    ) {
                        CourseDetailItemContent(
                            model = uiModels[page],
                            classIcon = classIcon,
                            personIcon = personIcon,
                            locationIcon = locationIcon,
                            calendarIcon = calendarIcon,
                            scheduleIcon = scheduleIcon,
                            creditIcon = creditIcon,
                            noteIcon = noteIcon,
                            editIcon = editIcon,
                            editA11yText = editA11yText,
                            onEditClick = onEditClick
                        )
                    }
                }
            }
            if (uiModels.size > 1) {
                WormPagerIndicator(
                    pagerState = pagerState,
                    pageCount = uiModels.size,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = if (isCoui) 8.dp else 36.dp)
                )
            }
        }
    }
}

/**
 * 胶囊拉伸分页指示器组件
 * 支持手指滑动选择和点击跳转
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WormPagerIndicator(
    pagerState: PagerState,
    pageCount: Int,
    modifier: Modifier = Modifier,
    dotSize: Dp = 6.dp,
    spacing: Dp = 8.dp,
    activeColor: Color = uiStyleColors().accent,
    inactiveColor: Color = uiStyleColors().secondaryText.copy(alpha = 0.3f)
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var dragSelectedPage by remember { mutableStateOf<Int?>(null) }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .semantics {
                    progressBarRangeInfo = ProgressBarRangeInfo(
                        pagerState.currentPage.toFloat(), 0f..(pageCount - 1).toFloat(), pageCount - 2
                    )
                    setProgress { target ->
                        scope.launch {
                            pagerState.animateScrollToPage(target.roundToInt().coerceIn(0, pageCount - 1))
                        }
                        true
                    }
                }
                .drawWithContent {
                    drawContent()

                    val dotSizePx = dotSize.toPx()
                    val stepPx = dotSizePx + spacing.toPx()
                    val currentPage = dragSelectedPage ?: pagerState.currentPage
                    val fraction = if (dragSelectedPage != null) 0f else pagerState.currentPageOffsetFraction

                    val startDot = if (fraction >= 0) currentPage else currentPage - 1
                    val progress = if (fraction >= 0) fraction else 1f + fraction

                    val headProgress = (progress * 2f).coerceAtMost(1f)
                    val tailProgress = ((progress - 0.5f) * 2f).coerceAtLeast(0f)

                    val leftX = startDot * stepPx + tailProgress * stepPx
                    val rightX = startDot * stepPx + dotSizePx + headProgress * stepPx

                    drawRoundRect(
                        color = activeColor,
                        topLeft = Offset(x = leftX, y = (size.height - dotSizePx) / 2f),
                        size = Size(width = (rightX - leftX).coerceAtLeast(dotSizePx), height = dotSizePx),
                        cornerRadius = CornerRadius(dotSizePx / 2f, dotSizePx / 2f)
                    )
                }
                .pointerInput(pageCount) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val stepPx = with(density) { (dotSize + spacing).toPx() }
                        val startPage = (down.position.x / stepPx).roundToInt().coerceIn(0, pageCount - 1)
                        dragSelectedPage = startPage

                        scope.launch {
                            pagerState.scrollToPage(startPage)
                        }

                        while (true) {
                            val event = awaitPointerEvent()
                            val position = event.changes.firstOrNull()?.position ?: break

                            if (event.changes.firstOrNull()?.pressed != true) {
                                dragSelectedPage = null
                                break
                            }

                            val selectedPage = (position.x / stepPx).roundToInt().coerceIn(0, pageCount - 1)
                            if (selectedPage != dragSelectedPage) {
                                dragSelectedPage = selectedPage
                                scope.launch {
                                    pagerState.scrollToPage(selectedPage)
                                }
                            }
                        }
                    }
                }
                .padding(vertical = 21.dp)
        ) {
            repeat(pageCount) { _ ->
                Box(
                    modifier = Modifier
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(inactiveColor)
                )
            }
        }
    }
}

/**
 * 课程详情内容组件
 */
@Composable
private fun CourseDetailItemContent(
    model: CourseDetailUIModel,
    classIcon: ImageVector,
    personIcon: ImageVector,
    locationIcon: ImageVector,
    calendarIcon: ImageVector,
    scheduleIcon: ImageVector,
    creditIcon: ImageVector,
    noteIcon: ImageVector,
    editIcon: ImageVector,
    editA11yText: String,
    onEditClick: (String) -> Unit
) {
    val isCoui = LocalUiStyle.current == UiStyle.COUI
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(if (isCoui) 0.dp else 16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (!isCoui) {
                    Icon(classIcon, null, tint = uiStyleColors().accent, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                }
                UiText(
                    model.name,
                    modifier = Modifier.weight(1f).padding(end = if (isCoui) 8.dp else 60.dp),
                    style = MaterialTheme.typography.titleLarge,
                    color = uiStyleColors().onSurface,
                )
                if (isCoui) UiCourseIconButton(editIcon, editA11yText, tint = uiStyleColors().accent) {
                    onEditClick(model.id)
                }
            }

            model.number?.let { DetailItem(classIcon, "课程序号：$it") }

            if (model.teacher.isNotBlank()) {
                DetailItem(personIcon, model.teacher)
            }

            if (model.position.isNotBlank()) {
                DetailItem(locationIcon, model.position)
            }

            if (model.weeksDisplayStr.isNotEmpty()) {
                DetailItem(calendarIcon, model.weeksDisplayStr)
            }

            if (isCoui) UiSettingRow(
                title = model.dayStr,
                summary = model.timeStr,
                leadingContent = { Icon(scheduleIcon, null, tint = uiStyleColors().secondaryText) },
            ) else DetailItem(scheduleIcon) {
                Column {
                    UiText(model.dayStr, style = MaterialTheme.typography.bodyLarge, color = uiStyleColors().secondaryText)
                    UiText(model.timeStr, style = MaterialTheme.typography.bodyMedium, color = uiStyleColors().secondaryText.copy(alpha = 0.7f))
                }
            }

            if (model.credit != null) {
                val creditText = if (model.credit % 1f == 0f) {
                    "${model.credit.toInt()}"
                } else {
                    "${model.credit}"
                }
                DetailItem(creditIcon, "${stringResource(Res.string.label_credit)}：$creditText")
            }

            if (!model.remark.isNullOrBlank()) {
                DetailItem(noteIcon, model.remark)
            }
        }

        if (!isCoui) FilledIconButton(
            onClick = { onEditClick(model.id) },
            modifier = Modifier.align(Alignment.TopEnd).size(48.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = uiStyleColors().accent.copy(alpha = 0.16f),
                contentColor = uiStyleColors().onSurface
            )
        ) {
            Icon(editIcon, contentDescription = editA11yText, modifier = Modifier.size(20.dp))
        }
    }
}

/**
 * 详情项组件（带图标和文本）
 */
@Composable
private fun DetailItem(icon: ImageVector, text: String) {
    if (LocalUiStyle.current == UiStyle.COUI) UiSettingRow(
        title = text,
        leadingContent = { Icon(icon, null, tint = uiStyleColors().secondaryText) },
    ) else DetailItem(icon = icon) {
        UiText(text, style = MaterialTheme.typography.bodyLarge, color = uiStyleColors().secondaryText)
    }
}

/**
 * 详情项组件（带图标和自定义内容）
 */
@Composable
private fun DetailItem(icon: ImageVector, content: @Composable () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = uiStyleColors().secondaryText.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        content()
    }
}

/**
 * 格式化周数显示
 * 支持单双周、连续周和混合周的显示
 */
private fun formatWeeks(weeks: List<Int>, singleLabel: String, doubleLabel: String): String {
    if (weeks.isEmpty()) return ""
    val sorted = weeks.distinct().sorted()
    val result = mutableListOf<String>()

    var i = 0
    while (i < sorted.size) {
        // 识别等差序列（单双周）
        if (i + 1 < sorted.size && sorted[i + 1] - sorted[i] == 2) {
            var k = i
            while (k + 1 < sorted.size && sorted[k + 1] - sorted[k] == 2) {
                k++
            }
            val suffix = if (sorted[i] % 2 != 0) singleLabel else doubleLabel
            result.add("${sorted[i]}-${sorted[k]}($suffix)")
            i = k + 1
        }
        // 识别连续区间
        else if (i + 1 < sorted.size && sorted[i + 1] == sorted[i] + 1) {
            val start = sorted[i]
            var k = i
            while (k + 1 < sorted.size && sorted[k + 1] == sorted[k] + 1) {
                k++
            }
            result.add("${start}-${sorted[k]}")
            i = k + 1
        }
        // 孤立的周次
        else {
            result.add("${sorted[i]}")
            i++
        }
    }
    return result.joinToString(", ")
}
