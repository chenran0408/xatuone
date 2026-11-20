package com.xingheyuzhuan.shiguangschedule.ui.schedule.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalTextStyle
import com.xingheyuzhuan.shiguangschedule.ui.components.UiText
import com.xingheyuzhuan.shiguangschedule.ui.components.UiScheduleCourseCard
import com.xingheyuzhuan.shiguangschedule.ui.components.uiScheduleCourseShape
import com.xingheyuzhuan.shiguangschedule.ui.components.LocalUiStyle
import com.xingheyuzhuan.shiguangschedule.data.model.UiStyle
import com.xingheyuzhuan.shiguangschedule.ui.components.uiStyleColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Constraints
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.BorderTypeProto
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.ScheduleModeProto
import com.xingheyuzhuan.shiguangschedule.ui.theme.LocalIsDarkTheme

internal fun courseSurfaceColor(style: UiStyle, courseColor: Color, surface: Color, dark: Boolean): Color {
    val tint = when (style) {
        UiStyle.MATERIAL -> .76f
        UiStyle.MIUIX -> if (dark) .52f else .32f
        UiStyle.COUI -> if (dark) .40f else .22f
    }
    return lerp(surface, courseColor.compositeOver(surface), tint)
}

internal fun courseTextOn(surface: Color, preferred: Color): Color {
    val background = surface.luminance() + .05f
    val foreground = preferred.luminance() + .05f
    if (maxOf(background, foreground) / minOf(background, foreground) >= 4.5f) return preferred
    return if (background / .05f >= 1.05f / background) Color.Black else Color.White
}

internal fun fittingCourseTime(range: String, availableWidth: Int, measureWidth: (String) -> Int): String {
    if (measureWidth(range) <= availableWidth) return range
    val start = range.substringBefore('–')
    return if (measureWidth(start) <= availableWidth) start else "…"
}

