package com.xingheyuzhuan.shiguangschedule.ui.settings.xatu

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.Destination
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuAccount
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuAccountRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuSessionRepository
import com.xingheyuzhuan.shiguangschedule.ui.components.*
import com.xingheyuzhuan.shiguangschedule.xatu.academic.XATU_CAS_LOGIN_URL
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.koinInject
import shiguangschedule.shared.generated.resources.*

internal data class PortalServiceAction(val title: String, val summary: String, val icon: DrawableResource, val destination: Destination)

private fun webService(title: String, summary: String, icon: DrawableResource, url: String) =
    PortalServiceAction(title, summary, icon, Destination.WebView(initialUrl = url, serviceTitle = title))

internal fun xatuLoginDestination(courseSyncTableId: String? = null): Destination =
    Destination.WebView(XATU_CAS_LOGIN_URL, xatuLogin = true, courseSyncTableId = courseSyncTableId)

internal val academicServices = listOf(
    PortalServiceAction("成绩查询", "成绩、学分与绩点", Res.drawable.award_star_24px, Destination.XatuService("grades")),
    PortalServiceAction("考试安排", "时间、考场与座位号", Res.drawable.schedule_24px, Destination.XatuService("exams")),
    PortalServiceAction("学业进度", "已修学分与培养方案", Res.drawable.done_all_24px, Destination.XatuService("plan")),
    PortalServiceAction("教学周", "官方周次与课表日期参考", Res.drawable.view_week_24px, Destination.XatuService("week")),
    PortalServiceAction("校历", "学期日期、原图与识别文字", Res.drawable.calendar_today_24px, Destination.XatuService("calendar")),
    webService("教务系统网页", "教务系统首页", Res.drawable.school_24px, "http://jwgl2018.xatu.edu.cn/"),
)
internal val campusServices = listOf(
    PortalServiceAction("宿舍电费", "剩余电量、查询记录与趋势", Res.drawable.bolt_24px, Destination.XatuService("electricity")),
    webService("校园签到", "今日签到", Res.drawable.location_on_24px, "https://ehall.xatu.edu.cn/xsfw/sys/swmqdzsapp/*default/index.do#/jrqd"),
    webService("一站式大厅", "校园办事", Res.drawable.home_24px, "https://ehall.xatu.edu.cn/"),
)
internal val campusSettings = listOf(
    PortalServiceAction("电费与宿舍设置", "电费项目、校区、楼栋与房间", Res.drawable.bolt_24px, Destination.XatuService("electricitySettings")),
    PortalServiceAction("签到位置设置", "定位权限与自定义位置", Res.drawable.location_on_24px, Destination.XatuService("checkinLocation")),
)
private val webServices = listOf(
    webService("请销假", "申请与记录", Res.drawable.sticky_note_2_24px, "https://ehall.xatu.edu.cn/xsfw/sys/swmxsqjapp/*default/index.do?wxType=1"),
    webService("图书馆", "馆藏与借阅", Res.drawable.school_24px, "https://lib.xatu.edu.cn/"),
    webService("校园网", "充值与套餐", Res.drawable.cloud_24px, "https://self.xatu.edu.cn/"),
    webService("电费充值", "缴费平台", Res.drawable.bolt_24px, XATU_PAY_LOGIN),
    webService("一网通办", "综合办事", Res.drawable.home_24px, "https://ywtb.xatu.edu.cn/"),
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun XatuPage(
    title: String,
    onBack: (() -> Unit)? = null,
    largeTitle: Boolean = onBack == null,
    content: @Composable (PaddingValues) -> Unit,
) {
    UiScaffold(topBar = { UiTopAppBar(title, onBack = onBack, large = largeTitle) }) { padding ->
        Box(Modifier.fillMaxSize().uiContentWidth()) { content(padding) }
    }
}

@Composable
actual fun XatuLoginScreen(onNavigate: (Destination) -> Unit, onBack: () -> Unit, courseSyncTableId: String?) {
    val accountRepository: XatuAccountRepository = koinInject()
    val sessionRepository: XatuSessionRepository = koinInject()
    val savedAccount by accountRepository.account.collectAsState(initial = XatuAccount("", false))
    var account by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var status by rememberSaveable { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    var confirmLogout by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(savedAccount.studentNumber) { account = savedAccount.studentNumber }
    XatuPage("账号与隐私", onBack) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(if (LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.COUI) 0.dp else UiMetrics.PageInset), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                AccountPanel(savedAccount, account, { account = it; status = "" }, busy, status,
                    onLogin = {
                        busy = true
                        status = "正在准备登录…"
                        scope.launch {
                            try {
                                val next = account.trim()
                                check(savedAccount.studentNumber.isBlank() || savedAccount.studentNumber == next) { "切换账号前请先退出当前登录" }
                                accountRepository.saveStudentNumber(next)
                                status = ""
                                onNavigate(xatuLoginDestination(courseSyncTableId))
                            } catch (error: CancellationException) { throw error
                            } catch (error: Exception) { status = "登录准备失败：${error.message}"
                            } finally { busy = false }
                        }
                    }
                )
            }
            item {
                SectionHeading("本机数据与隐私")
                UiCard(Modifier.fillMaxWidth()) {
                    UiServiceRow("数据备份与恢复", "", { onNavigate(Destination.BackupAndRestore) })
                    UiServiceRow("系统存储与权限", "", {
                        context.startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:${context.packageName}")))
                    })
                }
            }
            if (savedAccount.studentNumber.isNotBlank()) item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    when (LocalUiStyle.current) {
                        com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.MATERIAL ->
                            TextButton(
                                onClick = { confirmLogout = true },
                                enabled = !busy,
                                modifier = Modifier.heightIn(min = UiMetrics.TouchTarget),
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            ) { Text("退出登录") }
                        com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.MIUIX ->
                            top.yukonga.miuix.kmp.basic.TextButton(
                                text = "退出登录",
                                onClick = { confirmLogout = true },
                                modifier = Modifier.heightIn(min = UiMetrics.TouchTarget),
                                enabled = !busy,
                                colors = top.yukonga.miuix.kmp.basic.ButtonDefaults.textButtonColors(
                                    color = androidx.compose.ui.graphics.Color.Transparent,
                                    disabledColor = androidx.compose.ui.graphics.Color.Transparent,
                                    textColor = uiStyleColors().secondaryText,
                                    disabledTextColor = uiStyleColors().secondaryText.copy(alpha = 0.38f),
                                ),
                            )
                        com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.COUI ->
                            UiButton(
                                text = "退出登录",
                                onClick = { confirmLogout = true },
                                minHeight = UiMetrics.TouchTarget,
                                enabled = !busy,
                                contentColor = uiStyleColors().secondaryText,
                            )
                    }
                }
            }
        }
    }
    if (confirmLogout) UiDialog(
        onDismissRequest = { confirmLogout = false },
        title = "退出登录？",
        text = { UiText("清除登录状态和本机校园数据，保留课程表。") },
        confirmButton = UiDialogAction(onClick = {
            confirmLogout = false
                        busy = true
                        status = "正在退出登录…"
                        scope.launch {
                            try {
                                sessionRepository.clear()
                                account = ""
                                status = "已退出登录"
                            } catch (error: CancellationException) { throw error
                            } catch (error: Exception) { status = "退出登录失败：${error.message}"
                            } finally { busy = false }
                        }
        }, text = "确认退出"),
        dismissButton = UiDialogAction(onClick = { confirmLogout = false }, text = "取消")
    )
}

