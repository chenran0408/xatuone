package com.xingheyuzhuan.shiguangschedule.ui.schedule.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import com.xingheyuzhuan.shiguangschedule.data.model.UiStyle
import com.xingheyuzhuan.shiguangschedule.ui.components.LocalUiStyle
import com.xingheyuzhuan.shiguangschedule.ui.components.UiText
import com.xingheyuzhuan.shiguangschedule.ui.components.UiScheduleMarker
import com.xingheyuzhuan.shiguangschedule.ui.components.uiStyleColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedule.ui.schedule.model.MergedCourseBlock
import org.jetbrains.compose.resources.stringResource
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.date_format_month_day
import shiguangschedule.shared.generated.resources.schedule_header_month
import shiguangschedule.shared.generated.resources.widget_title_today
import kotlin.math.roundToInt

/**
 * 课表项统一调度接口
 */
interface ISingleSchedulable {
    val columnIndex: Int
    val subColumnIndex: Int
    val subColumnCount: Int
    val startSection: Float
    val endSection: Float
    val courseWrapper: CourseWithWeeks
    val parentBlock: MergedCourseBlock
}

/**
 * 核心设计：移动意图（Move Intent）
 */
data class CourseMoveIntent(
    val parentBlock: MergedCourseBlock,     // 原始被拖拽的块
    val initialDay: Int,                   // 拖拽前是周几 (1..7)
    val initialStartSection: Float,        // 拖拽前的起始节次/时间
    val duration: Float                    // 课程跨越的时间/节次长度 (end - start)
)

/**
 * 1. 数据收拢：外部向课表传递的纯展示数据与基础配置
 */
@Immutable
data class ScheduleGridViewState(
    val dates: List<String>,
    val currentYear: String,
    val currentWeek: String? = null,
    val timeSlots: List<TimeSlot>,
    val mergedCourses: List<MergedCourseBlock>,
    val showWeekends: Boolean,
    val todayIndex: Int,
    val firstDayOfWeek: Int,
    val currentSectionIndex: Int = -1,
    val currentHour: Int = -1,
    val dayTimeSlots: Map<Int, List<TimeSlot>> = emptyMap()
)

/**
 * 2. 动作收拢：外部业务逻辑响应的交互回调接口
 */
interface ScheduleGridActions {
    fun onCourseBlockClicked(block: MergedCourseBlock)
    fun onGridCellClicked(day: Int, section: Int)
    fun onTimeSlotClicked()
    fun onHoldStateChanged(isHolding: Boolean) {}
    fun onCourseMovedWithinGrid(block: MergedCourseBlock, newDay: Int, newStartSection: Float, newEndSection: Float) {}
    fun onCourseTimeAdjusted(block: MergedCourseBlock, newStart: Float, newEnd: Float) {}
    fun onInitiateFloatingMode(block: MergedCourseBlock) {}
}

/**
 * 3. 运行状态收拢：课表内部手势运行时的临时状态管理类
 */
@Stable
class ScheduleGridState(
    val gridScrollState: ScrollState
) {
    var expandedItem by mutableStateOf<ISingleSchedulable?>(null)
    var pressedItem by mutableStateOf<ISingleSchedulable?>(null)

    var activeMoveIntent by mutableStateOf<CourseMoveIntent?>(null)
    var bodyDragOffsetX by mutableStateOf(0f)
    var bodyDragOffsetY by mutableStateOf(0f)

    var isTopHandleDragging by mutableStateOf(false)
    var isBottomHandleDragging by mutableStateOf(false)

    var topHandleDragOffsetY by mutableStateOf(0f)
    var bottomHandleDragOffsetY by mutableStateOf(0f)

    var gridWidthPx by mutableStateOf(0f)
    var viewportHeightPx by mutableStateOf(0f)

    val isEditingActive: Boolean
        get() = expandedItem != null && (activeMoveIntent != null || isTopHandleDragging || isBottomHandleDragging)

    fun resetAllStates() {
        expandedItem = null
        pressedItem = null
        activeMoveIntent = null
        isTopHandleDragging = false
        isBottomHandleDragging = false
        topHandleDragOffsetY = 0f
        bottomHandleDragOffsetY = 0f
        bodyDragOffsetX = 0f
        bodyDragOffsetY = 0f
    }
}

/**
 * 方便在 Composable 中创建并记住 ScheduleGridState 实例的快捷函数
 */
@Composable
fun rememberScheduleGridState(
    gridScrollState: ScrollState = rememberScrollState()
): ScheduleGridState {
    return remember(gridScrollState) {
        ScheduleGridState(gridScrollState)
    }
}

/**
 * 课表顶部日期/星期标头组件
 */
