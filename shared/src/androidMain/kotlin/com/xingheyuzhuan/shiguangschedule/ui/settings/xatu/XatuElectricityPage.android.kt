package com.xingheyuzhuan.shiguangschedule.ui.settings.xatu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web.WebViewScreen
import com.itos.coui.backdrop.couiBackdrop
import com.itos.coui.backdrop.rememberCouiBackdrop
import com.itos.coui.chip.ChipTabRow
import com.itos.coui.chip.CouiChipTabItem
import androidx.compose.ui.graphics.compositeOver
import com.itos.coui.theme.CouiTheme
import com.xingheyuzhuan.shiguangschedule.data.repository.*
import com.xingheyuzhuan.shiguangschedule.data.model.UiStyle
import com.xingheyuzhuan.shiguangschedule.ui.components.*
import com.xingheyuzhuan.shiguangschedule.xatu.campus.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

internal const val XATU_PAY_LOGIN = "https://jfpay.xatu.edu.cn/casLogin/"

@Composable
internal fun ElectricityReading(balance: String?) {
    if (balance == null) {
        UiText("尚未取得电量", style = MaterialTheme.typography.bodyLarge)
        return
    }
    val theme = LocalUiStyle.current
    val numberStyle = when (theme) {
        UiStyle.MATERIAL -> MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Medium)
        UiStyle.MIUIX -> top.yukonga.miuix.kmp.theme.MiuixTheme.textStyles.title1
        UiStyle.COUI -> com.itos.coui.text.rememberCouiTextStyle(com.itos.coui.text.CouiTextKind.PageTitle)
            .copy(fontSize = 24.sp, lineHeight = 1.15.em)
    }.copy(color = uiStyleColors().onSurface, fontFeatureSettings = "tnum")
    val unitStyle = MaterialTheme.typography.bodyMedium.toSpanStyle()
        .copy(color = uiStyleColors().secondaryText, fontWeight = FontWeight.Normal)
    val reading = buildAnnotatedString {
        append(balance)
        withStyle(unitStyle) { append(" 度") }
    }
    when (theme) {
        UiStyle.MATERIAL -> Text(reading, style = numberStyle)
        UiStyle.MIUIX -> top.yukonga.miuix.kmp.basic.Text(reading, style = numberStyle)
        // alpha7's Text accepts plain strings. Keep its typeface and metrics for rich text.
        UiStyle.COUI -> BasicText(reading, style = numberStyle)
    }
}

@Composable
internal fun rememberElectricityLogin(onAuthenticated: () -> Unit): () -> Unit {
    var open by rememberSaveable { mutableStateOf(false) }
    if (open) {
        val service: XatuCampusService = koinInject()
        val authenticated by rememberUpdatedState(onAuthenticated)
        val close = { open = false }
        Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            WebViewScreen(
                onNavigate = { close() }, onBack = close,
                initialUrl = XATU_PAY_LOGIN, assetJsPath = null, serviceTitle = "登录缴费平台",
                confirmServiceSession = service::awaitPayLogin,
                onLoginComplete = {
                    if (open) {
                        close()
                        authenticated()
                    }
                },
            )
        }
    }
    return { open = true }
}

@Composable
internal fun ElectricityFirstRoomAction(onSelect: () -> Unit) {
    val service: XatuCampusService = koinInject()
    var hasCookie by remember { mutableStateOf(service.hasPayCookie()) }
    val login = rememberElectricityLogin { hasCookie = true; onSelect() }
    if (hasCookie) {
        UiFeedback("尚未选择房间", UiFeedbackKind.EMPTY)
        UiButton("选择房间", onSelect)
    } else {
        UiFeedback("请先登录缴费平台", UiFeedbackKind.INFO)
        UiButton("登录缴费平台", login)
    }
}

@Composable
internal fun electricityNeedsLogin(state: XatuElectricityQueryState): Boolean {
    val service: XatuCampusService = koinInject()
    return state.requiresLogin || !remember(state) { service.hasPayCookie() }
}

internal fun electricityReadingSource(snapshot: XatuCampusSnapshot, query: XatuElectricityQueryState): String = when {
    query.dorm != snapshot.dorm -> "上次更新"
    query.loading -> "上次更新"
    query.error != null -> "上次更新"
    query.checkedAt > 0 && query.checkedAt == snapshot.balanceAt -> "更新于"
    else -> "上次更新"
}

