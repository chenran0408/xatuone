package com.xingheyuzhuan.shiguangschedule.ui.settings.xatu

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.webkit.WebViewFeature
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import com.xingheyuzhuan.shiguangschedule.ui.components.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuCampusRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuCampusSnapshot
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuCalendar
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuDormSelection
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuAccount
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuAccountRepository
import com.xingheyuzhuan.shiguangschedule.xatu.academic.XatuExam
import com.xingheyuzhuan.shiguangschedule.xatu.academic.XatuGrade
import com.xingheyuzhuan.shiguangschedule.xatu.academic.XatuGradePage
import com.xingheyuzhuan.shiguangschedule.xatu.academic.XatuCourseInfo
import com.xingheyuzhuan.shiguangschedule.xatu.academic.XatuPlanSection
import com.xingheyuzhuan.shiguangschedule.xatu.academic.XatuStudyPlan
import com.xingheyuzhuan.shiguangschedule.xatu.academic.XatuSemester
import com.xingheyuzhuan.shiguangschedule.xatu.academic.XATU_ALL_GRADES
import com.xingheyuzhuan.shiguangschedule.xatu.campus.XatuCalendarService
import com.xingheyuzhuan.shiguangschedule.xatu.campus.XatuCampusOption
import com.xingheyuzhuan.shiguangschedule.xatu.campus.XatuCampusService
import com.xingheyuzhuan.shiguangschedule.xatu.campus.XatuDorm
import com.xingheyuzhuan.shiguangschedule.xatu.campus.XatuLocation
import com.xingheyuzhuan.shiguangschedule.xatu.campus.currentXatuLocation
import com.xingheyuzhuan.shiguangschedule.xatu.campus.summarizeXatuElectricity
import com.xingheyuzhuan.shiguangschedule.xatu.academic.startDateTime
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
actual fun XatuServicePage(
    page: String,
    onOpen: (String, String) -> Unit,
    modifier: Modifier,
    onLogin: () -> Unit,
    onCourseSettings: () -> Unit,
) {
    val academicViewModel: XatuAcademicViewModel = koinViewModel()
    val academicState by academicViewModel.uiState.collectAsState()
    val accountRepository: XatuAccountRepository = koinInject()
    val account by accountRepository.account.collectAsState(initial = XatuAccount("", false))
    val canRefreshAcademic = !academicState.loading &&
        (account.loggedIn || (account.canRestore && !academicState.requiresLogin))
    val campus: XatuCampusRepository = koinInject()
    val storedSnapshot by campus.snapshot.collectAsState(initial = null)
    val snapshot = storedSnapshot ?: run {
        UiFeedback("正在读取本机校园服务数据…", UiFeedbackKind.LOADING)
        return
    }
    val campusService: XatuCampusService = koinInject()
    val calendarService: XatuCalendarService = koinInject()
    var status by remember(page) { mutableStateOf("正在读取教务数据…") }
    var calendarRefreshing by remember(page) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun refreshCalendar() {
        calendarRefreshing = true
        status = "正在刷新校历…"
        try {
            calendarService.refresh()
            status = "校历已更新"
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            status = "校历读取失败：${error.message ?: "请重试"}"
        } finally {
            calendarRefreshing = false
        }
    }

    DisposableEffect(page, academicViewModel) {
        onDispose { academicViewModel.cancel() }
    }

    LaunchedEffect(page, account.studentNumber, account.loggedIn) {
        if (page == "plan" || page == "grades" || page == "exams" || page == "week") academicViewModel.load(page)
    }

    LaunchedEffect(page, account.studentNumber) {
        when {
            page == "calendar" && snapshot.calendar == null -> {
                refreshCalendar()
            }
            page == "calendar" -> status = ""
            else -> status = ""
        }
    }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(
            if (LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.COUI) 0.dp
            else UiMetrics.SectionGap
        )
    ) {
            if (academicState.requiresLogin && page in listOf("grades", "exams", "plan", "week")) {
                UiActionButton(onClick = onLogin, modifier = Modifier.fillMaxWidth(), text = "重新登录教务")
            }
            if (page in listOf("plan", "grades", "exams")) {
                if (LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.MIUIX) {
                    top.yukonga.miuix.kmp.basic.Card(Modifier.fillMaxWidth()) {
                        top.yukonga.miuix.kmp.basic.BasicComponent(title = if (academicState.loading) "正在刷新…" else "刷新教务数据",
                            summary = academicState.fetchedAt?.let { "更新于 ${campusDateTime(it)}" },
                            enabled = canRefreshAcademic,
                            endActions = { if (academicState.loading) top.yukonga.miuix.kmp.basic.CircularProgressIndicator(Modifier.size(24.dp)) },
                            onClick = { academicViewModel.load(page) })
                    }
                } else if (LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.COUI) {
                    com.itos.coui.layout.Group {
                        com.itos.coui.layout.Item(title = { UiText(if (academicState.loading) "正在刷新…" else "刷新教务数据") },
                            summary = academicState.fetchedAt?.let { time -> { UiText("更新于 ${campusDateTime(time)}") } },
                            trailing = { if (academicState.loading) UiLoadingIndicator(Modifier.size(24.dp)) },
                            enabled = canRefreshAcademic, onClick = { academicViewModel.load(page) })
                    }
                } else {
                    UiActionButton(
                    onClick = { academicViewModel.load(page) },
                    enabled = canRefreshAcademic,
                    modifier = Modifier.fillMaxWidth(), text = if (academicState.loading) "正在刷新…" else "刷新教务数据")
                academicState.fetchedAt?.let {
                    UiText("更新于 ${campusDateTime(it)}", color = uiStyleColors().secondaryText)
                }
                }
            }
            when (page) {
                "plan" -> PlanContent(academicState.plan, academicState.catalog, academicState.status)
                "grades" -> {
                    if (account.loggedIn) {
                        SemesterPicker(academicState.semesters, academicState.selectedSemesterId, academicViewModel::selectSemester, includeAllGrades = true)
                    }
                    GradesContent(academicState.grades, academicState.status)
                }
                "week" -> {
                    val courseConfig by academicViewModel.courseConfig.collectAsState()
                    if (LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.COUI) {
                        Column {
                            StatusFeedback(academicState.status)
                            com.itos.coui.layout.Group(title = "教学周") {
                                academicState.officialWeek?.let { week ->
                                    com.itos.coui.layout.Item(
                                        title = { com.itos.coui.text.Text(text = "当前教学周", kind = com.itos.coui.text.CouiTextKind.PreferenceTitle) },
                                        trailing = { com.itos.coui.text.Text(text = "第 $week 周", kind = com.itos.coui.text.CouiTextKind.PreferenceValue) },
                                    )
                                }
                                com.itos.coui.layout.Divider()
                                com.itos.coui.layout.Item(
                                    title = { com.itos.coui.text.Text(text = "开学日期", kind = com.itos.coui.text.CouiTextKind.PreferenceTitle) },
                                    summary = {
                                        com.itos.coui.text.Text(
                                            text = courseConfig?.semesterStartDate ?: "当前课表尚未设置开学日期",
                                            kind = com.itos.coui.text.CouiTextKind.PreferenceSummary,
                                            color = com.itos.coui.layout.CouiPageDefaults.themeColors().summary,
                                        )
                                    },
                                )
                                courseConfig?.let { config ->
                                    com.itos.coui.layout.Divider()
                                    com.itos.coui.layout.Item(
                                        title = { com.itos.coui.text.Text(text = "教学周数", kind = com.itos.coui.text.CouiTextKind.PreferenceTitle) },
                                        trailing = {
                                            com.itos.coui.text.Text(
                                                text = "${config.semesterTotalWeeks} 周",
                                                kind = com.itos.coui.text.CouiTextKind.PreferenceValue,
                                            )
                                        },
                                    )
                                }
                                com.itos.coui.layout.Divider()
                                com.itos.coui.layout.Item(
                                    title = { com.itos.coui.text.Text(text = "设置课表日期", kind = com.itos.coui.text.CouiTextKind.PreferenceTitle) },
                                    trailing = {
                                        androidx.compose.material3.Icon(
                                            com.itos.coui.icons.CouiIcons.Next,
                                            contentDescription = null,
                                            modifier = Modifier.size(width = 12.dp, height = 24.dp),
                                            tint = com.itos.coui.layout.CouiPageDefaults.themeColors().summary,
                                        )
                                    },
                                    onClick = onCourseSettings,
                                )
                                com.itos.coui.layout.Divider()
                                com.itos.coui.layout.Item(
                                    enabled = canRefreshAcademic,
                                    title = {
                                        com.itos.coui.text.Text(
                                            text = if (academicState.loading) "正在刷新…" else "刷新教学周",
                                            kind = com.itos.coui.text.CouiTextKind.PreferenceTitle,
                                        )
                                    },
                                    trailing = {
                                        if (academicState.loading) UiLoadingIndicator(Modifier.size(24.dp))
                                        else androidx.compose.material3.Icon(
                                            com.itos.coui.icons.CouiIcons.Next,
                                            contentDescription = null,
                                            modifier = Modifier.size(width = 12.dp, height = 24.dp),
                                            tint = com.itos.coui.layout.CouiPageDefaults.themeColors().summary,
                                        )
                                    },
                                    onClick = if (canRefreshAcademic) ({ academicViewModel.load("week") }) else null,
                                )
                            }
                        }
                    } else ServiceCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusFeedback(academicState.status)
                            academicState.officialWeek?.let { UiText("当前教学周：第 $it 周") }
                            SmallTitle("当前课表开学日期")
                            UiText(courseConfig?.semesterStartDate ?: "当前课表尚未设置开学日期")
                            if (courseConfig?.semesterStartDate != null) {
                                UiText("共 ${courseConfig!!.semesterTotalWeeks} 周")
                            }
                            UiActionButton(onClick = onCourseSettings, modifier = Modifier.fillMaxWidth(), text = "设置当前课表日期")
                            UiActionButton(onClick = { academicViewModel.load("week") }, enabled = canRefreshAcademic, modifier = Modifier.fillMaxWidth(), text = "刷新教学周")
                        }
                    }
                }
                "exams" -> {
                    if (account.loggedIn) {
                        SemesterPicker(academicState.semesters, academicState.selectedSemesterId, academicViewModel::selectSemester)
                    }
                    ExamsContent(academicState.exams, academicState.status)
                }
                "calendar" -> CalendarContent(
                    calendar = snapshot.calendar,
                    service = calendarService,
                    onOpenSource = { onOpen(snapshot.calendar?.pageUrl ?: "https://jwcw.xatu.edu.cn/index/xagydxxl.htm", "教务处校历") },
                    refreshing = calendarRefreshing,
                    statusText = status,
                    onRefresh = {
                        scope.launch { refreshCalendar() }
                    }
                )
                "electricity" -> ElectricityBalanceContent(snapshot) { onOpen(it, "校园缴费平台") }
                "electricitySettings" -> ElectricitySettingsContent(snapshot, campus, campusService)
                "checkinLocation" -> CheckinLocationContent(snapshot, campus)
                else -> UiActionButton(onClick = { onOpen("https://ehall.xatu.edu.cn/", "一站式服务") }, text = "打开一站式服务")
            }
    }
}

