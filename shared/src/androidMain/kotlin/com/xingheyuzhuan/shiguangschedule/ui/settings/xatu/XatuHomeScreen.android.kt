package com.xingheyuzhuan.shiguangschedule.ui.settings.xatu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xingheyuzhuan.shiguangschedule.Destination
import com.xingheyuzhuan.shiguangschedule.data.model.UiStyle
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuCampusSnapshot
import com.xingheyuzhuan.shiguangschedule.ui.components.*
import com.xingheyuzhuan.shiguangschedule.ui.today.*
import com.xingheyuzhuan.shiguangschedule.xatu.academic.startDateTime
import com.xingheyuzhuan.shiguangschedule.xatu.campus.recentXatuElectricityChange
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import java.time.LocalDateTime
import java.time.ZoneOffset
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.chevron_right_24px

internal fun homeCourseAt(courses: List<CourseDisplayModel>, time: String): CourseDisplayModel? {
    val now = kotlinx.datetime.LocalTime.parse(time)
    val timed = courses.map { Triple(it, courseTime(it.startTime), courseTime(it.endTime)) }
    return timed.firstOrNull { (_, start, end) -> start != null && end != null && now >= start && now <= end }?.first
        ?: timed.firstOrNull { (_, start, _) -> start != null && start > now }?.first
        ?: timed.firstOrNull { (_, start, end) -> start == null || end == null }?.first
}

private fun courseTime(value: String?): kotlinx.datetime.LocalTime? = value?.let { runCatching { kotlinx.datetime.LocalTime.parse(it) }.getOrNull() }

@Composable
actual fun XatuHomeScreen(onNavigate: (Destination) -> Unit, onBack: () -> Unit, navigationPadding: PaddingValues) {
    val courseViewModel: TodayScheduleViewModel = koinViewModel()
    val today by courseViewModel.uiState.collectAsStateWithLifecycle()
    val overviewViewModel: XatuHomeViewModel = koinViewModel()
    val overview by overviewViewModel.uiState.collectAsStateWithLifecycle()
    val campus by overviewViewModel.campusSnapshot.collectAsStateWithLifecycle()
    val electricity by overviewViewModel.electricityState.collectAsStateWithLifecycle()
    var detailCourseId by rememberSaveable { mutableStateOf<String?>(null) }
    var now by remember { mutableStateOf(LocalDateTime.now(ZoneOffset.ofHours(8))) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now(ZoneOffset.ofHours(8))
            delay(60_000L - now.second * 1000L - now.nano / 1_000_000L)
        }
    }
    LaunchedEffect(now.toLocalDate()) { overviewViewModel.onDateChanged(now.toLocalDate()) }
    val fontScale = LocalDensity.current.fontScale
    val style = LocalUiStyle.current
    val sectionSpacing = if (style == UiStyle.COUI) 0.dp else 12.dp
    val showAccountNearTop = overview.requiresLogin || overview.account?.loggedIn == false
    XatuPage("一校通", largeTitle = false) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(bottom = (navigationPadding.calculateBottomPadding() - padding.calculateBottomPadding()).coerceAtLeast(0.dp)),
            contentPadding = PaddingValues(start = if (style == UiStyle.COUI) 0.dp else 16.dp,
                end = if (style == UiStyle.COUI) 0.dp else 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(sectionSpacing)
        ) {
            item(key = "date") { HomeDate(overview, today, now) }
            item(key = "course") {
                HomeCourseCard(today, now, courseViewModel::retry, onNavigate) { detailCourseId = it.course.id }
            }
            if (showAccountNearTop) item(key = "account") { HomeAccount(overview, onNavigate) }
            item(key = "summaries") {
                BoxWithConstraints {
                    if (style == UiStyle.MATERIAL && maxWidth / fontScale >= 600.dp) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.weight(1f)) { HomeExamCard(overview, now, overviewViewModel::refresh, onNavigate) }
                            Column(Modifier.weight(1f)) { HomeElectricitySummary(campus, electricity, overviewViewModel::refreshElectricity, onNavigate) }
                        }
                    } else Column(verticalArrangement = Arrangement.spacedBy(sectionSpacing)) {
                        HomeElectricitySummary(campus, electricity, overviewViewModel::refreshElectricity, onNavigate)
                        HomeExamCard(overview, now, overviewViewModel::refresh, onNavigate)
                    }
                }
            }
            if (!showAccountNearTop) item(key = "account") { HomeAccount(overview, onNavigate) }
        }
    }
    (today as? TodayUiState.Success)?.courses?.firstOrNull { it.course.id == detailCourseId }?.let { model ->
        TodayCourseDetail(
            model = model,
            onDismiss = { detailCourseId = null },
            onEdit = {
                detailCourseId = null
                onNavigate(Destination.AddEditCourse(courseId = it))
            },
        )
    }
}