@Composable
internal fun XatuFirstLoginDialog(
    onDismiss: () -> Unit,
    courseSyncTableId: String? = null,
    onNavigate: (Destination) -> Unit,
) {
    val accountRepository: XatuAccountRepository = koinInject()
    val scope = rememberCoroutineScope()
    var studentNumber by rememberSaveable { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    UiDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = "登录教务",
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                UiInputField(
                    value = studentNumber,
                    onValueChange = { studentNumber = it; error = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = "学号",
                    enabled = !saving,
                    singleLine = true,
                )
                error?.let { UiFeedback(it, UiFeedbackKind.ERROR) }
            }
        },
        confirmButton = UiDialogAction(
            text = if (saving) "正在准备…" else "继续登录",
            enabled = studentNumber.trim().isNotEmpty() && !saving,
            onClick = {
                if (!saving) {
                    val number = studentNumber.trim()
                    saving = true
                    error = null
                    scope.launch {
                        try {
                            accountRepository.saveStudentNumber(number)
                            onNavigate(xatuLoginDestination(courseSyncTableId))
                        } catch (exception: CancellationException) {
                            throw exception
                        } catch (exception: Exception) {
                            error = "保存学号失败：${exception.message}"
                        } finally {
                            saving = false
                        }
                    }
                }
            },
        ),
        dismissButton = UiDialogAction(
            text = "取消",
            enabled = !saving,
            onClick = onDismiss,
        ),
    )
}

