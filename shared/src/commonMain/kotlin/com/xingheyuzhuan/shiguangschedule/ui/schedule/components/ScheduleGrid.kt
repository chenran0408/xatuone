package com.xingheyuzhuan.shiguangschedule.ui.schedule.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import com.xingheyuzhuan.shiguangschedule.ui.components.uiStyleColors
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.xingheyuzhuan.shiguangschedule.data.model.UiStyle
import com.xingheyuzhuan.shiguangschedule.ui.components.LocalUiStyle
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.ScheduleModeProto
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringArrayResource
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.week_days_short_names
import kotlin.math.roundToInt
import kotlin.math.ceil
import kotlin.math.abs
import kotlin.math.sign
import kotlin.time.Duration.Companion.milliseconds

// Row distributes the leftover pixels from equal-weight weekday headers starting at the first cell.
internal fun scheduleColumnEdge(width: Int, count: Int, index: Int): Int {
    val columnWidth = (width.toFloat() / count).roundToInt()
    val remainder = width - columnWidth * count
    return columnWidth * index + remainder.sign * minOf(index, abs(remainder))
}

@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
@Composable
fun ScheduleGrid(
    state: ScheduleGridState,
    viewState: ScheduleGridViewState,
    actions: ScheduleGridActions,
    style: ScheduleGridStyleComposed,
    modifier: Modifier = Modifier
) {
    val currentActions by rememberUpdatedState(actions)
    Box(modifier.fillMaxSize()) {
        val density = LocalDensity.current

        LaunchedEffect(viewState.mergedCourses) {
            state.resetAllStates()
            currentActions.onHoldStateChanged(false)
        }

        val pageTextColor = style.pageTextColor ?: uiStyleColors().onSurface
        val pageSubTextColor = pageTextColor.copy(alpha = 0.7f)
        val weekDays = stringArrayResource(Res.array.week_days_short_names).toList()
        val visibleDays = remember(viewState.firstDayOfWeek, viewState.showWeekends) {
            visibleWeekdays(viewState.firstDayOfWeek, viewState.showWeekends)
        }
        val displayDays = visibleDays.map { weekDays[it - 1] }
        val dateOffsets = visibleDays.map { (it - viewState.firstDayOfWeek + 7) % 7 }
        val displayDates = dateOffsets.mapNotNull { viewState.dates.getOrNull(it) }
        val todayColumn = dateOffsets.indexOf(viewState.todayIndex)

        val displayDaysCount = displayDays.size
        val is24HourMode = style.scheduleMode == ScheduleModeProto.TIME_24H_MODE
        val courseEndSection = viewState.mergedCourses.maxOfOrNull { ceil(it.endSection).toInt() } ?: 0
        val maxGridSections = if (is24HourMode) 24 else maxOf(viewState.timeSlots.size, courseEndSection)

        val sectionHeightPx = with(density) { style.sectionHeight.roundToPx().toFloat() }
        val totalGridHeight = with(density) { (sectionHeightPx * maxGridSections).toDp() }
        val uiStyle = LocalUiStyle.current
        val palette = uiStyleColors()
        val gridLineColor = palette.secondaryText.copy(alpha = if (uiStyle == UiStyle.MIUIX) .16f else .20f)
        val columnLineColor = if (uiStyle == UiStyle.MIUIX) gridLineColor.copy(alpha = .08f) else gridLineColor
        val todayColumnColor = palette.accent.copy(alpha = if (uiStyle == UiStyle.MIUIX) .045f else .035f)
        val strokeWidthPx = 1f

        val singleSchedulables = remember(viewState.mergedCourses, viewState.firstDayOfWeek, viewState.showWeekends) {
            calculateSingleSchedulables(viewState.mergedCourses, visibleDays)
        }

        var activeDragHour by remember { mutableStateOf<Int?>(null) }
        var activeDragMinuteStr by remember { mutableStateOf<String?>(null) }

        if (is24HourMode && state.expandedItem != null) {
            val currentTargetSection = when {
                state.isTopHandleDragging -> {
                    val minGap = 0.25f
                    val deltaSection = state.topHandleDragOffsetY / sectionHeightPx
                    var proposedStart = state.expandedItem!!.startSection + deltaSection
                    proposedStart = (proposedStart / 0.25f).roundToInt() * 0.25f
                    proposedStart.coerceIn(0f, state.expandedItem!!.endSection - minGap)
                }
                state.isBottomHandleDragging -> {
                    val minGap = 0.25f
                    val deltaSection = state.bottomHandleDragOffsetY / sectionHeightPx
                    var proposedEnd = state.expandedItem!!.endSection + deltaSection
                    proposedEnd = (proposedEnd / 0.25f).roundToInt() * 0.25f
                    proposedEnd.coerceIn(state.expandedItem!!.startSection + minGap, maxGridSections.toFloat())
                }
                state.activeMoveIntent != null -> {
                    val duration = state.activeMoveIntent!!.duration
                    var targetStart = state.activeMoveIntent!!.initialStartSection + (state.bodyDragOffsetY / sectionHeightPx)
                    targetStart = (targetStart / 0.25f).roundToInt() * 0.25f
                    targetStart.coerceIn(0f, maxGridSections - duration)
                }
                else -> null
            }

            if (currentTargetSection != null) {
                val hour = kotlin.math.floor(currentTargetSection).toInt().coerceIn(0, maxGridSections - 1)
                val minuteFraction = currentTargetSection - kotlin.math.floor(currentTargetSection)
                val minute = (minuteFraction * 60).roundToInt() % 60

                activeDragHour = hour
                activeDragMinuteStr = minute.toString().padStart(2, '0')
            } else {
                activeDragHour = null
                activeDragMinuteStr = null
            }
        } else {
            activeDragHour = null
            activeDragMinuteStr = null
        }

        // 自动边缘滚动监测线程
        LaunchedEffect(state.isEditingActive) {
            if (state.isEditingActive) {
                while (true) {
                    val item = state.expandedItem ?: break
                    val threshold = with(density) { 40.dp.toPx() }
                    val scrollSpeed = with(density) { 8.dp.toPx() }

                    var relativeTop: Float? = null
                    var relativeBottom: Float? = null

                    if (state.activeMoveIntent != null) {
                        val currentTopInGrid = item.startSection * sectionHeightPx + state.bodyDragOffsetY
                        val currentBottomInGrid = item.endSection * sectionHeightPx + state.bodyDragOffsetY
                        relativeTop = currentTopInGrid - state.gridScrollState.value
                        relativeBottom = currentBottomInGrid - state.gridScrollState.value
                    } else if (state.isTopHandleDragging) {
                        val currentTopInGrid = item.startSection * sectionHeightPx + state.topHandleDragOffsetY
                        relativeTop = currentTopInGrid - state.gridScrollState.value
                    } else if (state.isBottomHandleDragging) {
                        val currentBottomInGrid = item.endSection * sectionHeightPx + state.bottomHandleDragOffsetY
                        relativeBottom = currentBottomInGrid - state.gridScrollState.value
                    }

                    var scrollAmount = 0f
                    if (relativeTop != null && relativeTop < threshold) {
                        if (state.gridScrollState.value > 0) {
                            scrollAmount = -scrollSpeed
                            if (state.gridScrollState.value + scrollAmount < 0) scrollAmount = -state.gridScrollState.value.toFloat()
                        }
                    } else if (relativeBottom != null && state.viewportHeightPx > 0f && relativeBottom > state.viewportHeightPx - threshold) {
                        val maxScroll = state.gridScrollState.maxValue
                        if (state.gridScrollState.value < maxScroll) {
                            scrollAmount = scrollSpeed
                            if (state.gridScrollState.value + scrollAmount > maxScroll) scrollAmount = (maxScroll - state.gridScrollState.value).toFloat()
                        }
                    }

                    if (scrollAmount != 0f) {
                        state.gridScrollState.scrollBy(scrollAmount)
                        if (state.activeMoveIntent != null) {
                            state.bodyDragOffsetY += scrollAmount
                        } else if (state.isTopHandleDragging) {
                            state.topHandleDragOffsetY += scrollAmount
                        } else if (state.isBottomHandleDragging) {
                            state.bottomHandleDragOffsetY += scrollAmount
                        }
                    }
                    delay(16.milliseconds)
                }
            }
        }

        Column(Modifier.fillMaxSize()) {
            DayHeader(style, displayDays, displayDates, viewState.currentYear, viewState.currentWeek, todayColumn, gridLineColor, pageTextColor, pageSubTextColor, strokeWidthPx)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { state.viewportHeightPx = it.height.toFloat() }
                    .verticalScroll(state = state.gridScrollState, enabled = state.expandedItem == null)
            ) {
                Row(Modifier.fillMaxWidth().height(totalGridHeight)) {
                    TimeColumn(
                        style = style, timeSlots = viewState.timeSlots, maxGridSections = maxGridSections,
                        is24HourMode = is24HourMode, onTimeSlotClicked = { currentActions.onTimeSlotClicked() },
                        modifier = Modifier.height(totalGridHeight), lineColor = gridLineColor,
                        currentSectionIndex = viewState.currentSectionIndex, currentHour = viewState.currentHour, textColor = pageTextColor,
                        subTextColor = pageSubTextColor, strokeWidthPx = strokeWidthPx,
                        activeDragHour = activeDragHour,
                        activeDragMinuteStr = activeDragMinuteStr
                    )

                    Layout(
                        content = {
                        singleSchedulables.forEach { item ->
                            val isExpanded = state.expandedItem != null && state.expandedItem?.parentBlock === item.parentBlock

                            Box(
                                modifier = Modifier
                                    .padding(style.courseBlockOuterPadding)
                                    .zIndex(if (isExpanded) 2f else 0f)
                                    .semantics(mergeDescendants = true) {
                                        role = Role.Button
                                        onClick {
                                            if (isExpanded) {
                                                state.expandedItem = null
                                                currentActions.onHoldStateChanged(false)
                                            } else {
                                                currentActions.onCourseBlockClicked(item.parentBlock)
                                            }
                                            true
                                        }
                                        onLongClick {
                                            state.expandedItem = item
                                            currentActions.onHoldStateChanged(true)
                                            true
                                        }
                                    }
                                    .then(
                                        if (!isExpanded) {
                                            Modifier.pointerInput(item) {
                                                detectTapGestures(
                                                    onPress = {
                                                        state.pressedItem = item
                                                        try {
                                                            tryAwaitRelease()
                                                        } finally {
                                                            if (state.pressedItem === item) state.pressedItem = null
                                                        }
                                                    },
                                                    onTap = { currentActions.onCourseBlockClicked(item.parentBlock) },
                                                    onLongPress = {
                                                        state.expandedItem = item
                                                        currentActions.onHoldStateChanged(true)
                                                    }
                                                )
                                            }
                                        } else {
                                            Modifier.pointerInput(item) {
                                                detectTapGestures(onTap = { state.expandedItem = null; currentActions.onHoldStateChanged(false) })
                                            }
                                        }
                                    )
                            ) {
                                CourseBlock(
                                    courseWrapper = item.courseWrapper,
                                    isVisualDemoted = item.parentBlock.isVisualDemoted,
                                    style = style,
                                    timeSlots = viewState.dayTimeSlots[item.parentBlock.day] ?: viewState.timeSlots,
                                    isFloating = isExpanded,
                                    isPressed = state.pressedItem === item,
                                    modifier = if (isExpanded) {
                                        if (!item.parentBlock.isVisualDemoted) {
                                            Modifier.pointerInput(item, state.gridWidthPx) {
                                                detectDragGestures(
                                                    onDragStart = {
                                                        state.activeMoveIntent = CourseMoveIntent(item.parentBlock, item.parentBlock.day, item.startSection, item.endSection - item.startSection)
                                                        state.bodyDragOffsetX = 0f; state.bodyDragOffsetY = 0f
                                                    },
                                                    onDragEnd = {
                                                        val intent = state.activeMoveIntent
                                                        if (intent != null && state.gridWidthPx > 0f) {
                                                            val cellWidth = state.gridWidthPx / displayDaysCount

                                                            val initialX = item.columnIndex * cellWidth + (item.subColumnIndex * (cellWidth / item.subColumnCount))

                                                            val currentAbsoluteX = initialX + state.bodyDragOffsetX
                                                            val blockWidth = cellWidth / item.subColumnCount

                                                            val strictThresholdPx = with(density) { (-6.18).dp.toPx() }

                                                            val touchLeftEdge = currentAbsoluteX <= strictThresholdPx
                                                            val touchRightEdge = (currentAbsoluteX + blockWidth) >= (state.gridWidthPx - strictThresholdPx)

                                                            if (touchLeftEdge || touchRightEdge) {
                                                                currentActions.onInitiateFloatingMode(intent.parentBlock)
                                                                state.resetAllStates()
                                                                currentActions.onHoldStateChanged(false)
                                                            } else {
                                                                val deltaCols = (state.bodyDragOffsetX / cellWidth).roundToInt()
                                                                val targetDisplayIdx = (item.columnIndex + deltaCols).coerceIn(0, displayDaysCount - 1)
                                                                val targetDay = visibleDays[targetDisplayIdx]
                                                                var targetStart = intent.initialStartSection + (state.bodyDragOffsetY / sectionHeightPx)
                                                                targetStart = if (is24HourMode) (targetStart / 0.25f).roundToInt() * 0.25f else targetStart.roundToInt().toFloat()
                                                                targetStart = targetStart.coerceIn(0f, maxGridSections - intent.duration)
                                                                val targetEnd = targetStart + intent.duration

                                                                val targetX = targetDisplayIdx * cellWidth
                                                                state.bodyDragOffsetX = targetX - initialX
                                                                state.bodyDragOffsetY = (targetStart - intent.initialStartSection) * sectionHeightPx

                                                                currentActions.onCourseMovedWithinGrid(intent.parentBlock, targetDay, targetStart, targetEnd)
                                                            }
                                                        }
                                                    },
                                                    onDragCancel = {
                                                        state.resetAllStates()
                                                        currentActions.onHoldStateChanged(false)
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        state.bodyDragOffsetX += dragAmount.x
                                                        state.bodyDragOffsetY += dragAmount.y
                                                    }
                                                )
                                            }
                                        } else {
                                            Modifier
                                        }
                                    } else Modifier
                                )

                                if (isExpanded && state.activeMoveIntent == null && !item.parentBlock.isVisualDemoted) {
                                    CourseEditHandles(
                                        onDragStart = { isTop ->
                                            if (isTop) {
                                                state.isTopHandleDragging = true
                                                state.isBottomHandleDragging = false
                                                state.topHandleDragOffsetY = 0f
                                            } else {
                                                state.isTopHandleDragging = false
                                                state.isBottomHandleDragging = true
                                                state.bottomHandleDragOffsetY = 0f
                                            }
                                        },
                                        onDragging = { deltaY ->
                                            if (state.isTopHandleDragging) {
                                                val minGap = if (is24HourMode) 0.25f else 1f
                                                val maxAllowedDragY = ((item.endSection - item.startSection) - minGap) * sectionHeightPx

                                                val tentativeOffsetY = state.topHandleDragOffsetY + deltaY
                                                if (is24HourMode) {
                                                    val proposedStart = item.startSection + (tentativeOffsetY / sectionHeightPx)
                                                    state.topHandleDragOffsetY = if (proposedStart > item.endSection - minGap) {
                                                        maxAllowedDragY
                                                    } else {
                                                        tentativeOffsetY
                                                    }
                                                } else {
                                                    state.topHandleDragOffsetY = tentativeOffsetY.coerceAtMost(maxAllowedDragY)
                                                }
                                            } else if (state.isBottomHandleDragging) {
                                                val minGap = if (is24HourMode) 0.25f else 1f
                                                val maxAllowedDragUpY = -(((item.endSection - item.startSection) - minGap) * sectionHeightPx)

                                                val tentativeOffsetY = state.bottomHandleDragOffsetY + deltaY
                                                if (is24HourMode) {
                                                    val proposedEnd = item.endSection + (tentativeOffsetY / sectionHeightPx)
                                                    state.bottomHandleDragOffsetY = if (proposedEnd < item.startSection + minGap) {
                                                        maxAllowedDragUpY
                                                    } else {
                                                        tentativeOffsetY
                                                    }
                                                } else {
                                                    state.bottomHandleDragOffsetY = tentativeOffsetY.coerceAtLeast(maxAllowedDragUpY)
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            val currentItem = state.expandedItem
                                            if (currentItem != null) {
                                                val minGap = if (is24HourMode) 0.25f else 1f
                                                var finalStart = currentItem.startSection
                                                var finalEnd = currentItem.endSection

                                                if (state.isTopHandleDragging) {
                                                    val deltaSection = state.topHandleDragOffsetY / sectionHeightPx
                                                    var proposedStart = currentItem.startSection + deltaSection
                                                    proposedStart = if (is24HourMode) (proposedStart / 0.25f).roundToInt() * 0.25f else proposedStart.roundToInt().toFloat()
                                                    finalStart = proposedStart.coerceIn(0f, finalEnd - minGap)
                                                    state.topHandleDragOffsetY = (finalStart - currentItem.startSection) * sectionHeightPx
                                                } else if (state.isBottomHandleDragging) {
                                                    val deltaSection = state.bottomHandleDragOffsetY / sectionHeightPx
                                                    var proposedEnd = currentItem.endSection + deltaSection
                                                    proposedEnd = if (is24HourMode) (proposedEnd / 0.25f).roundToInt() * 0.25f else proposedEnd.roundToInt().toFloat()
                                                    finalEnd = proposedEnd.coerceIn(finalStart + minGap, maxGridSections.toFloat())
                                                    state.bottomHandleDragOffsetY = (finalEnd - currentItem.endSection) * sectionHeightPx
                                                }
                                                if (finalStart != currentItem.startSection || finalEnd != currentItem.endSection) {
                                                    currentActions.onCourseTimeAdjusted(currentItem.parentBlock, finalStart, finalEnd)
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        },
                        modifier = Modifier
                        .height(totalGridHeight).weight(1f)
                        .clipToBounds()
                        .onSizeChanged { state.gridWidthPx = it.width.toFloat() }
                        .drawBehind {
                            val width = size.width.roundToInt()
                            if (uiStyle != UiStyle.MATERIAL && todayColumn in displayDays.indices) {
                                val left = scheduleColumnEdge(width, displayDaysCount, todayColumn)
                                val right = scheduleColumnEdge(width, displayDaysCount, todayColumn + 1)
                                drawRect(todayColumnColor, Offset(left.toFloat(), 0f), Size((right - left).toFloat(), size.height))
                            }
                            if (style.hideGridLines) return@drawBehind
                            for (i in 1..displayDaysCount) {
                                val x = scheduleColumnEdge(width, displayDaysCount, i).toFloat()
                                drawLine(columnLineColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = strokeWidthPx)
                            }
                            for (i in 1..maxGridSections) {
                                val y = i * sectionHeightPx
                                drawLine(gridLineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = strokeWidthPx)
                            }
                        }
                        .pointerInput(displayDaysCount, sectionHeightPx, viewState.firstDayOfWeek, maxGridSections, is24HourMode, state.expandedItem) {
                            detectTapGestures { offset ->
                                if (state.expandedItem != null) {
                                    state.expandedItem = null
                                    currentActions.onHoldStateChanged(false)
                                    return@detectTapGestures
                                }
                                val dayIdx = (1 until displayDaysCount).count { offset.x >= scheduleColumnEdge(size.width, displayDaysCount, it) }
                                val secIdx = (offset.y / sectionHeightPx).toInt().coerceIn(0, maxGridSections - 1)
                                currentActions.onGridCellClicked(visibleDays[dayIdx], if (is24HourMode) secIdx else (secIdx + 1))
                            }
                        }
                    ) { measurables, constraints ->
                    val edges = IntArray(displayDaysCount + 1) { scheduleColumnEdge(constraints.maxWidth, displayDaysCount, it) }
                    val minGapPx = if (is24HourMode) 0f else with(density) { 30.dp.toPx() }

                    val placeables = measurables.mapIndexed { index, measurable ->
                        val item = singleSchedulables[index]
                        val isExpanded = state.expandedItem != null && state.expandedItem?.parentBlock === item.parentBlock

                        val originalHeightPx = (item.endSection * sectionHeightPx).roundToInt() - (item.startSection * sectionHeightPx).roundToInt()

                        var calculatedHeightPx = originalHeightPx
                        if (isExpanded) {
                            if (state.isTopHandleDragging || state.topHandleDragOffsetY != 0f) {
                                calculatedHeightPx = (originalHeightPx - state.topHandleDragOffsetY.roundToInt()).coerceAtLeast(minGapPx.toInt())
                            } else if (state.isBottomHandleDragging || state.bottomHandleDragOffsetY != 0f) {
                                calculatedHeightPx = (originalHeightPx + state.bottomHandleDragOffsetY.roundToInt()).coerceAtLeast(minGapPx.toInt())
                            }
                        }

                        val cellWidth = edges[item.columnIndex + 1] - edges[item.columnIndex]
                        val blockWidth = if (isExpanded) cellWidth else
                            scheduleColumnEdge(cellWidth, item.subColumnCount, item.subColumnIndex + 1) -
                                scheduleColumnEdge(cellWidth, item.subColumnCount, item.subColumnIndex)
                        measurable.measure(
                            Constraints.fixed(
                                blockWidth,
                                calculatedHeightPx
                            )
                        )
                    }

                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeables.forEachIndexed { index, placeable ->
                            val item = singleSchedulables[index]
                            val isExpanded = state.expandedItem != null && state.expandedItem?.parentBlock === item.parentBlock
                            val isMoving = state.activeMoveIntent != null && state.activeMoveIntent?.parentBlock === item.parentBlock

                            val cellLeft = edges[item.columnIndex]
                            val cellWidth = edges[item.columnIndex + 1] - cellLeft
                            val originalX = cellLeft + (if (isExpanded) 0 else scheduleColumnEdge(cellWidth, item.subColumnCount, item.subColumnIndex))
                            val originalY = (item.startSection * sectionHeightPx).roundToInt()

                            var xPosition = originalX
                            var yPosition = originalY

                            if (isMoving) {
                                val intent = state.activeMoveIntent
                                if (intent != null) {
                                    val intentOriginalX = cellLeft
                                    val intentOriginalY = (intent.initialStartSection * sectionHeightPx).toInt()
                                    xPosition = (intentOriginalX + state.bodyDragOffsetX).toInt()
                                    yPosition = (intentOriginalY + state.bodyDragOffsetY).toInt()
                                }
                            } else if (isExpanded) {
                                if (state.isTopHandleDragging || state.topHandleDragOffsetY != 0f) {
                                    val originalHeightPx = ((item.endSection - item.startSection) * sectionHeightPx).toInt()
                                    yPosition = if (originalHeightPx - state.topHandleDragOffsetY >= minGapPx) {
                                        (originalY + state.topHandleDragOffsetY).toInt()
                                    } else {
                                        (originalY + (originalHeightPx - minGapPx)).toInt()
                                    }
                                }
                            }

                            placeable.placeRelative(xPosition, yPosition)
                        }
                    }
                    }
                }
            }
        }
    }
}