@Composable
private fun SmallTitle(text: String) {
    if (LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.MIUIX) {
        top.yukonga.miuix.kmp.basic.SmallTitle(text, insideMargin = PaddingValues(vertical = 8.dp))
    } else UiText(text, style = MaterialTheme.typography.titleSmall, color = uiStyleColors().onSurface)
}

@Composable
private fun ServiceCard(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    if (LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.COUI) {
        com.itos.coui.layout.Group(modifier = modifier) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
        }
        return
    }
    UiCard(modifier) { Column(Modifier.fillMaxWidth().padding(UiMetrics.PageInset), verticalArrangement = Arrangement.spacedBy(UiMetrics.ContentGap), content = content) }
}

@Composable
private fun StatusFeedback(status: String) {
    if (status.isBlank()) return
    UiFeedback(status, when {
        status.contains("正在") -> UiFeedbackKind.LOADING
        status.contains("失败") || status.contains("请输入有效") -> UiFeedbackKind.ERROR
        status.contains("已保存") || status.contains("已更新") -> UiFeedbackKind.SUCCESS
        else -> UiFeedbackKind.INFO
    })
}

@Composable
private fun CheckinLocationContent(snapshot: XatuCampusSnapshot, campus: XatuCampusRepository) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissionStatus by remember { mutableStateOf(locationPermissionStatus(context)) }
    var locationEnabled by remember { mutableStateOf(context.getSystemService(LocationManager::class.java).isLocationEnabled) }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionStatus = locationPermissionStatus(context)
                locationEnabled = context.getSystemService(LocationManager::class.java).isLocationEnabled
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val saved = snapshot.checkinLocation
    var latitude by remember(saved.latitude) { mutableStateOf(saved.latitude.toString()) }
    var longitude by remember(saved.longitude) { mutableStateOf(saved.longitude.toString()) }
    var coordinateSystem by remember(saved.system) { mutableStateOf(saved.system) }
    var status by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var locating by remember { mutableStateOf(false) }
    val supportsEarlyLocation = remember { WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT) }
    fun locate() {
        locating = true
        status = "正在获取当前位置…"
        scope.launch {
            try {
                val location = currentXatuLocation(context)
                latitude = location.latitude.toString()
                longitude = location.longitude.toString()
                coordinateSystem = "WGS84"
                status = "已填入手机当前位置${if (location.hasAccuracy()) "，精度约 ${location.accuracy.toInt()} 米" else ""}"
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                status = "获取定位失败：${error.message}"
            } finally {
                locating = false
            }
        }
    }
    val requestPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        permissionStatus = locationPermissionStatus(context)
        if (grants.values.any { it }) locate() else status = "未授予定位权限"
    }
    val requestCurrentLocation = {
        if (permissionStatus == "未授权") {
            requestPermission.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
        } else locate()
    }
    fun saveLocation(enable: Boolean, useInputs: Boolean) {
        if (enable && !supportsEarlyLocation) {
            status = "当前 WebView 不支持自定义位置"
            return
        }
        val location = try {
            if (useInputs) XatuLocation.checkinLocation(latitude, longitude, enable, coordinateSystem)
            else saved.copy(enabled = enable)
        } catch (error: IllegalArgumentException) {
            status = error.message.orEmpty()
            return
        }
        saving = true
        status = "正在保存签到位置…"
        scope.launch {
            try {
                campus.saveCheckinLocation(location)
                status = if (location.enabled) "签到位置已保存并启用" else "签到位置已保存，自定义位置已停用"
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                status = "签到位置保存失败：${error.message ?: "请重试"}"
            } finally {
                saving = false
            }
        }
    }
    if (LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.COUI) {
        Column {
            com.itos.coui.layout.Group(title = "定位状态") {
                com.itos.coui.layout.Item(
                    title = { com.itos.coui.text.Text(text = "定位权限", kind = com.itos.coui.text.CouiTextKind.PreferenceTitle) },
                    summary = { com.itos.coui.text.Text(text = permissionStatus, kind = com.itos.coui.text.CouiTextKind.PreferenceSummary) },
                )
                com.itos.coui.layout.Divider()
                com.itos.coui.layout.Item(
                    title = { com.itos.coui.text.Text(text = "系统定位", kind = com.itos.coui.text.CouiTextKind.PreferenceTitle) },
                    summary = { com.itos.coui.text.Text(text = if (locationEnabled) "已开启" else "已关闭", kind = com.itos.coui.text.CouiTextKind.PreferenceSummary) },
                )
                com.itos.coui.layout.Divider()
                com.itos.coui.layout.Item(
                    title = { com.itos.coui.text.Text(text = "自定义位置", kind = com.itos.coui.text.CouiTextKind.PreferenceTitle) },
                    summary = {
                        com.itos.coui.text.Text(
                            text = if (saved.enabled && !supportsEarlyLocation) "已启用，但当前 WebView 不支持" else if (saved.enabled) "已启用" else "未启用",
                            kind = com.itos.coui.text.CouiTextKind.PreferenceSummary,
                        )
                    },
                )
            }
            com.itos.coui.layout.Group(title = "签到位置") {
                if (!supportsEarlyLocation) {
                    com.itos.coui.layout.Item(
                        title = { com.itos.coui.text.Text(text = "当前 WebView 不支持自定义位置", kind = com.itos.coui.text.CouiTextKind.PreferenceSummary) },
                    )
                }
                UiInputField(latitude, { latitude = it; status = "" }, Modifier.fillMaxWidth(), enabled = !saving && !locating, label = "纬度", singleLine = true)
                UiInputField(longitude, { longitude = it; status = "" }, Modifier.fillMaxWidth(), enabled = !saving && !locating, label = "经度", singleLine = true)
                CouiAction(
                    text = "坐标系：$coordinateSystem",
                    onClick = { coordinateSystem = if (coordinateSystem == "GCJ02") "WGS84" else "GCJ02" },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !saving && !locating,
                )
                CouiAction(
                    text = if (locating) "正在定位…" else "使用当前位置",
                    onClick = requestCurrentLocation,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !saving && !locating,
                )
                CouiAction(
                    text = "定位权限设置",
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (!locationEnabled) CouiAction(
                    text = "开启系统定位",
                    onClick = { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                StatusFeedback(status)
                CouiAction(
                    text = if (saving) "正在保存…" else "保存签到位置",
                    onClick = { saveLocation(saved.enabled, useInputs = true) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !saving && !locating,
                    filled = true,
                )
            }
            com.itos.coui.layout.Group {
                com.itos.coui.layout.Item(
                    title = { com.itos.coui.text.Text(text = "签到网页使用自定义位置", kind = com.itos.coui.text.CouiTextKind.PreferenceTitle) },
                    trailing = {
                        UiSwitch(
                            checked = saved.enabled,
                            enabled = !saving && !locating && (supportsEarlyLocation || saved.enabled),
                            onCheckedChange = { value -> saveLocation(value, useInputs = value) },
                        )
                    },
                )
            }
        }
        return
    }

    ServiceCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SmallTitle("签到位置")
            UiText("自定义位置：${if (saved.enabled && !supportsEarlyLocation) "已保存启用，但当前 WebView 不支持" else if (saved.enabled) "已启用" else "未启用"}")
            UiText("定位权限：$permissionStatus")
            UiText("系统定位：${if (locationEnabled) "已开启" else "已关闭"}")
            if (!supportsEarlyLocation) UiText("当前 WebView 不支持自定义位置")
            UiActionButton(onClick = {
                if (permissionStatus == "未授权") requestPermission.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
                else locate()
            }, enabled = !saving && !locating, modifier = Modifier.fillMaxWidth(), text = if (locating) "正在定位…" else "使用当前位置")
            UiActionButton(onClick = {
                context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
            }, modifier = Modifier.fillMaxWidth(), text = "定位权限设置")
            if (!locationEnabled) UiActionButton(onClick = {
                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }, modifier = Modifier.fillMaxWidth(), text = "开启系统定位")
            UiActionButton(onClick = { coordinateSystem = if (coordinateSystem == "GCJ02") "WGS84" else "GCJ02" }, enabled = !saving && !locating, text = "坐标系：$coordinateSystem")
            UiInputField(latitude, { latitude = it; status = "" }, Modifier.fillMaxWidth(), enabled = !saving && !locating, label = "纬度", singleLine = true)
            UiInputField(longitude, { longitude = it; status = "" }, Modifier.fillMaxWidth(), enabled = !saving && !locating, label = "经度", singleLine = true)
            StatusFeedback(status)
            UiActionButton(onClick = {
                saveLocation(saved.enabled, useInputs = true)
            }, enabled = !saving && !locating, modifier = Modifier.fillMaxWidth(), text = "保存签到位置")
            Row(verticalAlignment = Alignment.CenterVertically) {
                UiText("签到网页使用自定义位置", modifier = Modifier.weight(1f))
                UiSwitch(
                    checked = saved.enabled,
                    enabled = !saving && !locating && (supportsEarlyLocation || saved.enabled),
                    onCheckedChange = { value ->
                        saveLocation(value, useInputs = value)
                    }
                )
            }
        }
    }
}

private fun locationPermissionStatus(context: Context): String = when {
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED -> "已允许精确定位"
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED -> "仅允许大致定位"
    else -> "未授权"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SemesterPicker(semesters: List<XatuSemester>, selected: String?, onSelect: (String) -> Unit, includeAllGrades: Boolean = false) {
    if (semesters.isEmpty() && !includeAllGrades) return
    var expanded by remember { mutableStateOf(false) }
    if (LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.MATERIAL) {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = if (selected == XATU_ALL_GRADES) "全部历史" else semesters.firstOrNull { it.id == selected }?.name ?: "选择学期",
                onValueChange = {}, readOnly = true, label = { Text("学期") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                if (includeAllGrades) DropdownMenuItem(text = { Text("全部历史") }, onClick = {
                    expanded = false; if (selected != XATU_ALL_GRADES) onSelect(XATU_ALL_GRADES)
                })
                semesters.forEach { semester -> DropdownMenuItem(text = { Text(semester.name) }, onClick = {
                    expanded = false; if (semester.id != selected) onSelect(semester.id)
                }) }
            }
        }
        semesters.firstOrNull { it.id == selected }?.startDate?.takeIf { it.isNotBlank() }?.let { Text("开学日期 $it") }
        return
    }
    if (LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.MIUIX) {
        val options = (if (includeAllGrades) listOf(XATU_ALL_GRADES to "全部历史") else emptyList()) + semesters.map { it.id to it.name }
        top.yukonga.miuix.kmp.basic.Card(Modifier.fillMaxWidth()) {
            MiuixValuePicker("学期", options.map { it.second }, options.indexOfFirst { it.first == selected }) { index ->
                val id = options[index].first
                if (id != selected) onSelect(id)
            }
            semesters.firstOrNull { it.id == selected }?.startDate?.takeIf { it.isNotBlank() }?.let {
                top.yukonga.miuix.kmp.basic.BasicComponent(title = "开学日期", summary = it)
            }
        }
        return
    }
    val options = (if (includeAllGrades) listOf(XATU_ALL_GRADES to "全部历史") else emptyList()) + semesters.map { it.id to it.name }
    com.itos.coui.layout.Group(title = "学期") {
        CouiValuePicker("查询学期", options.map { it.second }, options.indexOfFirst { it.first == selected }) { index ->
            val id = options[index].first
            if (id != selected) onSelect(id)
        }
        semesters.firstOrNull { it.id == selected }?.startDate?.takeIf { it.isNotBlank() }?.let { date ->
            com.itos.coui.layout.Item(title = { UiText("开学日期") }, summary = { UiText(date) })
        }
    }
}


@Composable
private fun GradesContent(page: XatuGradePage?, status: String) {
    var query by rememberSaveable { mutableStateOf("") }
    if (LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.COUI) {
        if (page == null) {
            com.itos.coui.layout.Group {
                StatusFeedback(status)
            }
            return
        }
        val filtered = page.grades.filter { grade ->
            listOf(grade.course, grade.code, grade.term).any { it.contains(query.trim(), ignoreCase = true) }
        }
        Column {
            StatusFeedback(status)
            com.itos.coui.layout.Group(title = "成绩汇总") {
                page.summary.count?.let { value ->
                    com.itos.coui.layout.Item(
                        title = { com.itos.coui.text.Text(text = "课程数", kind = com.itos.coui.text.CouiTextKind.PreferenceTitle) },
                        trailing = { com.itos.coui.text.Text(text = "$value", kind = com.itos.coui.text.CouiTextKind.PreferenceValue) },
                    )
                }
                page.summary.credits?.let { value ->
                    com.itos.coui.layout.Item(
                        title = { com.itos.coui.text.Text(text = "总学分", kind = com.itos.coui.text.CouiTextKind.PreferenceTitle) },
                        trailing = { com.itos.coui.text.Text(text = "$value", kind = com.itos.coui.text.CouiTextKind.PreferenceValue) },
                    )
                }
                page.summary.gpa?.let { value ->
                    com.itos.coui.layout.Item(
                        title = { com.itos.coui.text.Text(text = "平均绩点", kind = com.itos.coui.text.CouiTextKind.PreferenceTitle) },
                        trailing = { com.itos.coui.text.Text(text = "$value", kind = com.itos.coui.text.CouiTextKind.PreferenceValue) },
                    )
                }
            }
            com.itos.coui.layout.Group(title = "成绩明细") {
                UiInputField(query, { query = it }, Modifier.fillMaxWidth(), label = "搜索课程、代码或学期", singleLine = true)
                if (page.grades.isEmpty()) UiFeedback("暂无成绩", UiFeedbackKind.EMPTY)
                else if (filtered.isEmpty()) UiFeedback("没有匹配的成绩", UiFeedbackKind.EMPTY)
                filtered.forEachIndexed { index, grade ->
                    if (index > 0) com.itos.coui.layout.Divider()
                    key(grade.id) { GradeRow(grade) }
                }
            }
        }
        return
    }
    ServiceCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (page == null) {
                StatusFeedback(status)
                return@Column
            }
            StatusFeedback(status)
            page.summary.count?.let { UiText("课程数：$it") }
            page.summary.credits?.let { UiText("总学分：$it") }
            page.summary.gpa?.let { UiText("平均绩点：$it") }
            UiInputField(query, { query = it }, Modifier.fillMaxWidth(), label = "搜索课程、代码或学期", singleLine = true)
            val filtered = page.grades.filter { grade -> listOf(grade.course, grade.code, grade.term).any { it.contains(query.trim(), ignoreCase = true) } }
            if (page.grades.isEmpty()) UiFeedback("暂无成绩", UiFeedbackKind.EMPTY)
            else if (filtered.isEmpty()) UiFeedback("没有匹配的成绩", UiFeedbackKind.EMPTY)
            filtered.forEachIndexed { index, grade ->
                if (index > 0) UiListDivider()
                key(grade.id) { GradeRow(grade) }
            }
        }
    }
}