@Composable
actual fun XatuAcademicScreen(onNavigate: (Destination) -> Unit) {
    ServiceDirectory("教务", listOf("" to academicServices), onNavigate)
}

@Composable
actual fun XatuServicesScreen(onNavigate: (Destination) -> Unit, onBack: () -> Unit) {
    ServiceDirectory("校园服务", listOf(
        "宿舍与缴费" to (campusServices.filter { (it.destination as? Destination.XatuService)?.page == "electricity" } + webServices.filter { it.title == "电费充值" }),
        "日常办理" to (campusServices.filter { it.title == "校园签到" } + webServices.filter { it.title == "请销假" }),
        "学习资源与网络" to webServices.filter { it.title in listOf("图书馆", "校园网") },
        "综合办事" to (campusServices.filter { it.title == "一站式大厅" } + webServices.filter { it.title == "一网通办" })
    ), onNavigate)
}

@Composable
private fun ServiceDirectory(title: String, groups: List<Pair<String, List<PortalServiceAction>>>, onNavigate: (Destination) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val visible = groups.map { (name, entries) -> name to entries.filter { it.title.contains(query.trim(), true) || it.summary.contains(query.trim(), true) } }
    XatuPage(title, largeTitle = false) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(start = if (LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.COUI) 0.dp else UiMetrics.PageInset,
                end = if (LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.COUI) 0.dp else UiMetrics.PageInset, top = 8.dp, bottom = UiMetrics.PageInset),
            verticalArrangement = Arrangement.spacedBy(
                if (LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.COUI) 0.dp else 16.dp
            )) {
            item {
                if (LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.COUI) {
                    val pageColors = com.itos.coui.layout.CouiPageDefaults.themeColors()
                    com.itos.coui.layout.Group {
                        com.itos.coui.layout.Item(
                            icon = {
                                androidx.compose.material3.Icon(
                                    com.itos.coui.icons.CouiIcons.SearchField,
                                    contentDescription = null,
                                    tint = pageColors.summary,
                                )
                            },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            title = {
                                androidx.compose.foundation.text.BasicTextField(
                                    value = query,
                                    onValueChange = { query = it },
                                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "搜索$title" },
                                    singleLine = true,
                                    textStyle = com.itos.coui.text.rememberCouiTextStyle(com.itos.coui.text.CouiTextKind.Search)
                                        .copy(color = com.itos.coui.theme.CouiTheme.colorScheme.onSurface),
                                    cursorBrush = SolidColor(com.itos.coui.theme.CouiTheme.colorScheme.accent),
                                    decorationBox = { input ->
                                        androidx.compose.foundation.layout.Box {
                                            if (query.isEmpty()) {
                                                com.itos.coui.text.Text(
                                                    text = "搜索$title",
                                                    kind = com.itos.coui.text.CouiTextKind.Search,
                                                    color = pageColors.summary,
                                                )
                                            }
                                            input()
                                        }
                                    },
                                )
                            },
                            trailing = {
                                if (query.isNotEmpty()) {
                                    androidx.compose.foundation.layout.Box(
                                        Modifier.size(48.dp)
                                            .semantics { contentDescription = "清除搜索" }
                                            .clickable(role = androidx.compose.ui.semantics.Role.Button) { query = "" },
                                        contentAlignment = androidx.compose.ui.Alignment.Center,
                                    ) {
                                        androidx.compose.material3.Icon(
                                            com.itos.coui.icons.CouiIcons.Close,
                                            contentDescription = null,
                                            tint = pageColors.summary,
                                        )
                                    }
                                }
                            },
                        )
                    }
                } else {
                    UiInputField(query, { query = it }, Modifier.fillMaxWidth(), label = "搜索$title", singleLine = true)
                }
            }
            if (visible.all { it.second.isEmpty() }) item { UiFeedback("没有匹配的服务", UiFeedbackKind.EMPTY) }
            visible.filter { it.second.isNotEmpty() }.forEach { (name, entries) ->
                item(key = name) {
                    when (LocalUiStyle.current) {
                        com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.MATERIAL -> {
                            if (name.isNotEmpty()) SectionHeading(name)
                            Column(Modifier.fillMaxWidth()) {
                                entries.forEachIndexed { index, entry ->
                                    val summary = if (entry.destination is Destination.WebView) "网页" else entry.summary
                                    ListItem(
                                        headlineContent = { Text(entry.title) },
                                        leadingContent = { Icon(vectorResource(entry.icon), null) },
                                        trailingContent = { Icon(vectorResource(Res.drawable.chevron_right_24px), null) },
                                        supportingContent = if (summary.isNotBlank()) ({ Text(summary) }) else null,
                                        modifier = Modifier.clickable(role = androidx.compose.ui.semantics.Role.Button) { onNavigate(entry.destination) },
                                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                                    )
                                    if (index < entries.lastIndex) HorizontalDivider(Modifier.padding(start = 56.dp))
                                }
                            }
                        }
                        com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.MIUIX -> {
                            if (name.isNotEmpty()) SectionHeading(name)
                            top.yukonga.miuix.kmp.basic.Card(Modifier.fillMaxWidth()) {
                                entries.forEach { entry ->
                                    val summary = if (entry.destination is Destination.WebView) "网页" else entry.summary.takeIf { it.isNotBlank() }
                                    top.yukonga.miuix.kmp.basic.BasicComponent(
                                        title = entry.title,
                                        summary = summary,
                                        startAction = { top.yukonga.miuix.kmp.basic.Icon(vectorResource(entry.icon), contentDescription = null) },
                                        endActions = { top.yukonga.miuix.kmp.basic.Icon(vectorResource(Res.drawable.chevron_right_24px), contentDescription = null) },
                                        insideMargin = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                                        onClick = { onNavigate(entry.destination) },
                                    )
                                }
                            }
                        }
                        com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.COUI -> com.itos.coui.layout.Group(title = name.takeIf { it.isNotEmpty() }) {
                            entries.forEachIndexed { index, entry ->
                                val summary = if (entry.destination is Destination.WebView) "网页" else entry.summary
                                com.itos.coui.layout.Item(
                                    icon = {
                                        Icon(
                                            vectorResource(entry.icon),
                                            contentDescription = null,
                                            tint = com.itos.coui.theme.CouiTheme.colorScheme.onSurfaceVariant,
                                        )
                                    },
                                    title = { com.itos.coui.text.Text(text = entry.title, kind = com.itos.coui.text.CouiTextKind.PreferenceTitle,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Start) },
                                    summary = if (summary.isNotBlank()) ({
                                        com.itos.coui.text.Text(
                                            text = summary,
                                            kind = com.itos.coui.text.CouiTextKind.PreferenceSummary,
                                            color = com.itos.coui.layout.CouiPageDefaults.themeColors().summary,
                                        )
                                    }) else null,
                                    trailing = {
                                        Icon(
                                            com.itos.coui.icons.CouiIcons.Next,
                                            contentDescription = null,
                                            modifier = Modifier.size(width = 12.dp, height = 24.dp),
                                            tint = com.itos.coui.layout.CouiPageDefaults.themeColors().summary,
                                        )
                                    },
                                    onClick = { onNavigate(entry.destination) })
                                if (index < entries.lastIndex) {
                                    com.itos.coui.layout.Divider(
                                        startIndent = com.itos.coui.layout.CouiPageDefaults.rowPadding +
                                            com.itos.coui.layout.CouiPageDefaults.iconSize +
                                            com.itos.coui.layout.CouiPageDefaults.iconSpacing,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
actual fun XatuServiceScreen(page: String, onNavigate: (Destination) -> Unit, onBack: () -> Unit) {
    val title = (academicServices + campusServices + campusSettings).first { (it.destination as? Destination.XatuService)?.page == page }.title
    val accountRepository: XatuAccountRepository = koinInject()
    val account by accountRepository.account.collectAsState(initial = null)
    var firstLogin by rememberSaveable { mutableStateOf(false) }
    val login = {
        if (account?.studentNumber.isNullOrBlank()) firstLogin = true
        else onNavigate(xatuLoginDestination())
    }
    XatuPage(title, onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = if (LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.COUI) 0.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (page in listOf("grades", "exams", "plan", "week")) {
                if (account == null) { UiFeedback("正在读取账号…", UiFeedbackKind.LOADING); return@Column }
                if (account?.loggedIn == false && account?.canRestore != true) {
                    UiCard(Modifier.fillMaxWidth()) {
                        UiServiceRow("登录教务账号", "", login)
                    }
                    if (page != "week") return@Column
                }
            }
            XatuServicePage(
                page = page,
                onOpen = { url, title -> onNavigate(Destination.WebView(initialUrl = url, serviceTitle = title)) },
                modifier = Modifier.weight(1f).padding(bottom = 16.dp),
                onLogin = login,
                onCourseSettings = { onNavigate(Destination.CourseStartDateSettings) },
            )
        }
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