@Composable
fun DayHeader(
    style: ScheduleGridStyleComposed,
    displayDays: List<String>,
    dates: List<String>,
    currentYear: String,
    currentWeek: String?,
    todayIndex: Int,
    lineColor: Color,
    textColor: Color,
    subTextColor: Color,
    strokeWidthPx: Float
) {
    val todayTextColor = scheduleMarkerTextColor()
    val uiStyle = LocalUiStyle.current
    val isMiuix = uiStyle == UiStyle.MIUIX
    val isCoui = uiStyle == UiStyle.COUI
    val density = LocalDensity.current
    val horizontalInset = if (density.fontScale > 1.2f) 1.dp else 3.dp
    val dateMeasurer = rememberTextMeasurer()
    val dateStyle = LocalTextStyle.current.merge(TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium))
    BoxWithConstraints(Modifier.fillMaxWidth().background(scheduleRailColor())) {
        val shouldShowDate = !style.hideDateUnderDay && minOf(style.dayHeaderHeight, maxHeight) >= 42.dp
        val dateWidthPx = remember(dateMeasurer, dateStyle, dates, displayDays.size) {
            dates.take(displayDays.size).maxOfOrNull {
                dateMeasurer.measure(it, style = dateStyle, maxLines = 1).size.width
            } ?: 0
        }
        val dateWidth = with(density) { dateWidthPx.toDp() }
        val cellWidth = (maxWidth - style.timeColumnWidth) / displayDays.size.coerceAtLeast(1)
        val markerPadding = if (isMiuix && todayIndex in displayDays.indices) 8.dp else 0.dp
        val available = cellWidth - horizontalInset * 2
        val monthOnRail = shouldShowDate && (isMiuix || isCoui) && dates.isNotEmpty()
        val compactDates = shouldShowDate && (monthOnRail || available < dateWidth + markerPadding + 8.dp)
        val todayStartsMonth = todayIndex == 0 ||
            dates.getOrNull(todayIndex)?.take(2) != dates.getOrNull(todayIndex - 1)?.take(2)
        val monthDatePadding = if (todayStartsMonth) markerPadding else 0.dp
        val visibleCount = minOf(dates.size, displayDays.size)
        val monthChanges = visibleCount > 0 && dates.first().take(2) != dates[visibleCount - 1].take(2)
        val showDateRange = visibleCount > 0 && compactDates && (!monthOnRail || monthChanges) && available < dateWidth + monthDatePadding
        val rangeHeight = if (showDateRange) with(density) { 16.sp.toDp() } else 0.dp
        if (showDateRange) {
            UiText(
                text = "${dates.first()} – ${dates[visibleCount - 1]}",
                modifier = Modifier.fillMaxWidth().padding(start = style.timeColumnWidth).height(rangeHeight),
                textAlign = TextAlign.Center, color = subTextColor,
                fontSize = 11.sp, lineHeight = 13.sp, maxLines = 1,
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = rangeHeight).height(style.dayHeaderHeight)) {
            Box(
                modifier = Modifier
                    .width(style.timeColumnWidth)
                    .fillMaxHeight()
                    .drawBehind {
                        if (!style.hideGridLines) {
                            if (!isMiuix && !isCoui) {
                                drawLine(lineColor, Offset(size.width, 0f), Offset(size.width, size.height), strokeWidthPx)
                            }
                            drawLine(lineColor, Offset(0f, size.height), Offset(size.width, size.height), strokeWidthPx)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    UiText(
                        text = currentYear,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = subTextColor,
                        style = TextStyle.Default
                    )

                    val railLabel = if (monthOnRail) stringResource(Res.string.schedule_header_month, dates.first().take(2).toInt())
                        else currentWeek
                    if (!railLabel.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(1.dp))
                        UiText(
                            text = railLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Normal,
                            color = subTextColor,
                            style = TextStyle.Default
                        )
                    }
                }
            }

            val displayDaysCount = displayDays.size

            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .drawBehind {
                        if (!style.hideGridLines) {
                            if (!isMiuix && !isCoui) {
                                for (i in 1..displayDaysCount) {
                                    val x = scheduleColumnEdge(size.width.roundToInt(), displayDaysCount, i).toFloat()
                                    drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = strokeWidthPx)
                                }
                            }
                            drawLine(lineColor, Offset(0f, size.height), Offset(size.width, size.height), strokeWidthPx)
                        }
                    }
            ) {
                displayDays.forEachIndexed { index, day ->
                    val isToday = index == todayIndex
                    val date = dates.getOrNull(index)?.takeIf { shouldShowDate }
                    val markDateOnly = isMiuix && date != null
                    val cellContent: @Composable () -> Unit = {
                        Column(
                            modifier = Modifier
                                .fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            UiText(
                                text = day,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    !isToday -> textColor
                                    markDateOnly -> uiStyleColors().accent
                                    else -> todayTextColor
                                },
                                maxLines = 1,
                                lineHeight = 16.sp,
                            )

                            if (date != null) {
                                // The live grid uses MM-DD and the appearance preview uses MM/DD.
                                val showMonth = if (index == 0) !monthOnRail else date.take(2) != dates[index - 1].take(2)
                                val dateText = if (compactDates && (!showMonth || showDateRange)) date.takeLast(2) else date
                                Spacer(modifier = Modifier.height(2.dp))
                                val dateLabel: @Composable () -> Unit = {
                                    UiText(
                                        text = dateText,
                                        modifier = if (isCoui) Modifier.fillMaxWidth() else Modifier,
                                        textAlign = TextAlign.Center,
                                        fontSize = 11.sp,
                                        fontWeight = if (isToday) FontWeight.Medium else FontWeight.Normal,
                                        color = if (isToday) todayTextColor else subTextColor,
                                        maxLines = 1,
                                        lineHeight = 13.sp,
                                    )
                                }
                                if (markDateOnly && isToday) {
                                    UiScheduleMarker(Modifier.padding(horizontal = 1.dp), true, cornerRadius = 9.dp) {
                                        Box(Modifier.padding(horizontal = 3.dp)) {
                                            dateLabel()
                                        }
                                    }
                                } else {
                                    dateLabel()
                                }
                            }
                        }
                    }
                    val dateSemantics = if (compactDates && date != null) {
                        val spokenDate = stringResource(
                            Res.string.date_format_month_day, date.take(2).toInt(), date.takeLast(2).toInt(),
                        )
                        val description = if (isToday) {
                            "${stringResource(Res.string.widget_title_today)}, $day, $spokenDate"
                        } else "$day, $spokenDate"
                        Modifier.clearAndSetSemantics { contentDescription = description }
                    } else Modifier
                    val cellModifier = Modifier.weight(1f).fillMaxHeight().then(dateSemantics)
                        .padding(horizontal = horizontalInset, vertical = 3.dp)
                    if (isCoui) {
                        Box(cellModifier.drawBehind {
                            if (isToday) {
                                val markerWidth = 14.dp.toPx()
                                val y = size.height - 1.dp.toPx()
                                drawLine(todayTextColor, Offset(center.x - markerWidth / 2, y),
                                    Offset(center.x + markerWidth / 2, y), strokeWidth = 2.dp.toPx(),
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round)
                            }
                        }) { cellContent() }
                    } else if (markDateOnly) {
                        Box(cellModifier) { cellContent() }
                    } else {
                        UiScheduleMarker(cellModifier, isToday, cornerRadius = 12.dp) { cellContent() }
                    }
                }
            }
        }
    }
}