@Composable
private fun GradeRow(grade: XatuGrade) {
    var expanded by rememberSaveable(grade.id) { mutableStateOf(false) }
    if (LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.MIUIX) {
        top.yukonga.miuix.kmp.basic.BasicComponent(title = grade.course,
            summary = listOf(grade.term, grade.code, grade.credit.takeIf { it.isNotBlank() }?.let { "学分 $it" },
                grade.score.takeIf { it.isNotBlank() }?.let { "成绩 $it" }, grade.gpa.takeIf { it.isNotBlank() }?.let { "绩点 $it" })
                .filterNotNull().filter { it.isNotBlank() }.joinToString(" · "),
            insideMargin = PaddingValues(vertical = 12.dp),
            onClick = if (grade.parts.isNotEmpty()) ({ expanded = !expanded }) else null,
            onClickLabel = if (expanded) "收起成绩构成" else "查看成绩构成",
            role = if (grade.parts.isNotEmpty()) androidx.compose.ui.semantics.Role.Button else null,
            endActions = { if (grade.parts.isNotEmpty()) top.yukonga.miuix.kmp.basic.Text(if (expanded) "−" else "+") },
            bottomAction = { if (expanded) Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                grade.parts.forEach { part -> top.yukonga.miuix.kmp.basic.Text("${part.name}：${part.score}") }
            } })
        return
    }
    if (LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.COUI) {
        val summary = listOf(
            grade.term.takeIf { it.isNotBlank() },
            grade.code.takeIf { it.isNotBlank() },
            grade.credit.takeIf { it.isNotBlank() }?.let { "学分 $it" },
            grade.score.takeIf { it.isNotBlank() }?.let { "成绩 $it" },
            grade.gpa.takeIf { it.isNotBlank() }?.let { "绩点 $it" },
        ).filterNotNull().joinToString(" · ")
        com.itos.coui.layout.Item(
            title = {
                com.itos.coui.text.Text(
                    text = grade.course,
                    kind = com.itos.coui.text.CouiTextKind.PreferenceTitle,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                )
            },
            summary = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (summary.isNotBlank()) com.itos.coui.text.Text(
                        text = summary,
                        kind = com.itos.coui.text.CouiTextKind.PreferenceSummary,
                        color = com.itos.coui.layout.CouiPageDefaults.themeColors().summary,
                    )
                    if (expanded) grade.parts.forEach { part ->
                        com.itos.coui.text.Text(
                            text = "${part.name}：${part.score}",
                            kind = com.itos.coui.text.CouiTextKind.PreferenceSummary,
                            color = com.itos.coui.layout.CouiPageDefaults.themeColors().summary,
                        )
                    }
                }
            },
            onClick = if (grade.parts.isNotEmpty()) ({ expanded = !expanded }) else null,
            trailing = {
                if (grade.parts.isNotEmpty()) {
                    androidx.compose.material3.Icon(
                        com.itos.coui.icons.CouiIcons.Popup,
                        contentDescription = if (expanded) "收起成绩构成" else "展开成绩构成",
                        modifier = Modifier
                            .size(width = 14.dp, height = 24.dp)
                            .then(if (expanded) Modifier.graphicsLayer { rotationZ = 180f } else Modifier),
                        tint = com.itos.coui.layout.CouiPageDefaults.themeColors().summary,
                    )
                }
            },
        )
        return
    }
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        UiText(grade.course, style = MaterialTheme.typography.titleMedium)
        if (grade.code.isNotBlank()) UiText("课程代码/序号：${grade.code}")
        UiText(listOf(grade.term, grade.credit.takeIf { it.isNotBlank() }?.let { "学分 $it" }, grade.score.takeIf { it.isNotBlank() }?.let { "成绩 $it" }, grade.gpa.takeIf { it.isNotBlank() }?.let { "绩点 $it" }).filterNotNull().filter { it.isNotBlank() }.joinToString(" · "))
        if (grade.parts.isNotEmpty()) {
            UiActionButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth(), text = if (expanded) "收起成绩构成" else "查看成绩构成")
            if (expanded) grade.parts.forEach { part -> UiText("${part.name}：${part.score}") }
        }
    }
}