@Composable
fun CourseBlock(
    courseWrapper: CourseWithWeeks,
    isVisualDemoted: Boolean,
    style: ScheduleGridStyleComposed,
    timeSlots: List<TimeSlot>,
    modifier: Modifier = Modifier,
    isFloating: Boolean = false,
    isPressed: Boolean = false,
) {
    val course = courseWrapper.course
    val uiStyle = LocalUiStyle.current
    val isCoui = uiStyle == UiStyle.COUI
    val isMiuix = uiStyle == UiStyle.MIUIX
    val colors = uiStyleColors()
    val isDarkTheme = LocalIsDarkTheme.current
    val displayName = com.xingheyuzhuan.shiguangschedule.ui.schedule.courseDisplayName(course.name)
    val displayNumber = displayName.number.takeUnless { style.hideCourseNumber }

    // 颜色适配
    val colorIndex = course.colorInt.takeIf { it in style.courseColorMaps.indices }
    val courseColorAdapted: Color? = colorIndex?.let { index ->
        val baseColorMap = style.courseColorMaps[index]
        if (isDarkTheme) baseColorMap.dark else baseColorMap.light
    }
    val fallbackColorAdapted: Color = if (isDarkTheme) style.courseColorMaps.first().dark else style.courseColorMaps.first().light

    val currentAlpha = if (isFloating) 0.95f else style.courseBlockAlpha
    val courseColor = courseColorAdapted ?: fallbackColorAdapted
    val surface = colors.surface.compositeOver(colors.background)
    val isOutsideWeek = isVisualDemoted && !isFloating
    val courseSurface = courseSurfaceColor(uiStyle, courseColor, surface, isDarkTheme)
    val blockColor = (if (isOutsideWeek) lerp(courseSurface, surface, .65f) else courseSurface)
        .copy(alpha = currentAlpha)
    val effectiveSurface = blockColor.compositeOver(surface)
    val textColor = style.courseTextColor ?: courseTextOn(effectiveSurface, colors.onSurface)
    val labelColor = style.courseTextColor ?: courseTextOn(effectiveSurface, lerp(textColor, effectiveSurface, .15f))
    val accent = lerp(courseColor, textColor, if (isDarkTheme) .42f else .34f)

    // 字体大小
    val s13 = (13 * style.fontScale).sp
    val s10 = (10 * style.fontScale).sp

    // 核心分支逻辑：判断 24小时模式 与 节次模式 的时间文本渲染
    val customStartTime = course.customStartTime
    val customEndTime = course.customEndTime
    val customTimeString = if (customStartTime != null && customEndTime != null) "$customStartTime–$customEndTime" else null
    val isCustomTimeCourse = customTimeString != null

    val timeTextToShow = if (style.scheduleMode == ScheduleModeProto.TIME_24H_MODE) {
        // 24小时绝对时间轴模式：全部课程都显示起止时间
        if (isCustomTimeCourse) {
            customTimeString
        } else {
            val startSlot = timeSlots.find { it.number == course.startSection }
            val endSlot = timeSlots.find { it.number == course.endSection }
            if (startSlot != null && endSlot != null) "${startSlot.startTime}–${endSlot.endTime}" else null
        }
    } else {
        // 传统节次模式：只有自定义课程显示起止时间；普通节次课程只有在开启展示开始时间时才显示开始时间
        if (isCustomTimeCourse) {
            customTimeString
        } else if (style.showStartTime) {
            timeSlots.find { it.number == course.startSection }?.startTime
        } else {
            null
        }
    }

    // 边框样式配置
    val borderColor = if (isFloating) colors.accent else colors.secondaryText
    val borderWidth = if (isFloating) 2.dp else 1.dp
    val borderAlpha = if (isFloating) 1.0f else style.courseBlockAlpha
    val shape = uiScheduleCourseShape(style.courseBlockCornerRadius)

    val borderModifier = when (style.borderType) {
        BorderTypeProto.BORDER_TYPE_SOLID -> {
            Modifier.border(borderWidth, borderColor.copy(alpha = borderAlpha), shape)
        }
        BorderTypeProto.BORDER_TYPE_DASHED -> {
            Modifier.drawBehind {
                val strokeWidth = borderWidth.toPx()
                val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
                drawOutline(
                    outline = shape.createOutline(size, layoutDirection, this),
                    color = borderColor.copy(alpha = borderAlpha),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, pathEffect = dashPathEffect)
                )
            }
        }
        else -> {
            if (isFloating) Modifier.border(borderWidth, borderColor, shape) else Modifier
        }
    }

    val horizontalAlignment = if (style.textAlignCenterHorizontal) Alignment.CenterHorizontally else Alignment.Start
    val verticalArrangement = if (style.textAlignCenterVertical) Arrangement.Center else Arrangement.Top
    val textAlign = if (style.textAlignCenterHorizontal) TextAlign.Center else TextAlign.Start

    // 选中捏起时，增加三维物理阴影
    val floatingShadowModifier = if (isFloating) {
        Modifier.shadow(elevation = 8.dp, shape = shape, clip = false)
    } else {
        Modifier
    }

    UiScheduleCourseCard(
        modifier = modifier.then(floatingShadowModifier).fillMaxSize().clip(shape),
        cornerRadius = style.courseBlockCornerRadius,
        containerColor = blockColor,
        raised = isFloating,
        pressed = isPressed,
    ) {
        val outsideWeekMark = if (isOutsideWeek) Modifier.drawWithCache {
            val step = 5.dp.toPx()
            val ink = colors.onSurface.copy(alpha = .06f * currentAlpha)
            val stripe = Brush.linearGradient(0f to ink, .45f to ink, .55f to Color.Transparent, 1f to Color.Transparent,
                start = Offset.Zero, end = Offset(step, step), tileMode = TileMode.Repeated)
            onDrawBehind { drawRect(stripe) }
        } else Modifier
        BoxWithConstraints(Modifier.fillMaxSize().clip(shape).then(outsideWeekMark).then(borderModifier)) {
            val inset = style.courseBlockInnerPadding
            val startInset = if (isCoui) maxOf(inset, 3.dp) else inset
            val topInset = if (isMiuix) inset + 5.dp else inset
            val density = LocalDensity.current
            val innerWidth = with(density) { (maxWidth - startInset - inset).roundToPx().coerceAtLeast(0) }
            val innerHeight = maxHeight - topInset - inset
            val titleStyle = LocalTextStyle.current.copy(fontSize = s13, lineHeight = 1.2.em,
                fontWeight = if (isMiuix || isCoui) FontWeight.SemiBold else FontWeight.Bold)
            val timeStyle = LocalTextStyle.current.copy(fontSize = s10, lineHeight = 1.em, fontWeight = FontWeight.SemiBold)
            var titleModifier: Modifier = Modifier
            val timeLabel = timeTextToShow?.let { range ->
                val measurer = rememberTextMeasurer()
                val timeSize = measurer.measure(range, style = timeStyle, maxLines = 1).size
                val titleHeight = measurer.measure(displayName.title, style = titleStyle, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, constraints = Constraints(maxWidth = innerWidth)).size.height
                val titleSpace = innerHeight - with(density) { timeSize.height.toDp() }
                val timeOnly = with(density) { titleHeight > titleSpace.roundToPx() }
                val label = fittingCourseTime(range, if (timeOnly) constraints.maxWidth else innerWidth) {
                    if (it == range) timeSize.width else measurer.measure(it, style = timeStyle, maxLines = 1).size.width
                }
                if (timeOnly) {
                    UiText(label, style = timeStyle, color = textColor, maxLines = 1,
                        modifier = Modifier.align(Alignment.Center).clearAndSetSemantics {
                            contentDescription = "${displayName.title}, $range"
                        })
                    return@BoxWithConstraints
                }
                titleModifier = Modifier.heightIn(max = titleSpace)
                label
            }
            Column(
                modifier = Modifier.fillMaxSize().padding(start = startInset, top = topInset, end = inset, bottom = inset),
                horizontalAlignment = horizontalAlignment,
                verticalArrangement = verticalArrangement
            ) {
                UiText(
                    text = displayName.title,
                    modifier = titleModifier,
                    color = textColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = textAlign,
                    style = titleStyle,
                )

                if (timeLabel != null && timeTextToShow != null) {
                    UiText(
                        text = timeLabel,
                        color = labelColor,
                        modifier = Modifier.clearAndSetSemantics { contentDescription = timeTextToShow },
                        textAlign = textAlign,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = timeStyle
                    )
                }

                if (!style.hideLocation) {
                    val position = course.position
                    if (position.isNotBlank()) {
                        val prefix = if (style.removeLocationAt) "" else "@"
                        UiText(text = "$prefix$position", fontSize = s10, fontWeight = FontWeight.Medium, color = labelColor,
                            textAlign = textAlign, maxLines = 1, overflow = TextOverflow.Ellipsis, style = TextStyle(lineHeight = 1.em))
                    }
                }
                if (!style.hideTeacher && course.teacher.isNotBlank()) {
                    UiText(text = course.teacher, fontSize = s10, fontWeight = FontWeight.Normal, color = labelColor,
                        textAlign = textAlign, maxLines = 1, overflow = TextOverflow.Ellipsis, style = TextStyle(lineHeight = 1.em))
                }
                if (displayNumber != null && innerHeight >= 96.dp) {
                    UiText(text = displayNumber, fontSize = s10, color = labelColor,
                        textAlign = textAlign, maxLines = 1, overflow = TextOverflow.Ellipsis, style = TextStyle(lineHeight = 1.em))
                }
            }
            if (isCoui) {
                Box(
                    Modifier.width(2.dp).fillMaxHeight().align(Alignment.CenterStart)
                        .background(accent.copy(alpha = currentAlpha))
                )
            }
            if (isMiuix) {
                Box(
                    Modifier.align(Alignment.TopStart)
                        .padding(start = inset, top = inset)
                        .width(16.dp).height(3.dp)
                        .background(accent.copy(alpha = currentAlpha), CircleShape)
                )
            }
        }
    }
}