@Composable
private fun HomeDate(state: XatuHomeUiState, today: TodayUiState, now: LocalDateTime) {
    val local = today as? TodayUiState.Success
    val week = when {
        state.officialWeek != null -> "${if (state.loading || state.error != null) "上次获取：" else ""}教务第 ${state.officialWeek} 周"
        local?.status == TodayStatus.Normal -> "课表第 ${local.weekIndex} 周"
        state.loading -> "正在确认教学周…"
        else -> "教学周尚未确认"
    }
    val weekdays = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    val date = "${now.monthValue}月${now.dayOfMonth}日 · ${weekdays[now.dayOfWeek.value - 1]}"
    when (LocalUiStyle.current) {
        UiStyle.MATERIAL -> Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(date, style = MaterialTheme.typography.titleLarge)
            Text(week, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        UiStyle.MIUIX -> FlowRow(
            Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            top.yukonga.miuix.kmp.basic.Text(
                date, modifier = Modifier.align(Alignment.CenterVertically),
                style = top.yukonga.miuix.kmp.theme.MiuixTheme.textStyles.title2,
            )
            top.yukonga.miuix.kmp.basic.Text(
                week,
                modifier = Modifier.align(Alignment.CenterVertically)
                    .background(top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.secondaryContainer, CircleShape)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                style = top.yukonga.miuix.kmp.theme.MiuixTheme.textStyles.body2,
                color = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.onSecondaryContainer,
            )
        }
        UiStyle.COUI -> FlowRow(
            Modifier.fillMaxWidth().padding(horizontal = UiMetrics.PageInset),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            com.itos.coui.text.Text(
                text = date,
                modifier = Modifier.align(Alignment.CenterVertically).padding(end = 12.dp),
                kind = com.itos.coui.text.CouiTextKind.SectionTitle,
                color = com.itos.coui.theme.CouiTheme.colorScheme.onSurface,
                textAlign = androidx.compose.ui.text.style.TextAlign.Start,
            )
            com.itos.coui.text.Text(
                text = week,
                modifier = Modifier.align(Alignment.CenterVertically),
                kind = com.itos.coui.text.CouiTextKind.PreferenceTitle,
                color = com.itos.coui.theme.CouiTheme.colorScheme.accentOnSurface,
                textAlign = androidx.compose.ui.text.style.TextAlign.End)
        }
    }
}

@Composable
private fun HomeAccount(state: XatuHomeUiState, onNavigate: (Destination) -> Unit) {
    val account = state.account
    var firstLogin by rememberSaveable { mutableStateOf(false) }
    val accountLabel = account?.studentNumber?.takeIf { it.isNotBlank() }
        ?: if (account?.loggedIn == false) "登录教务" else "教务账号"
    val connection = when {
        account == null -> "正在读取账号…"
        state.requiresLogin -> "登录已失效"
        state.error != null -> "教务连接失败"
        account.canRestore && !account.loggedIn && state.loading -> "正在恢复教务登录…"
        !account.loggedIn -> "未登录"
        state.loading -> "正在验证会话"
        else -> "教务已连接"
    }
    val accountRow: @Composable () -> Unit = {
        UiServiceRow(accountLabel, connection, {
            when {
                account == null || (account.loggedIn && !state.requiresLogin) -> onNavigate(Destination.XatuLogin)
                account.studentNumber.isBlank() -> firstLogin = true
                else -> onNavigate(xatuLoginDestination())
            }
        })
    }
    when (LocalUiStyle.current) {
        UiStyle.MATERIAL -> accountRow()
        UiStyle.MIUIX -> top.yukonga.miuix.kmp.basic.Card(Modifier.fillMaxWidth()) { accountRow() }
        UiStyle.COUI -> com.itos.coui.layout.Group { accountRow() }
    }
    if (firstLogin) {
        XatuFirstLoginDialog(
            onDismiss = { firstLogin = false },
            onNavigate = {
                firstLogin = false
                onNavigate(it)
            },
        )
    }
}

@Composable
private fun HomePanel(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (LocalUiStyle.current == UiStyle.MIUIX) {
        Column(Modifier.fillMaxWidth()) {
            top.yukonga.miuix.kmp.basic.SmallTitle(
                title,
                insideMargin = PaddingValues(horizontal = UiMetrics.PageInset, vertical = 4.dp),
            )
            top.yukonga.miuix.kmp.basic.Card(modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(UiMetrics.PageInset), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
            }
        }
    } else if (LocalUiStyle.current == UiStyle.COUI) {
        com.itos.coui.layout.Group(title = title, modifier = modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
        }
    } else UiCard(modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(UiMetrics.PageInset), verticalArrangement = Arrangement.spacedBy(UiMetrics.ContentGap)) {
            UiText(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun HomeCourseCard(
    state: TodayUiState,
    now: LocalDateTime,
    onRetry: () -> Unit,
    onNavigate: (Destination) -> Unit,
    onOpenCourse: (CourseDisplayModel) -> Unit,
) {
    val success = state as? TodayUiState.Success
    val time = now.toLocalTime().withNano(0).toString()
    val current = success?.takeIf { it.status == TodayStatus.Normal }?.let { homeCourseAt(it.courses, time) }
    val openToday = { onNavigate(Destination.TodaySchedule) }

    if (current == null) {
        val status = when (state) {
            TodayUiState.Loading -> "正在读取课表…"
            is TodayUiState.Error -> state.message
            is TodayUiState.Success -> when (state.status) {
                TodayStatus.NoSemesterConfig -> "未设置开学日期"
                TodayStatus.Vacation -> state.startDate?.let { "$it 开学" } ?: "学期尚未开始"
                TodayStatus.SemesterEnded -> "本学期已结束"
                TodayStatus.Normal -> if (state.courses.isEmpty()) "今天没有课程" else "今天的课程已结束"
            }
        }
        if (state is TodayUiState.Success && state.status != TodayStatus.NoSemesterConfig) {
            val sync = { onNavigate(xatuCourseImport) }
            when (LocalUiStyle.current) {
                UiStyle.MIUIX -> {
                    top.yukonga.miuix.kmp.basic.Card(Modifier.fillMaxWidth()) {
                        BoxWithConstraints(Modifier.fillMaxWidth()) {
                            val todayRow: @Composable (Modifier) -> Unit = { modifier ->
                                top.yukonga.miuix.kmp.basic.BasicComponent(
                                    modifier = modifier,
                                    title = status, onClick = openToday,
                                    endActions = {
                                        top.yukonga.miuix.kmp.basic.Icon(vectorResource(Res.drawable.chevron_right_24px),
                                            null, Modifier.size(18.dp), tint = uiStyleColors().secondaryText)
                                    },
                                    insideMargin = PaddingValues(horizontal = UiMetrics.PageInset, vertical = 14.dp),
                                )
                            }
                            if (maxWidth / LocalDensity.current.fontScale >= 290.dp) {
                                Row(Modifier.fillMaxWidth().padding(end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    todayRow(Modifier.weight(1f))
                                    UiButton("同步课表", sync, selected = true)
                                }
                            } else {
                                Column {
                                    todayRow(Modifier.fillMaxWidth())
                                    UiButton("同步课表", sync, Modifier.padding(start = 12.dp, bottom = 8.dp), selected = true)
                                }
                            }
                        }
                    }
                    return
                }
                UiStyle.COUI -> {
                    com.itos.coui.layout.Group(title = "今日课表", headerActions = { UiButton("同步课表", sync) }) {
                        com.itos.coui.layout.Item(
                            icon = { Icon(com.itos.coui.icons.CouiIcons.Calendar, null, tint = uiStyleColors().accent) },
                            title = { com.itos.coui.text.Text(text = status, kind = com.itos.coui.text.CouiTextKind.PreferenceTitle) },
                            trailing = { Icon(com.itos.coui.icons.CouiIcons.Next, null, Modifier.size(16.dp), tint = uiStyleColors().secondaryText) },
                            onClick = openToday,
                        )
                    }
                    return
                }
                UiStyle.MATERIAL -> Unit
            }
        }
        val courseSummary: @Composable ColumnScope.() -> Unit = {
            if (state == TodayUiState.Loading || state is TodayUiState.Error) {
                UiServiceRow("今日课程", "", openToday)
                Column(Modifier.padding(horizontal = UiMetrics.PageInset)) {
                    UiFeedback(status, if (state == TodayUiState.Loading) UiFeedbackKind.LOADING else UiFeedbackKind.ERROR)
                    if (state is TodayUiState.Error) UiButton("重新读取", onRetry)
                }
            } else {
                UiServiceRow("今日课程", status, openToday)
            }
            val actionsModifier = if (LocalUiStyle.current == UiStyle.COUI)
                Modifier.padding(start = UiMetrics.PageInset, end = UiMetrics.PageInset, bottom = UiMetrics.ContentGap)
            else Modifier.padding(horizontal = UiMetrics.ContentGap)
            HomeCourseActions(state, onNavigate, actionsModifier)
        }
        if (LocalUiStyle.current == UiStyle.MATERIAL) {
            Column(Modifier.fillMaxWidth(), content = courseSummary)
        } else {
            UiCard(Modifier.fillMaxWidth(), content = courseSummary)
        }
        return
    }

    val startTime = courseTime(current.startTime)
    val endTime = courseTime(current.endTime)
    val label = when {
        startTime == null || endTime == null -> "时间待确认"
        kotlinx.datetime.LocalTime.parse(time) >= startTime -> "正在上课"
        else -> "下一节课"
    }
    val accent = uiStyleColors().accent
    val openCourse = { onOpenCourse(current) }
    val details: @Composable ColumnScope.() -> Unit = {
        val secondary = listOfNotNull(
            current.course.startSection?.let { "第 $it–${current.course.endSection} 节" },
            current.course.teacher.takeIf { it.isNotBlank() },
        ).joinToString(" · ")
        if (LocalUiStyle.current == UiStyle.MIUIX) {
            top.yukonga.miuix.kmp.basic.Text(
                current.course.name,
                style = top.yukonga.miuix.kmp.theme.MiuixTheme.textStyles.title2,
            )
            if (startTime != null && endTime != null) {
                top.yukonga.miuix.kmp.basic.Text(
                    "$startTime–$endTime",
                    style = top.yukonga.miuix.kmp.theme.MiuixTheme.textStyles.title3,
                    color = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.primary,
                )
            }
            if (current.course.position.isNotBlank()) {
                top.yukonga.miuix.kmp.basic.Text(
                    current.course.position,
                    style = top.yukonga.miuix.kmp.theme.MiuixTheme.textStyles.body1,
                )
            }
            if (secondary.isNotEmpty()) {
                top.yukonga.miuix.kmp.basic.Text(
                    secondary,
                    style = top.yukonga.miuix.kmp.theme.MiuixTheme.textStyles.body2,
                    color = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        } else if (LocalUiStyle.current == UiStyle.COUI) {
            com.itos.coui.text.Text(text = current.course.name, kind = com.itos.coui.text.CouiTextKind.SectionTitle,
                textAlign = androidx.compose.ui.text.style.TextAlign.Start)
            if (startTime != null && endTime != null) {
                com.itos.coui.text.Text(text = "$startTime–$endTime", kind = com.itos.coui.text.CouiTextKind.PreferenceTitle, color = accent)
            }
            if (current.course.position.isNotBlank()) {
                com.itos.coui.text.Text(text = current.course.position, kind = com.itos.coui.text.CouiTextKind.Body)
            }
            if (secondary.isNotEmpty()) {
                com.itos.coui.text.Text(text = secondary, kind = com.itos.coui.text.CouiTextKind.PreferenceSummary, color = uiStyleColors().secondaryText)
            }
        } else {
            UiText(current.course.name, style = MaterialTheme.typography.titleLarge)
            if (startTime != null && endTime != null) {
                UiText("$startTime–$endTime", style = MaterialTheme.typography.titleMedium)
            }
            if (current.course.position.isNotBlank()) {
                UiText(current.course.position, style = MaterialTheme.typography.bodyMedium)
            }
            if (secondary.isNotEmpty()) {
                UiText(secondary, style = MaterialTheme.typography.bodySmall, color = uiStyleColors().secondaryText)
            }
        }
    }
    when (LocalUiStyle.current) {
        UiStyle.MATERIAL -> ElevatedCard(
            onClick = openCourse,
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(Modifier.padding(UiMetrics.PageInset), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                details()
            }
        }
        UiStyle.MIUIX -> top.yukonga.miuix.kmp.basic.Card(
            modifier = Modifier.fillMaxWidth(), insideMargin = PaddingValues(UiMetrics.PageInset),
            showIndication = true, onClick = openCourse,
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                top.yukonga.miuix.kmp.basic.Text(label, modifier = Modifier.weight(1f),
                    style = top.yukonga.miuix.kmp.theme.MiuixTheme.textStyles.body2,
                    color = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.primary)
                top.yukonga.miuix.kmp.basic.Icon(vectorResource(Res.drawable.chevron_right_24px), null,
                    Modifier.size(20.dp), tint = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
            Spacer(Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(3.dp), content = details)
        }
        UiStyle.COUI -> com.itos.coui.layout.Group(title = label) {
            com.itos.coui.layout.Item(
                title = { Column(verticalArrangement = Arrangement.spacedBy(4.dp), content = details) },
                trailing = { Icon(vectorResource(Res.drawable.chevron_right_24px), null, tint = accent) },
                onClick = openCourse,
            )
        }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(UiMetrics.ContentGap)) {
        UiButton("今日课表", openToday)
        UiButton("同步课表", { onNavigate(xatuCourseImport) }, selected = LocalUiStyle.current == UiStyle.COUI)
    }
}

@Composable
private fun HomeCourseActions(state: TodayUiState, onNavigate: (Destination) -> Unit, modifier: Modifier) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if ((state as? TodayUiState.Success)?.status == TodayStatus.NoSemesterConfig) {
            UiButton("设置开学日期", { onNavigate(Destination.CourseStartDateSettings) })
        }
        UiButton("同步课表", { onNavigate(xatuCourseImport) }, selected = LocalUiStyle.current == UiStyle.COUI)
    }
}

@Composable
private fun HomeElectricitySummary(snapshot: XatuCampusSnapshot?, query: com.xingheyuzhuan.shiguangschedule.xatu.campus.XatuElectricityQueryState, onRefresh: () -> Unit, onNavigate: (Destination) -> Unit) {
    val open = { onNavigate(Destination.XatuService("electricity")) }
    val loginToRefresh = rememberElectricityLogin(onRefresh)
    var showRoomPicker by rememberSaveable { mutableStateOf(false) }
    val state = query.takeIf { it.dorm == snapshot?.dorm } ?: com.xingheyuzhuan.shiguangschedule.xatu.campus.XatuElectricityQueryState()
    val style = LocalUiStyle.current
    val panelModifier = if (style == UiStyle.COUI) Modifier else Modifier.clickable(
        role = Role.Button,
        onClickLabel = "查看完整电费记录",
        onClick = open,
    )
    HomePanel("宿舍剩余电量", panelModifier) {
        when {
            snapshot == null -> {
                if (style == UiStyle.COUI) {
                    com.itos.coui.layout.Item(title = { UiText("正在读取电量记录…") })
                } else {
                    UiFeedback("正在读取电量记录…", UiFeedbackKind.LOADING)
                }
            }
            snapshot.dorm == null -> {
                Column(Modifier.padding(horizontal = if (style == UiStyle.COUI) UiMetrics.PageInset else 0.dp)) {
                    ElectricityFirstRoomAction { showRoomPicker = true }
                }
            }
            else -> {
                val dorm = snapshot.dorm
                val needsLogin = electricityNeedsLogin(state)
                val refreshLabel = if (needsLogin) "登录缴费平台" else if (state.loading) "正在刷新…" else "刷新电量"
                val refresh = if (needsLogin) loginToRefresh else onRefresh
                val room = "${dorm.areaName.trim()} · ${dorm.buildingName.trim()} · ${dorm.roomName.trim()}"
                val readingTime = if (snapshot.balanceAt > 0) {
                    "${electricityReadingSource(snapshot, state)} ${campusDateTime(snapshot.balanceAt)}"
                } else "更新时间未知"
                val change = remember(snapshot.balanceHistory) { recentXatuElectricityChange(snapshot.balanceHistory) }
                val changeText = change?.let {
                    when (it.signum()) {
                        -1 -> "较上次减少 ${it.abs().toPlainString()} 度"
                        1 -> "较上次增加 ${it.toPlainString()} 度"
                        else -> "与上次查询持平"
                    }
                }

                when (style) {
                    UiStyle.MATERIAL -> {
                        ElectricityReading(snapshot.balance)
                        UiText(room, style = MaterialTheme.typography.bodySmall, color = uiStyleColors().secondaryText)
                        if (snapshot.balance != null) UiText(readingTime, style = MaterialTheme.typography.bodySmall, color = uiStyleColors().secondaryText)
                        changeText?.let { UiText(it, style = MaterialTheme.typography.bodySmall) }
                    }
                    UiStyle.MIUIX -> {
                        ElectricityReading(snapshot.balance)
                        top.yukonga.miuix.kmp.basic.Text(room, style = top.yukonga.miuix.kmp.theme.MiuixTheme.textStyles.body1)
                        if (snapshot.balance != null) {
                            top.yukonga.miuix.kmp.basic.Text(
                                readingTime,
                                style = top.yukonga.miuix.kmp.theme.MiuixTheme.textStyles.body2,
                                color = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                        changeText?.let {
                            top.yukonga.miuix.kmp.basic.Text(
                                it,
                                style = top.yukonga.miuix.kmp.theme.MiuixTheme.textStyles.body2,
                                color = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                    }
                    UiStyle.COUI -> {
                        com.itos.coui.layout.Item(
                            title = { ElectricityReading(snapshot.balance) },
                            summary = {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    UiText(room)
                                    if (snapshot.balance != null) UiText(readingTime, color = uiStyleColors().secondaryText)
                                    changeText?.let { UiText(it, color = uiStyleColors().secondaryText) }
                                }
                            },
                            trailing = { Icon(vectorResource(Res.drawable.chevron_right_24px), null, tint = uiStyleColors().secondaryText) },
                            onClick = open,
                        )
                    }
                }
                Column(
                    Modifier.padding(horizontal = if (style == UiStyle.COUI) UiMetrics.PageInset else 0.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    BalanceTrend(snapshot.balanceHistory, compact = true)
                }
                ElectricityRequestFeedback(state)
                if (style == UiStyle.COUI) {
                    com.itos.coui.layout.Item(
                        title = { UiText(refreshLabel) },
                        enabled = !state.loading,
                        onClick = refresh,
                    )
                    com.itos.coui.layout.Item(title = { UiText("切换房间") }, onClick = { showRoomPicker = true })
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        UiButton(refreshLabel, refresh, enabled = !state.loading)
                        UiButton("切换房间", { showRoomPicker = true })
                    }
                }
            }
        }
    }
    if (showRoomPicker && snapshot != null) {
        ElectricityRoomSheet(snapshot, onDismiss = { showRoomPicker = false })
    }
}

@Composable
private fun HomeExamCard(
    state: XatuHomeUiState,
    now: LocalDateTime,
    onRefresh: () -> Unit,
    onNavigate: (Destination) -> Unit,
) {
    var showReason by rememberSaveable { mutableStateOf(false) }
    val style = LocalUiStyle.current
    val openExams = { onNavigate(Destination.XatuService("exams")) }
    val panelModifier = if (style == UiStyle.COUI) Modifier else Modifier.clickable(
        role = Role.Button,
        onClickLabel = "查看考试安排",
        onClick = openExams,
    )
    HomePanel("最近考试", panelModifier) {
        val exam = state.exams?.let { upcomingXatuExam(it, now) }
        val examDetails = exam?.let {
            listOf(
                it.time,
                it.room.takeIf(String::isNotBlank)?.let { room -> "考场 $room" },
                it.seat.takeIf(String::isNotBlank)?.let { seat -> "座位 $seat" },
            ).filterNotNull().joinToString(" · ")
        }
        val status = when {
            state.account == null -> "正在读取账号…"
            !state.account.loggedIn && state.loading -> "正在恢复教务登录…"
            state.error != null && exam == null -> null
            !state.account.loggedIn -> "未登录"
            exam != null -> null
            state.loading && state.exams == null -> "正在读取考试…"
            state.exams == null && state.error == null -> "暂无考试数据"
            state.exams?.any { it.startDateTime() == null } == true -> "部分考试日期未定"
            state.exams != null -> "暂无待考场次"
            else -> null
        }
        if (exam != null) {
            when (style) {
                UiStyle.MATERIAL -> {
                    UiText(exam.course, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    UiText(examDetails.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                }
                UiStyle.MIUIX -> {
                    top.yukonga.miuix.kmp.basic.BasicComponent(
                        title = exam.course,
                        summary = examDetails,
                        onClick = openExams,
                    )
                }
                UiStyle.COUI -> {
                    com.itos.coui.layout.Item(
                        title = { UiText(exam.course, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) },
                        summary = examDetails?.let { { UiText(it) } },
                        onClick = openExams,
                    )
                }
            }
        } else if (status != null) {
            when (style) {
                UiStyle.MATERIAL -> UiText(status, style = MaterialTheme.typography.bodyMedium)
                UiStyle.MIUIX -> top.yukonga.miuix.kmp.basic.BasicComponent(title = status, onClick = openExams)
                UiStyle.COUI -> com.itos.coui.layout.Item(title = { UiText(status) }, onClick = openExams)
            }
        }
        if (state.examSemester.isNotBlank()) {
            if (style == UiStyle.MIUIX) {
                top.yukonga.miuix.kmp.basic.Text(
                    state.examSemester,
                    style = top.yukonga.miuix.kmp.theme.MiuixTheme.textStyles.body2,
                    color = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            } else UiText(state.examSemester, style = MaterialTheme.typography.bodySmall, color = uiStyleColors().secondaryText)
        }
        state.cacheUpdatedAt?.let {
            val cacheLabel = "${if (state.loading || state.error != null) "本机记录" else "更新于"} ${homeQueryTime(it)}"
            if (style == UiStyle.COUI) {
                com.itos.coui.layout.Item(title = { UiText(cacheLabel) })
            } else if (style == UiStyle.MIUIX) {
                top.yukonga.miuix.kmp.basic.Text(
                    cacheLabel,
                    style = top.yukonga.miuix.kmp.theme.MiuixTheme.textStyles.body2,
                    color = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            } else UiText(cacheLabel, style = MaterialTheme.typography.bodySmall, color = uiStyleColors().secondaryText)
        }
        if (!state.requiresLogin && state.error != null) {
            UiText("教务更新失败", color = MaterialTheme.colorScheme.error)
            if (showReason) UiText(state.error, style = MaterialTheme.typography.bodySmall)
        }
        if (state.account?.canRestore == true && !state.requiresLogin) {
            if (style == UiStyle.COUI) {
                com.itos.coui.layout.Item(
                    title = { UiText(if (state.loading) "正在刷新…" else if (state.error != null) "重试" else "刷新教务摘要") },
                    enabled = !state.loading,
                    onClick = onRefresh,
                )
                if (state.error != null) {
                    com.itos.coui.layout.Item(
                        title = { UiText(if (showReason) "收起原因" else "查看原因") },
                        onClick = { showReason = !showReason },
                    )
                }
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(UiMetrics.ContentGap)) {
                    UiButton(
                        if (state.loading) "正在刷新…" else if (state.error != null) "重试" else "刷新教务摘要",
                        onRefresh,
                        enabled = !state.loading,
                    )
                    if (state.error != null) UiButton(
                        if (showReason) "收起原因" else "查看原因",
                        { showReason = !showReason },
                    )
                }
            }
        }
    }
}