@Composable
private fun ExamsContent(exams: List<XatuExam>?, status: String) {
    if (LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.COUI) {
        if (exams == null) {
            com.itos.coui.layout.Group {
                StatusFeedback(status)
            }
            return
        }
        val today = java.time.LocalDate.now(java.time.ZoneOffset.ofHours(8))
        val sorted = exams.sortedBy { it.startDateTime() ?: java.time.LocalDateTime.MAX }
        com.itos.coui.layout.Group(title = "考试安排") {
            StatusFeedback(status)
            if (sorted.isEmpty()) UiFeedback("暂无考试安排", UiFeedbackKind.EMPTY)
            sorted.forEachIndexed { index, exam ->
                if (index > 0) com.itos.coui.layout.Divider()
                val lines = buildList {
                    exam.startDateTime()?.toLocalDate()?.let { date ->
                        val days = java.time.temporal.ChronoUnit.DAYS.between(today, date)
                        if (days >= 0) add(if (days == 0L) "今天考试" else "距考试 $days 天")
                    }
                    if (exam.code.isNotBlank()) add("课程代码/序号：${exam.code}")
                    if (exam.name.isNotBlank()) add("考试名称：${exam.name}")
                    if (exam.time.isNotBlank()) add("考试日期/时间：${exam.time}")
                    if (exam.room.isNotBlank()) add("考场：${exam.room}")
                    if (exam.seat.isNotBlank()) add("座位号：${exam.seat}")
                    if (exam.note.isNotBlank()) add("备注：${exam.note}")
                }
                com.itos.coui.layout.Item(
                    title = {
                        com.itos.coui.text.Text(
                            text = exam.course,
                            kind = com.itos.coui.text.CouiTextKind.PreferenceTitle,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                        )
                    },
                    summary = if (lines.isEmpty()) null else ({
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            lines.forEach { line ->
                                com.itos.coui.text.Text(
                                    text = line,
                                    kind = com.itos.coui.text.CouiTextKind.PreferenceSummary,
                                    color = com.itos.coui.layout.CouiPageDefaults.themeColors().summary,
                                )
                            }
                        }
                    }),
                )
            }
        }
        return
    }
    ServiceCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (exams == null) {
                StatusFeedback(status)
                return@Column
            }
            StatusFeedback(status)
            if (exams.isEmpty()) UiFeedback("暂无考试安排", UiFeedbackKind.EMPTY)
            val today = java.time.LocalDate.now(java.time.ZoneOffset.ofHours(8))
            exams.sortedBy { it.startDateTime() ?: java.time.LocalDateTime.MAX }.forEachIndexed { index, exam ->
                if (index > 0) UiListDivider()
                val details: @Composable ColumnScope.() -> Unit = {
                    if (LocalUiStyle.current != com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.MIUIX) UiText(exam.course, style = MaterialTheme.typography.titleMedium)
                    exam.startDateTime()?.toLocalDate()?.let { date ->
                        val days = java.time.temporal.ChronoUnit.DAYS.between(today, date)
                        if (days >= 0) UiText(if (days == 0L) "今天考试" else "距考试 $days 天")
                    }
                    if (exam.code.isNotBlank()) UiText("课程代码/序号：${exam.code}")
                    if (exam.name.isNotBlank()) UiText("考试名称：${exam.name}")
                    if (exam.time.isNotBlank()) UiText("考试日期/时间：${exam.time}")
                    if (exam.room.isNotBlank()) UiText("考场：${exam.room}")
                    if (exam.seat.isNotBlank()) UiText("座位号：${exam.seat}")
                    if (exam.note.isNotBlank()) UiText("备注：${exam.note}")
                }
                if (LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.MIUIX) {
                    top.yukonga.miuix.kmp.basic.BasicComponent(title = exam.course,
                        insideMargin = PaddingValues(vertical = 12.dp), bottomAction = {
                            Column(Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp), content = details)
                        })
                } else if (LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.COUI) {
                    com.itos.coui.layout.Item(title = { Column(verticalArrangement = Arrangement.spacedBy(4.dp), content = details) })
                } else Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp), content = details)
            }
        }
    }
}