@Composable
internal fun ElectricityRequestFeedback(state: XatuElectricityQueryState) {
    if (state.loading) {
        UiFeedback("正在查询剩余电量…", UiFeedbackKind.LOADING)
    }
    state.error?.let {
        UiFeedback(
            when {
                state.requiresLogin -> it
                state.noData -> it
                else -> "查询失败：$it"
            },
            kind = when {
                state.requiresLogin -> UiFeedbackKind.INFO
                state.noData -> UiFeedbackKind.EMPTY
                else -> UiFeedbackKind.ERROR
            },
        )
    }
}

@Composable
private fun ElectricityPanel(title: String, content: @Composable ColumnScope.() -> Unit) {
    if (LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.MIUIX) {
        Column(Modifier.fillMaxWidth()) {
            top.yukonga.miuix.kmp.basic.SmallTitle(title,
                insideMargin = PaddingValues(horizontal = UiMetrics.PageInset, vertical = 8.dp))
            top.yukonga.miuix.kmp.basic.Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(UiMetrics.PageInset),
                    verticalArrangement = Arrangement.spacedBy(UiMetrics.ContentGap), content = content)
            }
        }
    } else if (LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.COUI) {
        com.itos.coui.layout.Group(title = title) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                content()
            }
        }
    } else UiCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            UiText(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ElectricityBalanceContent(snapshot: XatuCampusSnapshot, onOpen: (String) -> Unit) {
    val repository: XatuElectricityRepository = koinInject()
    val query by repository.state.collectAsState()
    val saved = snapshot.dorm
    val state = query.takeIf { it.dorm == saved } ?: XatuElectricityQueryState()
    val scope = rememberCoroutineScope()
    var showHistory by rememberSaveable(saved) { mutableStateOf(false) }
    var showRoomPicker by rememberSaveable { mutableStateOf(false) }
    var dailyHistory by rememberSaveable(saved) { mutableStateOf(false) }
    var trendDays by rememberSaveable(saved) { mutableIntStateOf(7) }
    val daily = remember(snapshot.balanceHistory) { summarizeXatuElectricity(snapshot.balanceHistory) }
    if (showRoomPicker) {
        ElectricityRoomSheet(snapshot, onDismiss = { showRoomPicker = false })
    }
    if (saved == null) {
        ElectricityPanel("剩余电量") {
            ElectricityFirstRoomAction { showRoomPicker = true }
        }
        return
    }
    val loginToRefresh = rememberElectricityLogin {
        scope.launch { repository.refresh(saved) }
    }
    val needsLogin = electricityNeedsLogin(state)
    val refreshLabel = if (needsLogin) "登录缴费平台" else if (state.loading) "正在刷新…" else "刷新电量"
    val refresh: () -> Unit = {
        if (needsLogin) loginToRefresh() else scope.launch { repository.refresh(saved) }
    }
    ElectricityPanel("剩余电量") {
        val colors = uiStyleColors()
        ElectricityReading(snapshot.balance)
        if (LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.COUI) {
            com.itos.coui.layout.Item(
                contentPadding = PaddingValues(vertical = 6.dp),
                title = {
                    com.itos.coui.text.Text(
                        text = "房间 ${saved.roomName.trim()}",
                        kind = com.itos.coui.text.CouiTextKind.PreferenceTitle,
                    )
                },
                summary = {
                    com.itos.coui.text.Text(
                        text = "${saved.areaName.trim()} · ${saved.buildingName.trim()}",
                        kind = com.itos.coui.text.CouiTextKind.PreferenceSummary,
                        color = com.itos.coui.layout.CouiPageDefaults.themeColors().summary,
                    )
                },
            )
        } else {
            UiText("${saved.areaName.trim()} · ${saved.buildingName.trim()} · 房间 ${saved.roomName.trim()}",
                style = MaterialTheme.typography.bodyMedium)
        }
        if (snapshot.balance != null) {
            UiText(
                if (snapshot.balanceAt > 0) "${electricityReadingSource(snapshot, state)} ${campusDateTime(snapshot.balanceAt)}" else "更新时间未知",
                style = MaterialTheme.typography.bodySmall, color = colors.secondaryText,
            )
        }
        ElectricityRequestFeedback(state)
        if (LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.MIUIX) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                top.yukonga.miuix.kmp.basic.TextButton(
                    refreshLabel, refresh,
                    enabled = !state.loading,
                    colors = top.yukonga.miuix.kmp.basic.ButtonDefaults.textButtonColorsPrimary(),
                )
                UiButton("切换房间", { showRoomPicker = true })
            }
        } else if (LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.COUI) {
            CouiAction(
                text = refreshLabel,
                onClick = refresh,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.loading,
                filled = true,
            )
            com.itos.coui.layout.Item(
                contentPadding = PaddingValues(vertical = 6.dp),
                title = { com.itos.coui.text.Text(text = "切换房间", kind = com.itos.coui.text.CouiTextKind.PreferenceTitle) },
                trailing = {
                    androidx.compose.material3.Icon(
                        com.itos.coui.icons.CouiIcons.Next,
                        contentDescription = null,
                        modifier = Modifier.size(width = 12.dp, height = 24.dp),
                        tint = com.itos.coui.layout.CouiPageDefaults.themeColors().summary,
                    )
                },
                onClick = { showRoomPicker = true },
            )
        } else FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = refresh, enabled = !state.loading) {
                if (state.loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Text(refreshLabel)
            }
            OutlinedButton(onClick = { showRoomPicker = true }) { Text("切换房间") }
        }
        if (!needsLogin) {
            UiButton("电费充值", { onOpen(XATU_PAY_LOGIN) })
        }
    }
    ElectricityPanel("剩余电量趋势") {
        val periods = listOf(7, 30)
        when (LocalUiStyle.current) {
            com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.MATERIAL -> SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                periods.forEachIndexed { index, days ->
                    SegmentedButton(
                        selected = trendDays == days,
                        onClick = { trendDays = days },
                        shape = SegmentedButtonDefaults.itemShape(index, periods.size),
                    ) { Text("$days 天") }
                }
            }
            com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.MIUIX -> top.yukonga.miuix.kmp.basic.TabRowWithContour(
                tabs = periods.map { "$it 天" },
                selectedTabIndex = periods.indexOf(trendDays),
                onTabSelected = { trendDays = periods[it] },
                minWidth = 84.dp, maxWidth = 140.dp,
                colors = top.yukonga.miuix.kmp.basic.TabRowDefaults.tabRowColors(
                    selectedBackgroundColor = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.primaryContainer,
                    selectedContentColor = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.onPrimaryContainer,
                ),
            )
            com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.COUI -> {
                val backdrop = rememberCouiBackdrop()
                Box(Modifier.fillMaxWidth()) {
                    val palette = CouiTheme.colorScheme
                    Box(Modifier.matchParentSize().couiBackdrop(backdrop).background(palette.card.compositeOver(palette.background)))
                    ChipTabRow(
                        items = periods.map { CouiChipTabItem(it, "$it 天") },
                        selectedKey = trendDays,
                        onSelectedKeyChange = { trendDays = it as Int },
                        backdrop = backdrop,
                    )
                }
            }
        }
        BalanceTrend(snapshot.balanceHistory, days = trendDays)
    }
    ElectricityPanel("最近电量记录") {
        if (snapshot.balanceHistory.isEmpty()) UiFeedback("暂无历史记录", UiFeedbackKind.EMPTY)
        else {
            val recentRecords = snapshot.balanceHistory.sortedByDescending { it.at }.take(3)
            recentRecords.forEachIndexed { index, record ->
                if (LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.MATERIAL) {
                    ListItem(headlineContent = { Text("${record.balance} 度") }, supportingContent = { Text(campusDateTime(record.at)) },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow))
                } else if (LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.MIUIX) {
                    top.yukonga.miuix.kmp.basic.BasicComponent(title = "${record.balance} 度", summary = campusDateTime(record.at), insideMargin = PaddingValues(vertical = 8.dp))
                } else {
                    com.itos.coui.layout.Item(
                        contentPadding = PaddingValues(vertical = 6.dp),
                        title = {
                            com.itos.coui.text.Text(
                                text = "${record.balance} 度",
                                kind = com.itos.coui.text.CouiTextKind.PreferenceTitle,
                            )
                        },
                        summary = {
                            com.itos.coui.text.Text(
                                text = campusDateTime(record.at),
                                kind = com.itos.coui.text.CouiTextKind.PreferenceSummary,
                                color = com.itos.coui.layout.CouiPageDefaults.themeColors().summary,
                            )
                        },
                    )
                    if (index < recentRecords.lastIndex) com.itos.coui.layout.Divider()
                }
            }
            if (LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.COUI) {
                com.itos.coui.layout.Divider()
                com.itos.coui.layout.Item(
                    contentPadding = PaddingValues(vertical = 6.dp),
                    title = {
                        com.itos.coui.text.Text(
                            text = "查看全部记录",
                            kind = com.itos.coui.text.CouiTextKind.PreferenceTitle,
                        )
                    },
                    summary = {
                        com.itos.coui.text.Text(
                            text = "${snapshot.balanceHistory.size} 条",
                            kind = com.itos.coui.text.CouiTextKind.PreferenceSummary,
                            color = com.itos.coui.layout.CouiPageDefaults.themeColors().summary,
                        )
                    },
                    trailing = {
                        androidx.compose.material3.Icon(
                            com.itos.coui.icons.CouiIcons.Next,
                            contentDescription = null,
                            modifier = Modifier.size(width = 12.dp, height = 24.dp),
                            tint = com.itos.coui.layout.CouiPageDefaults.themeColors().summary,
                        )
                    },
                    onClick = { showHistory = true },
                )
            } else {
                UiActionButton(onClick = { showHistory = true }, modifier = Modifier.fillMaxWidth(), text = "查看全部 ${snapshot.balanceHistory.size} 条记录")
            }
        }
    }
    if (showHistory) {
        val records: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.COUI) {
                    val backdrop = rememberCouiBackdrop()
                    Box(Modifier.fillMaxWidth()) {
                        Box(Modifier.matchParentSize().couiBackdrop(backdrop).background(CouiTheme.colorScheme.surface))
                        ChipTabRow(
                            items = listOf(
                                CouiChipTabItem(false, "按次查询"),
                                CouiChipTabItem(true, "按日汇总"),
                            ),
                            selectedKey = dailyHistory,
                            onSelectedKeyChange = { dailyHistory = it as Boolean },
                            backdrop = backdrop,
                        )
                    }
                } else {
                    UiActionButton(onClick = { dailyHistory = !dailyHistory }, text = if (dailyHistory) "按次查询" else "按日汇总")
                }
                LazyColumn(Modifier.heightIn(max = 360.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (dailyHistory) items(daily.asReversed()) {
                        UiText("${it.day} · ${it.balance} 度${it.decrease?.let { value -> " · 余额较前日减少 $value 度" }.orEmpty()}")
                    } else items(snapshot.balanceHistory.sortedByDescending { it.at }) {
                        UiText("${campusDateTime(it.at)} · ${it.balance} 度")
                    }
                }
            }
        }
        if (LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.MATERIAL) {
            ModalBottomSheet(onDismissRequest = { showHistory = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("${saved.roomName.trim()} · 电量记录", style = MaterialTheme.typography.headlineSmall)
                    records()
                }
            }
        } else if (LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.MIUIX) {
            top.yukonga.miuix.kmp.window.WindowBottomSheet(show = true, title = "${saved.roomName.trim()} · 电量记录", onDismissRequest = { showHistory = false }) {
                records()
            }
        } else UiDialog(onDismissRequest = { showHistory = false }, title = "${saved.roomName.trim()} · 电量记录",
            text = records, confirmButton = UiDialogAction(onClick = { showHistory = false }, text = "关闭"))
    }
}