/**
 * 课表左侧时间轴/节次轴组件
 */
@Composable
fun TimeColumn(
    style: ScheduleGridStyleComposed,
    timeSlots: List<TimeSlot>,
    maxGridSections: Int,
    is24HourMode: Boolean,
    onTimeSlotClicked: () -> Unit,
    modifier: Modifier,
    lineColor: Color,
    currentSectionIndex: Int = -1,
    currentHour: Int = -1,
    textColor: Color,
    subTextColor: Color,
    strokeWidthPx: Float,
    activeDragHour: Int? = null,
    activeDragMinuteStr: String? = null
) {
    val currentSectionTextColor = scheduleMarkerTextColor()
    Column(modifier.width(style.timeColumnWidth).background(scheduleRailColor())) {
        for (index in 0 until maxGridSections) {
            val isCurrentHourActive = if (is24HourMode) {
                currentHour == index
            } else {
                index + 1 == currentSectionIndex
            }

            val sectionContent: @Composable BoxScope.() -> Unit = {
                val h = style.sectionHeight
                if (is24HourMode && activeDragMinuteStr != null && index == activeDragHour) {
                    Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                        UiText(text = ":$activeDragMinuteStr", fontSize = if (h < 32.dp) 11.sp else 12.sp,
                            fontWeight = FontWeight.Bold, color = textColor)
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = if (is24HourMode) Arrangement.Top else Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 1.dp).then(if (is24HourMode) Modifier.offset(y = (-7).dp) else Modifier)
                ) {
                    if (is24HourMode) {
                        val formatHourStr = "${index.toString().padStart(2, '0')}:00"
                        UiScheduleMarker(
                            modifier = Modifier.padding(horizontal = 1.dp),
                            selected = isCurrentHourActive,
                            cornerRadius = 8.dp,
                        ) {
                            UiText(formatHourStr, fontSize = if (h < 32.dp) 11.sp else 12.sp,
                                fontWeight = FontWeight.Medium, color = if (isCurrentHourActive) currentSectionTextColor else textColor,
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp))
                        }
                    } else {
                        val slot = timeSlots.getOrNull(index)
                        if (slot != null) {
                            UiScheduleMarker(
                                modifier = Modifier.padding(horizontal = 1.dp),
                                selected = isCurrentHourActive,
                                cornerRadius = 8.dp,
                            ) {
                                UiText(
                                    text = slot.alias ?: slot.number.toString(),
                                    fontSize = if (h < 32.dp) 11.sp else 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrentHourActive) currentSectionTextColor else textColor,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp),
                                )
                            }
                            if (!style.hideSectionTime) {
                                when {
                                    h >= 52.dp -> {
                                        Spacer(Modifier.height(2.dp))
                                        TimeText(slot.startTime, subTextColor)
                                        TimeText(slot.endTime, subTextColor)
                                    }
                                    h >= 38.dp -> UiText(text = "${slot.startTime}-${slot.endTime}", fontSize = 8.sp, color = subTextColor, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(style.sectionHeight)
                    .clickable { onTimeSlotClicked() }
                    .drawBehind {
                        if (!style.hideGridLines) {
                            drawLine(lineColor, Offset(size.width, 0f), Offset(size.width, size.height), strokeWidthPx)
                            if (!is24HourMode) {
                                drawLine(lineColor, Offset(0f, size.height), Offset(size.width, size.height), strokeWidthPx)
                            }
                        }
                    },
                contentAlignment = if (is24HourMode) Alignment.TopCenter else Alignment.Center
            ) { sectionContent() }
        }
    }
}

@Composable
private fun scheduleMarkerTextColor() = when (LocalUiStyle.current) {
    UiStyle.MATERIAL, UiStyle.MIUIX -> MaterialTheme.colorScheme.onPrimaryContainer
    UiStyle.COUI -> uiStyleColors().accent
}

@Composable
private fun scheduleRailColor() = when (LocalUiStyle.current) {
    UiStyle.MATERIAL -> MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = .76f)
    UiStyle.MIUIX -> uiStyleColors().surface.copy(alpha = .88f)
    UiStyle.COUI -> uiStyleColors().surface
}

/** 课表编辑状态下的拉伸手柄（上下边界调整） */
@Composable
fun BoxScope.CourseEditHandles(
    onDragStart: (isTop: Boolean) -> Unit,
    onDragging: (deltaY: Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val handleBoxSize = 32.dp

    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .offset(x = (-16).dp, y = (-16).dp)
            .size(handleBoxSize)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { onDragStart(true) },
                    onDragEnd = { onDragEnd() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDragging(dragAmount.y)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        EditHandleDot()
    }

    Box(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .offset(x = 16.dp, y = 16.dp)
            .size(handleBoxSize)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { onDragStart(false) },
                    onDragEnd = { onDragEnd() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDragging(dragAmount.y)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        EditHandleDot()
    }
}

@Composable
private fun EditHandleDot() {
    Box(
        modifier = Modifier
            .size(12.dp)
            .background(uiStyleColors().accent, CircleShape)
    )
}

@Composable
fun TimeText(text: String, color: Color) {
    UiText(text = text, fontSize = 10.sp, color = color, lineHeight = 1.em, maxLines = 1)
}

/**
 * 计算扁平化的可调度项列表
 */
fun calculateSingleSchedulables(
    mergedCourses: List<MergedCourseBlock>,
    visibleDays: List<Int>
): List<ISingleSchedulable> {
    val list = mutableListOf<ISingleSchedulable>()
    mergedCourses.forEach { block ->
        val displayIdx = visibleDays.indexOf(block.day)
        if (displayIdx != -1) {
            val layoutInfo = block.nonActiveRanges.firstOrNull() ?: (0f to 1f)
            block.courses.firstOrNull()?.let { course ->
                list.add(object : ISingleSchedulable {
                    override val columnIndex = displayIdx
                    override val subColumnIndex = layoutInfo.first.toInt()
                    override val subColumnCount = layoutInfo.second.toInt()
                    override val startSection = block.startSection
                    override val endSection = block.endSection
                    override val courseWrapper = course
                    override val parentBlock = block.copy(courses = listOf(course))
                })
            }
        }
    }
    return list
}

internal fun visibleWeekdays(firstDayOfWeek: Int, showWeekends: Boolean): List<Int> =
    (0..6).map { (firstDayOfWeek - 1 + it) % 7 + 1 }.filter { showWeekends || it <= 5 }