@Composable
private fun PlanContent(plan: XatuStudyPlan?, catalog: List<XatuCourseInfo>, status: String) {
    var query by rememberSaveable { mutableStateOf("") }
    fun matches(section: XatuPlanSection): Boolean = section.courses.any {
        it.name.contains(query.trim(), true) || it.code.contains(query.trim(), true)
    } || section.children.any(::matches)

    if (LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.COUI) {
        if (plan == null) {
            com.itos.coui.layout.Group {
                StatusFeedback(status)
            }
            return
        }
        Column {
            StatusFeedback(status)
            com.itos.coui.layout.Group(title = "学业概况") {
                com.itos.coui.layout.Item(
                    title = { com.itos.coui.text.Text(text = "已修学分", kind = com.itos.coui.text.CouiTextKind.PreferenceTitle) },
                    trailing = {
                        com.itos.coui.text.Text(
                            text = "${plan.earnedCredits} / ${plan.requiredCredits}",
                            kind = com.itos.coui.text.CouiTextKind.PreferenceValue,
                        )
                    },
                )
                if (plan.studentName.isNotBlank()) com.itos.coui.layout.Item(
                    title = { com.itos.coui.text.Text(text = "姓名", kind = com.itos.coui.text.CouiTextKind.PreferenceTitle) },
                    trailing = { com.itos.coui.text.Text(text = plan.studentName, kind = com.itos.coui.text.CouiTextKind.PreferenceValue) },
                )
                if (plan.major.isNotBlank()) com.itos.coui.layout.Item(
                    title = { com.itos.coui.text.Text(text = "专业", kind = com.itos.coui.text.CouiTextKind.PreferenceTitle) },
                    trailing = { com.itos.coui.text.Text(text = plan.major, kind = com.itos.coui.text.CouiTextKind.PreferenceValue) },
                )
                if (plan.gpa.isNotBlank()) com.itos.coui.layout.Item(
                    title = { com.itos.coui.text.Text(text = "GPA", kind = com.itos.coui.text.CouiTextKind.PreferenceTitle) },
                    trailing = { com.itos.coui.text.Text(text = plan.gpa, kind = com.itos.coui.text.CouiTextKind.PreferenceValue) },
                )
                UiInputField(query, { query = it }, Modifier.fillMaxWidth(), label = "搜索学业课程或代码", singleLine = true)
            }
            if (query.isNotBlank() && plan.sections.none(::matches)) {
                UiFeedback("没有匹配的学业课程", UiFeedbackKind.EMPTY)
            }
            plan.sections.forEach { section -> PlanSectionContent(section, catalog, query.trim()) }
            PlanCatalogContent(catalog)
        }
        return
    }

    ServiceCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SmallTitle("学业完成情况")
            if (plan == null) {
                StatusFeedback(status)
                return@Column
            }
            UiText("已修 ${plan.earnedCredits} / 要求 ${plan.requiredCredits} 学分")
            if (plan.studentName.isNotBlank()) UiText("姓名：${plan.studentName}")
            if (plan.major.isNotBlank()) UiText(plan.major)
            if (plan.gpa.isNotBlank()) UiText("GPA：${plan.gpa}")
            StatusFeedback(status)
            UiInputField(query, { query = it }, Modifier.fillMaxWidth(), label = "搜索学业课程或代码", singleLine = true)
        }
    }
    if (plan != null) {
        if (query.isNotBlank() && plan.sections.none(::matches)) UiFeedback("没有匹配的学业课程", UiFeedbackKind.EMPTY)
        plan.sections.forEach { section -> PlanSectionContent(section, catalog, query.trim()) }
        PlanCatalogContent(catalog)
    }
}