@Composable
internal fun ElectricitySettingsContent(snapshot: XatuCampusSnapshot, repository: XatuCampusRepository, service: XatuCampusService) {
    ElectricityRoomForm(snapshot, repository, service)
}

@Composable
internal fun ElectricityRoomSheet(snapshot: XatuCampusSnapshot, onDismiss: () -> Unit) {
    val repository: XatuCampusRepository = koinInject()
    val service: XatuCampusService = koinInject()
    ElectricityRoomForm(snapshot, repository, service, onDismiss)
}

@Composable
private fun ElectricityRoomForm(
    snapshot: XatuCampusSnapshot,
    repository: XatuCampusRepository,
    service: XatuCampusService,
    onDismiss: (() -> Unit)? = null,
) {
    val electricity: XatuElectricityRepository = koinInject()
    val query by electricity.state.collectAsState()
    val scope = rememberCoroutineScope()
    val saved = snapshot.dorm
    var projects by remember { mutableStateOf<List<XatuCampusOption>>(emptyList()) }
    var areas by remember { mutableStateOf<List<XatuCampusOption>>(emptyList()) }
    var buildings by remember { mutableStateOf<List<XatuCampusOption>>(emptyList()) }
    var rooms by remember { mutableStateOf<List<XatuCampusOption>>(emptyList()) }
    var projectId by rememberSaveable { mutableStateOf(saved?.projectId) }
    var areaId by rememberSaveable { mutableStateOf(saved?.areaId) }
    var buildingId by rememberSaveable { mutableStateOf(saved?.buildingId) }
    var roomId by rememberSaveable { mutableStateOf(saved?.roomId) }
    val project = projects.firstOrNull { it.id == projectId }
    val area = areas.firstOrNull { it.id == areaId }
    val building = buildings.firstOrNull { it.id == buildingId }
    val room = rooms.firstOrNull { it.id == roomId }
    var status by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var requiresLogin by remember { mutableStateOf(!service.hasPayCookie()) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }
    var confirmClear by remember { mutableStateOf(false) }

    LaunchedEffect(project?.id, area?.id, building?.id, reload) {
        if (!service.hasPayCookie()) {
            requiresLogin = true
            error = XatuPaySessionExpired(false).message
            loading = false
            return@LaunchedEffect
        }
        loading = true
        error = null
        requiresLogin = false
        try {
            when {
                project == null -> {
                    status = "正在读取电费项目…"
                    service.confirmPaySession()
                    projects = service.projects()
                    status = if (projects.isEmpty()) "暂无可用电费项目" else ""
                }
                area == null -> {
                    status = "正在读取校区…"
                    areas = service.areas(project.id)
                    status = if (areas.isEmpty()) "该项目暂无校区" else ""
                }
                building == null -> {
                    status = "正在读取楼栋…"
                    buildings = service.buildings(project.id, area.id)
                    status = if (buildings.isEmpty()) "该校区暂无楼栋" else ""
                }
                else -> {
                    status = "正在读取房间…"
                    rooms = service.rooms(project.id, area.id, building.id)
                    status = if (rooms.isEmpty()) "该楼栋暂无房间" else ""
                }
            }
        } catch (cancelled: CancellationException) { throw cancelled
        } catch (failure: Exception) {
            error = failure.message ?: "宿舍选项读取失败"
            requiresLogin = failure is XatuPaySessionExpired
            status = ""
        } finally { loading = false }
    }

    val selected = if (project != null && area != null && building != null && room != null) XatuDormSelection(
        project.id, project.name, area.id, area.name, building.id, building.name, room.id, room.name
    ) else null
    val matchesSaved = selected != null && saved != null && selected.projectId == saved.projectId && selected.areaId == saved.areaId && selected.buildingId == saved.buildingId && selected.roomId == saved.roomId

    val matchingQuery = query.takeIf { !saving && it.dorm == saved && onDismiss == null }
    val login = rememberElectricityLogin {
        if (requiresLogin) reload++
        if (matchingQuery?.requiresLogin == true && saved != null) {
            scope.launch { electricity.refresh(saved) }
        }
    }
    val fields: @Composable () -> Unit = fields@{
        if (requiresLogin) {
            UiFeedback(error ?: "请先登录缴费平台", UiFeedbackKind.INFO)
            UiButton("登录缴费平台", login)
            return@fields
        }
        if (status.isNotBlank() && !saving) UiFeedback(status, if (loading) UiFeedbackKind.LOADING else UiFeedbackKind.INFO)
        error?.let { UiFeedback(it, UiFeedbackKind.ERROR) }
        ElectricityOptionPicker("项目", projects, project, !loading && !saving) {
            if (projectId != it.id) {
                projectId = it.id
                areaId = null
                buildingId = null
                roomId = null
                areas = emptyList()
                buildings = emptyList()
                rooms = emptyList()
            }
        }
        ElectricityOptionPicker("校区", areas, area, project != null && !loading && !saving) {
            if (areaId != it.id) {
                areaId = it.id
                buildingId = null
                roomId = null
                buildings = emptyList()
                rooms = emptyList()
            }
        }
        ElectricityOptionPicker("楼栋", buildings, building, area != null && !loading && !saving) {
            if (buildingId != it.id) {
                buildingId = it.id
                roomId = null
                rooms = emptyList()
            }
        }
        ElectricityOptionPicker("房间", rooms, room, building != null && !loading && !saving) { roomId = it.id }
        UiActionButton(onClick = {
            val next = selected ?: return@UiActionButton
            saving = true
            error = null
            scope.launch {
                try {
                    repository.saveDorm(next)
                    electricity.refresh(next)
                    status = "房间已保存"
                    onDismiss?.invoke()
                } catch (cancelled: CancellationException) { throw cancelled
                } catch (failure: Exception) { error = "房间保存失败：${failure.message}"
                } finally { saving = false }
            }
        }, enabled = selected != null && !matchesSaved && !loading && !saving, modifier = Modifier.fillMaxWidth(), text = if (saving) "正在保存与查询…" else if (matchesSaved) "当前房间已保存" else "保存房间并查询")
        matchingQuery?.let { ElectricityRequestFeedback(it) }
        if (matchingQuery?.requiresLogin == true) UiButton("登录缴费平台", login)
        if (onDismiss == null || error != null || (!loading && status.isNotBlank())) UiActionButton(onClick = {
            projects = emptyList()
            areas = emptyList()
            buildings = emptyList()
            rooms = emptyList()
            reload++
        }, enabled = !loading && !saving, modifier = Modifier.fillMaxWidth(), text = "重新读取宿舍选项")
    }
    if (onDismiss == null) {
        ElectricityPanel("选择查询房间") { fields() }
    } else {
        val dismiss = { if (!saving) onDismiss() }
        UiSheet(onDismissRequest = dismiss, skipPartiallyExpanded = true, sheetGesturesEnabled = !saving) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(UiMetrics.PageInset),
                verticalArrangement = Arrangement.spacedBy(UiMetrics.ContentGap),
            ) {
                UiText(if (saved == null) "选择房间" else "切换房间", style = MaterialTheme.typography.titleLarge)
                fields()
                if (LocalUiStyle.current != com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.COUI) {
                    UiActionButton(onClick = dismiss, enabled = !saving, modifier = Modifier.fillMaxWidth(), text = "取消")
                }
            }
        }
    }
    if (onDismiss == null && saved != null) ElectricityPanel("解绑与清除记录") {
        UiActionButton(onClick = { confirmClear = true }, enabled = !saving, modifier = Modifier.fillMaxWidth(), text = "解绑宿舍")
    }
    if (confirmClear) UiDialog(
        onDismissRequest = { confirmClear = false }, title = "解绑宿舍？",
        text = { UiText("清除房间和电量记录？") },
        confirmButton = UiDialogAction(onClick = {
            confirmClear = false
            saving = true
            scope.launch {
                try {
                    repository.clearDorm()
                    projectId = null
                    areaId = null
                    buildingId = null
                    roomId = null
                    status = "宿舍已解绑，本机电量记录已清除"
                } catch (cancelled: CancellationException) { throw cancelled
                } catch (failure: Exception) { error = "解绑失败：${failure.message}"
                } finally { saving = false }
            }
        }, text = "确认解绑"),
        dismissButton = UiDialogAction(onClick = { confirmClear = false }, text = "取消"),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ElectricityOptionPicker(label: String, options: List<XatuCampusOption>, selected: XatuCampusOption?, enabled: Boolean, onSelect: (XatuCampusOption) -> Unit) {
    var open by remember { mutableStateOf(false) }
    if (LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.MATERIAL) {
        ExposedDropdownMenuBox(expanded = open, onExpandedChange = { if (enabled && options.isNotEmpty()) open = it }) {
            OutlinedTextField(value = selected?.name?.trim().orEmpty(), onValueChange = {}, readOnly = true,
                enabled = enabled && options.isNotEmpty(), label = { Text(label) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) },
                modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable))
            ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                options.forEach { option -> DropdownMenuItem(text = { Text(option.name.trim()) }, onClick = { open = false; onSelect(option) }) }
            }
        }
        return
    }
    if (LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.MIUIX) {
        MiuixValuePicker(label, options.map { it.name.trim() }, options.indexOfFirst { it.id == selected?.id }, enabled) { onSelect(options[it]) }
        return
    }
    CouiValuePicker(label, options.map { it.name.trim() }, options.indexOfFirst { it.id == selected?.id }, enabled) { onSelect(options[it]) }
}