@Composable
private fun PlanSectionContent(section: XatuPlanSection, catalog: List<XatuCourseInfo>, query: String) {
    val courses = section.courses.filter { it.name.contains(query, true) || it.code.contains(query, true) }
    if (LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.COUI) {
        if (query.isBlank() || courses.isNotEmpty()) {
            com.itos.coui.layout.Group(
                modifier = Modifier.fillMaxWidth(),
                title = section.title,
                footer = "已修 ${section.earnedCredits} / 要求 ${section.requiredCredits} 学分",
            ) {
                courses.forEachIndexed { index, course ->
                    if (index > 0) com.itos.coui.layout.Divider()
                    val info = catalog.firstOrNull { it.code == course.code || it.name == course.name }
                    val summary = listOfNotNull(
                        course.code.takeIf { it.isNotBlank() }?.let { "课程代码 $it" },
                        "${course.earnedCredits}/${course.requiredCredits} 学分",
                        if (course.passed) "已通过" else "未通过",
                        course.score.takeIf { it.isNotBlank() }?.let { "成绩 $it" },
                        info?.term?.takeIf { it.isNotBlank() }?.let { "第 $it 学期" },
                    ).joinToString(" · ")
                    com.itos.coui.layout.Item(
                        title = {
                            com.itos.coui.text.Text(
                                text = course.name,
                                kind = com.itos.coui.text.CouiTextKind.PreferenceTitle,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                            )
                        },
                        summary = {
                            com.itos.coui.text.Text(
                                text = summary,
                                kind = com.itos.coui.text.CouiTextKind.PreferenceSummary,
                                color = com.itos.coui.layout.CouiPageDefaults.themeColors().summary,
                            )
                        },
                    )
                }
            }
        }
        section.children.forEach { child -> PlanSectionContent(child, catalog, query) }
        return
    }
    if (query.isBlank() || courses.isNotEmpty()) {
    ServiceCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            UiText(section.title)
            UiText("已修 ${section.earnedCredits} / 要求 ${section.requiredCredits} 学分")
            courses.forEach { course ->
                val info = catalog.firstOrNull { it.code == course.code || it.name == course.name }
                val summary = listOfNotNull(
                    course.code.takeIf { it.isNotBlank() }?.let { "课程代码 $it" },
                    "${course.earnedCredits}/${course.requiredCredits} 学分",
                    if (course.passed) "已通过" else "未通过",
                    course.score.takeIf { it.isNotBlank() }?.let { "成绩 $it" },
                    info?.term?.takeIf { it.isNotBlank() }?.let { "第 $it 学期" },
                ).joinToString(" · ")
                when (LocalUiStyle.current) {
                    com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.MATERIAL ->
                        ListItem(
                            headlineContent = { Text(course.name) },
                            supportingContent = { Text(summary) },
                            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                        )
                    com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.MIUIX ->
                        top.yukonga.miuix.kmp.basic.BasicComponent(
                            title = course.name,
                            summary = summary,
                            insideMargin = PaddingValues(vertical = 8.dp),
                        )
                    com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.COUI ->
                        com.itos.coui.layout.Item(
                            title = {
                                com.itos.coui.text.Text(
                                    text = course.name,
                                    kind = com.itos.coui.text.CouiTextKind.PreferenceTitle,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                                )
                            },
                            summary = {
                                com.itos.coui.text.Text(
                                    text = summary,
                                    kind = com.itos.coui.text.CouiTextKind.PreferenceSummary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                                )
                            },
                        )
                }
            }
        }
    }
    }
    section.children.forEach { child -> PlanSectionContent(child, catalog, query) }
}

@Composable
private fun PlanCatalogContent(catalog: List<XatuCourseInfo>) {
    var open by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var term by rememberSaveable { mutableStateOf("") }
    if (LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.COUI) {
        com.itos.coui.layout.Group(title = "培养方案") {
            if (catalog.isEmpty()) {
                UiFeedback("暂无培养方案课程", UiFeedbackKind.EMPTY)
            } else {
                com.itos.coui.layout.Item(
                    title = {
                        com.itos.coui.text.Text(
                            text = "查看全部课程",
                            kind = com.itos.coui.text.CouiTextKind.PreferenceTitle,
                        )
                    },
                    summary = {
                        com.itos.coui.text.Text(
                            text = "${catalog.size} 门课程",
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
                    onClick = { open = true },
                )
            }
        }
    } else {
        ServiceCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallTitle("培养方案")
                if (catalog.isEmpty()) UiFeedback("暂无培养方案课程", UiFeedbackKind.EMPTY)
                else UiActionButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth(), text = "查看全部 ${catalog.size} 门课程")
            }
        }
    }
    if (open) UiDialog(
        onDismissRequest = { open = false },
        title = "培养方案",
        text = {
            Column {
            UiInputField(query, { query = it }, Modifier.fillMaxWidth(), label = "课程名称或代码", singleLine = true)
            UiInputField(term, { term = it }, Modifier.fillMaxWidth(), label = "计划学期（留空显示全部）", singleLine = true)
            val filtered = catalog.filter { (it.name.contains(query.trim(), true) || it.code.contains(query.trim(), true)) && it.term.contains(term.trim(), true) }
            if (filtered.isEmpty()) UiText("没有匹配的培养方案课程")
            LazyColumn(Modifier.height(360.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(filtered) { course ->
                    val summary = listOfNotNull(
                        course.code.takeIf { it.isNotBlank() }?.let { "课程代码 $it" },
                        course.nature.takeIf { it.isNotBlank() },
                        course.credits.takeIf { it.isNotBlank() }?.let { "$it 学分" },
                        course.hours.takeIf { it.isNotBlank() }?.let { "$it 学时" },
                        course.term.takeIf { it.isNotBlank() }?.let { "计划学期 $it" },
                    ).joinToString(" · ")
                    when (LocalUiStyle.current) {
                        com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.MATERIAL ->
                            ListItem(
                                headlineContent = { Text(course.name) },
                                supportingContent = { Text(summary) },
                                colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                            )
                        com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.MIUIX ->
                            top.yukonga.miuix.kmp.basic.BasicComponent(
                                title = course.name,
                                summary = summary,
                                insideMargin = PaddingValues(vertical = 8.dp),
                            )
                        com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.COUI ->
                            com.itos.coui.layout.Item(
                                title = {
                                    com.itos.coui.text.Text(
                                        text = course.name,
                                        kind = com.itos.coui.text.CouiTextKind.PreferenceTitle,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                                    )
                                },
                                summary = {
                                    com.itos.coui.text.Text(
                                        text = summary,
                                        kind = com.itos.coui.text.CouiTextKind.PreferenceSummary,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                                    )
                                },
                            )
                    }
                }
            }
            }
        },
        confirmButton = UiDialogAction(onClick = { open = false }, text = "关闭"),
    )
}

@Composable
private fun CalendarContent(
    calendar: XatuCalendar?,
    service: XatuCalendarService,
    onOpenSource: () -> Unit,
    refreshing: Boolean,
    statusText: String,
    onRefresh: () -> Unit,
) {
    var showOriginal by rememberSaveable(calendar?.imageUrl) { mutableStateOf(false) }
    val rawText = calendar?.rawText.orEmpty()
    if (LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.COUI) {
        val pageColors = com.itos.coui.layout.CouiPageDefaults.themeColors()
        val semesters = calendar?.semesters.orEmpty()
        Column {
            com.itos.coui.layout.Group(title = "学期日期") {
                calendar?.let {
                    com.itos.coui.layout.Item(
                        title = {
                            com.itos.coui.text.Text(
                                text = it.title,
                                kind = com.itos.coui.text.CouiTextKind.PreferenceTitle,
                            )
                        },
                        summary = it.fetchedAt.takeIf { at -> at > 0 }?.let { at ->
                            {
                                com.itos.coui.text.Text(
                                    text = "更新于 ${campusDateTime(at)}",
                                    kind = com.itos.coui.text.CouiTextKind.PreferenceSummary,
                                    color = pageColors.summary,
                                )
                            }
                        },
                    )
                }
                StatusFeedback(statusText)
                semesters.forEachIndexed { index, semester ->
                    com.itos.coui.layout.Item(
                        title = {
                            com.itos.coui.text.Text(
                                text = semester.label,
                                kind = com.itos.coui.text.CouiTextKind.PreferenceTitle,
                            )
                        },
                        summary = {
                            com.itos.coui.text.Text(
                                text = "${semester.start} 至 ${semester.end}",
                                kind = com.itos.coui.text.CouiTextKind.PreferenceSummary,
                                color = pageColors.summary,
                            )
                        },
                    )
                    if (index < semesters.lastIndex) com.itos.coui.layout.Divider()
                }
                if (calendar != null && calendar.semesters.isEmpty()) {
                    com.itos.coui.layout.Item(
                        title = {
                            com.itos.coui.text.Text(
                                text = "未识别到学期日期",
                                kind = com.itos.coui.text.CouiTextKind.PreferenceSummary,
                                color = pageColors.summary,
                            )
                        },
                    )
                }
                com.itos.coui.layout.Divider()
                com.itos.coui.layout.Item(
                    enabled = !refreshing,
                    title = {
                        com.itos.coui.text.Text(
                            text = if (refreshing) "正在刷新校历…" else "刷新校历",
                            kind = com.itos.coui.text.CouiTextKind.PreferenceTitle,
                        )
                    },
                    trailing = {
                        if (refreshing) UiLoadingIndicator(Modifier.size(24.dp))
                        else androidx.compose.material3.Icon(
                            com.itos.coui.icons.CouiIcons.Next,
                            contentDescription = null,
                            modifier = Modifier.size(width = 12.dp, height = 24.dp),
                            tint = pageColors.summary,
                        )
                    },
                    onClick = if (refreshing) null else onRefresh,
                )
                if (calendar != null && (calendar.imagePath.isNotBlank() || calendar.imageUrl.isNotBlank())) {
                    com.itos.coui.layout.Divider()
                    com.itos.coui.layout.Item(
                        title = { com.itos.coui.text.Text(text = "查看原图", kind = com.itos.coui.text.CouiTextKind.PreferenceTitle) },
                        trailing = {
                            androidx.compose.material3.Icon(
                                com.itos.coui.icons.CouiIcons.Next,
                                contentDescription = null,
                                modifier = Modifier.size(width = 12.dp, height = 24.dp),
                                tint = pageColors.summary,
                            )
                        },
                        onClick = { showOriginal = true },
                    )
                }
                com.itos.coui.layout.Divider()
                com.itos.coui.layout.Item(
                    title = { com.itos.coui.text.Text(text = "教务处原文", kind = com.itos.coui.text.CouiTextKind.PreferenceTitle) },
                    trailing = {
                        androidx.compose.material3.Icon(
                            com.itos.coui.icons.CouiIcons.Next,
                            contentDescription = null,
                            modifier = Modifier.size(width = 12.dp, height = 24.dp),
                            tint = pageColors.summary,
                        )
                    },
                    onClick = onOpenSource,
                )
            }
            if (rawText.isNotBlank()) {
                com.itos.coui.layout.Group(title = "校历识别文字") {
                    androidx.compose.foundation.layout.Column(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        com.itos.coui.text.Text(
                            text = rawText,
                            kind = com.itos.coui.text.CouiTextKind.Body,
                            color = com.itos.coui.theme.CouiTheme.colorScheme.onSurface,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                        )
                        val context = LocalContext.current
                        CouiAction(
                            text = "复制识别文字",
                            onClick = {
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("校历识别文字", rawText))
                                ToastManager.show("识别文字已复制")
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            } else if (calendar != null) {
                com.itos.coui.layout.Group {
                    com.itos.coui.layout.Item(
                        title = {
                            com.itos.coui.text.Text(
                                text = "未识别到文字",
                                kind = com.itos.coui.text.CouiTextKind.PreferenceSummary,
                                color = pageColors.summary,
                            )
                        },
                    )
                }
            }
        }
        if (calendar != null && showOriginal) {
            val original by produceState<Result<String>?>(null, calendar) {
                value = try {
                    Result.success(service.originalImage(calendar))
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    Result.failure(error)
                }
            }
            val image = original?.getOrNull()
            if (image != null) XatuCalendarImage(image, calendar.fetchedAt) { showOriginal = false }
            else UiDialog(onDismissRequest = { showOriginal = false }, title = "校历原图", text = {
                val failure = original?.exceptionOrNull()
                if (failure == null) UiFeedback("正在读取原图…", UiFeedbackKind.LOADING)
                else UiFeedback(failure.message ?: "原图读取失败", UiFeedbackKind.ERROR)
            }, dismissButton = UiDialogAction("关闭", { showOriginal = false }))
        }
        return
    }
    ServiceCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            calendar?.let {
                UiText(it.title)
                if (it.fetchedAt > 0) UiText("校历获取时间：${campusDateTime(it.fetchedAt)}")
            }
            StatusFeedback(statusText)
            calendar?.semesters.orEmpty().forEach { semester ->
                UiText("${semester.label}：${semester.start} 至 ${semester.end}")
            }
            if (calendar != null && calendar.semesters.isEmpty()) UiText("未识别到学期日期")
            UiActionButton(onClick = onRefresh, enabled = !refreshing,
                modifier = Modifier.fillMaxWidth(), text = if (refreshing) "正在刷新…" else "刷新校历")
            if (calendar != null && (calendar.imagePath.isNotBlank() || calendar.imageUrl.isNotBlank())) {
                UiActionButton(
                    onClick = { showOriginal = true },
                    modifier = Modifier.fillMaxWidth(), text = "查看原图")
            }
            if (rawText.isNotBlank()) {
                UiText("校历识别文字")
                UiText(rawText)
                val context = LocalContext.current
                UiActionButton(
                    onClick = {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("校历识别文字", rawText))
                        ToastManager.show("识别文字已复制")
                    },
                    modifier = Modifier.fillMaxWidth(), text = "复制识别文字")
            } else if (calendar != null) UiText("未识别到文字")
            UiActionButton(onClick = onOpenSource, modifier = Modifier.fillMaxWidth(), text = "教务处原文")
        }
    }
    if (calendar != null && showOriginal) {
        val original by produceState<Result<String>?>(null, calendar) {
            value = try {
                Result.success(service.originalImage(calendar))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Result.failure(error)
            }
        }
        val image = original?.getOrNull()
        if (image != null) XatuCalendarImage(image, calendar.fetchedAt) { showOriginal = false }
        else UiDialog(onDismissRequest = { showOriginal = false }, title = "校历原图", text = {
            val failure = original?.exceptionOrNull()
            if (failure == null) UiFeedback("正在读取原图…", UiFeedbackKind.LOADING)
            else UiFeedback(failure.message ?: "原图读取失败", UiFeedbackKind.ERROR)
        }, dismissButton = UiDialogAction("关闭", { showOriginal = false }))
    }
}

internal fun campusDateTime(at: Long): String = java.time.Instant.ofEpochMilli(at)
    .atZone(java.time.ZoneId.of("Asia/Shanghai"))
    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
