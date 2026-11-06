package com.xingheyuzhuan.shiguangschedule.ui.settings.xatu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button as MiuixButton
import androidx.compose.material3.Card as MaterialCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuCampusRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuCampusSnapshot
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuCalendarSemester
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuDormSelection
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuCheckinLocation
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuAccount
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuAccountRepository
import com.xingheyuzhuan.shiguangschedule.xatu.academic.XatuExam
import com.xingheyuzhuan.shiguangschedule.xatu.academic.XatuGrade
import com.xingheyuzhuan.shiguangschedule.xatu.academic.XatuGradePage
import com.xingheyuzhuan.shiguangschedule.xatu.academic.XatuCourseInfo
import com.xingheyuzhuan.shiguangschedule.xatu.academic.XatuPlanSection
import com.xingheyuzhuan.shiguangschedule.xatu.academic.XatuStudyPlan
import com.xingheyuzhuan.shiguangschedule.xatu.academic.XatuSemester
import com.xingheyuzhuan.shiguangschedule.xatu.campus.XatuCalendarService
import com.xingheyuzhuan.shiguangschedule.xatu.campus.XatuCampusOption
import com.xingheyuzhuan.shiguangschedule.xatu.campus.XatuCampusService
import com.xingheyuzhuan.shiguangschedule.xatu.campus.XatuDorm
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
actual fun XatuServicePage(
    page: String,
    onOpen: (String) -> Unit,
    modifier: Modifier
) {
    val academicViewModel: XatuAcademicViewModel = koinViewModel()
    val academicState by academicViewModel.uiState.collectAsState()
    val accountRepository: XatuAccountRepository = koinInject()
    val account by accountRepository.account.collectAsState(initial = XatuAccount("", false))
    val campus: XatuCampusRepository = koinInject()
    val snapshot by campus.snapshot.collectAsState(initial = XatuCampusSnapshot())
    val campusService: XatuCampusService = koinInject()
    val calendarService: XatuCalendarService = koinInject()
    var status by remember(page) { mutableStateOf("正在读取教务数据…") }
    var calendarRefreshing by remember(page) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(page, account.studentNumber, account.loggedIn) {
        if (page == "plan" || page == "grades" || page == "exams") academicViewModel.load(page)
    }

    LaunchedEffect(page, account.studentNumber) {
        when {
            page == "calendar" && snapshot.calendar == null -> {
                try {
                    campus.saveCalendar(calendarService.latest())
                    status = "校历已更新"
                } catch (error: Exception) {
                    status = error.message ?: "校历读取失败"
                }
            }
            page == "calendar" -> status = ""
            else -> status = ""
        }
    }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
            when (page) {
                "plan" -> PlanContent(academicState.plan, academicState.catalog, academicState.status)
                "grades" -> {
                    SemesterPicker(academicState.semesters, academicState.selectedSemesterId, academicViewModel::selectSemester)
                    GradesContent(academicState.grades, academicState.status)
                }
                "exams" -> {
                    SemesterPicker(academicState.semesters, academicState.selectedSemesterId, academicViewModel::selectSemester)
                    ExamsContent(academicState.exams, academicState.status)
                }
                "calendar" -> LinkContent(
                    snapshot.calendar?.let { "${it.title}（${it.semesters.size} 个学期）" } ?: "校历使用教务处公开页面，查看学期和假期安排。",
                    "打开教务处校历",
                    "https://jwcw.xatu.edu.cn/index/xagydxxl.htm",
                    onOpen,
                    imageUrl = snapshot.calendar?.imageUrl.orEmpty(),
                    rawText = snapshot.calendar?.rawText.orEmpty(),
                    snapshot.calendar?.semesters.orEmpty(),
                    refreshing = calendarRefreshing,
                    statusText = status,
                    onRefresh = {
                        scope.launch {
                            calendarRefreshing = true
                            status = "正在刷新校历…"
                            try {
                                campus.saveCalendar(calendarService.latest())
                                status = "校历已更新"
                            } catch (error: Exception) {
                                status = error.message ?: "校历读取失败"
                            } finally {
                                calendarRefreshing = false
                            }
                        }
                    }
                )
                "electricity" -> ElectricityContent(snapshot, campus, campusService, onOpen)
                "checkinLocation" -> CheckinLocationContent(snapshot, campus, onOpen)
                else -> LinkContent(
                    "校园服务统一入口。",
                    "打开一站式服务",
                    "https://ehall.xatu.edu.cn/",
                    onOpen
                )
            }
    }
}

@Composable
private fun SmallTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
}

@Composable
private fun MiuixCard(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    MaterialCard(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        content = { Column(Modifier.padding(16.dp), content = content) }
    )
}

@Composable
private fun MiuixText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    fontSize: androidx.compose.ui.unit.TextUnit = 14.sp,
    style: TextStyle = MaterialTheme.typography.bodyMedium
) {
    Text(text = text, modifier = modifier, color = color, fontSize = fontSize, style = style)
}

@Composable
private fun StatusFeedback(status: String) {
    if (status.isBlank()) return
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (status.contains("正在")) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        Text(
            status,
            style = MaterialTheme.typography.bodyMedium,
            color = if (status.contains("失败") || status.contains("请输入有效")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CheckinLocationContent(snapshot: XatuCampusSnapshot, campus: XatuCampusRepository, onOpen: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    val saved = snapshot.checkinLocation
    var latitude by remember(saved) { mutableStateOf(if (saved.enabled) saved.latitude.toString() else "") }
    var longitude by remember(saved) { mutableStateOf(if (saved.enabled) saved.longitude.toString() else "") }
    var enabled by remember(saved) { mutableStateOf(saved.enabled) }
    var status by remember { mutableStateOf("") }
    MiuixCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SmallTitle("签到位置")
            MiuixText("保存坐标后，仅在 XATU 签到网页中使用该位置。")
            OutlinedTextField(latitude, { latitude = it; status = "" }, Modifier.fillMaxWidth(), label = { MiuixText("纬度") }, singleLine = true)
            OutlinedTextField(longitude, { longitude = it; status = "" }, Modifier.fillMaxWidth(), label = { MiuixText("经度") }, singleLine = true)
            StatusFeedback(status)
            MiuixButton(onClick = {
                val lat = latitude.toDoubleOrNull()
                val lng = longitude.toDoubleOrNull()
                if (lat == null || lng == null) {
                    status = "请输入有效的纬度和经度"
                } else {
                    scope.launch {
                        campus.saveCheckinLocation(XatuCheckinLocation(enabled, lat, lng, "GCJ02"))
                        status = "签到位置已保存"
                    }
                }
            }, modifier = Modifier.fillMaxWidth()) { MiuixText("保存签到位置") }
            MiuixButton(onClick = { enabled = !enabled; scope.launch { campus.saveCheckinLocation(saved.copy(enabled = enabled)) } }, modifier = Modifier.fillMaxWidth()) {
                MiuixText(if (enabled) "已启用自定义位置" else "已停用自定义位置")
            }
            MiuixButton(onClick = { onOpen("https://ehall.xatu.edu.cn/xsfw/sys/swmqdzsapp/*default/index.do#/jrqd") }, modifier = Modifier.fillMaxWidth()) { MiuixText("打开签到网页") }
        }
    }
}

@Composable
private fun SemesterPicker(semesters: List<XatuSemester>, selected: String?, onSelect: (String) -> Unit) {
    if (semesters.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SmallTitle("选择学期")
        Box {
            MiuixButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    MiuixText(semesters.firstOrNull { it.id == selected }?.name ?: "选择学期", modifier = Modifier.weight(1f))
                    MiuixText("⌄", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                semesters.forEach { semester ->
                    DropdownMenuItem(
                        text = { MiuixText(semester.name) },
                        onClick = {
                            expanded = false
                            if (semester.id != selected) onSelect(semester.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ElectricityContent(
    snapshot: XatuCampusSnapshot,
    repository: XatuCampusRepository,
    service: XatuCampusService,
    onOpen: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val saved = snapshot.dorm
    var projects by remember { mutableStateOf<List<XatuCampusOption>>(emptyList()) }
    var areas by remember { mutableStateOf<List<XatuCampusOption>>(emptyList()) }
    var buildings by remember { mutableStateOf<List<XatuCampusOption>>(emptyList()) }
    var rooms by remember { mutableStateOf<List<XatuCampusOption>>(emptyList()) }
    var project by remember { mutableStateOf<XatuCampusOption?>(null) }
    var area by remember { mutableStateOf<XatuCampusOption?>(null) }
    var building by remember { mutableStateOf<XatuCampusOption?>(null) }
    var room by remember { mutableStateOf<XatuCampusOption?>(null) }
    var status by remember { mutableStateOf("正在读取项目…") }
    var refreshing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            projects = service.projects()
            project = projects.firstOrNull { it.id == saved?.projectId }
            status = if (projects.isEmpty()) "暂无可用电费项目" else ""
        } catch (error: Exception) {
            status = error.message ?: "项目读取失败"
        }
    }
    LaunchedEffect(project?.id) {
        val selected = project ?: return@LaunchedEffect
        areas = emptyList(); buildings = emptyList(); rooms = emptyList()
        area = null; building = null; room = null
        try {
            areas = service.areas(selected.id)
            area = areas.firstOrNull { it.id == saved?.areaId }
        } catch (error: Exception) { status = error.message ?: "校区读取失败" }
    }
    LaunchedEffect(project?.id, area?.id) {
        val selectedProject = project ?: return@LaunchedEffect
        val selectedArea = area ?: return@LaunchedEffect
        buildings = emptyList(); rooms = emptyList(); building = null; room = null
        try {
            buildings = service.buildings(selectedProject.id, selectedArea.id)
            building = buildings.firstOrNull { it.id == saved?.buildingId }
        } catch (error: Exception) { status = error.message ?: "楼栋读取失败" }
    }
    LaunchedEffect(project?.id, area?.id, building?.id) {
        val selectedProject = project ?: return@LaunchedEffect
        val selectedArea = area ?: return@LaunchedEffect
        val selectedBuilding = building ?: return@LaunchedEffect
        rooms = emptyList(); room = null
        try {
            rooms = service.rooms(selectedProject.id, selectedArea.id, selectedBuilding.id)
            room = rooms.firstOrNull { it.id == saved?.roomId }
        } catch (error: Exception) { status = error.message ?: "房间读取失败" }
    }

    val dorm = if (project != null && area != null && building != null && room != null) {
        XatuDorm(project!!, area!!, building!!, room!!)
    } else null
    val savedMatches = dorm != null && saved != null &&
        dorm.project.id == saved.projectId && dorm.area.id == saved.areaId &&
        dorm.building.id == saved.buildingId && dorm.room.id == saved.roomId

    LaunchedEffect(dorm?.key) {
        val selected = dorm ?: return@LaunchedEffect
        if (!savedMatches) repository.saveDorm(XatuDormSelection(
            selected.project.id, selected.project.name, selected.area.id, selected.area.name,
            selected.building.id, selected.building.name, selected.room.id, selected.room.name
        ))
    }

    MiuixCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SmallTitle("电费余额")
            OptionPicker("项目", projects, project) {
                project = it; area = null; building = null; room = null
            }
            OptionPicker("校区", areas, area) {
                area = it; building = null; room = null
            }
            OptionPicker("楼栋", buildings, building) {
                building = it; room = null
            }
            OptionPicker("房间", rooms, room) { room = it }
            StatusFeedback(status)
            if (savedMatches && snapshot.balance != null) MiuixText("余额：${snapshot.balance} 元")
            if (savedMatches && snapshot.balanceHistory.isNotEmpty()) {
                SmallTitle("余额记录")
                snapshot.balanceHistory.asReversed().take(7).forEach { record ->
                    val time = java.time.Instant.ofEpochMilli(record.at)
                        .atZone(java.time.ZoneId.of("Asia/Shanghai"))
                        .toLocalDate()
                    MiuixText("$time：${record.balance} 元")
                }
                BalanceTrend(snapshot.balanceHistory)
            }
            MiuixButton(
                onClick = {
                    val selected = dorm ?: return@MiuixButton
                    scope.launch {
                        refreshing = true
                        status = "正在刷新余额…"
                        try {
                            repository.saveBalance(service.electricityBalance(selected), System.currentTimeMillis())
                            status = "余额已更新"
                        } catch (error: Exception) {
                            status = error.message ?: "余额读取失败"
                        } finally { refreshing = false }
                    }
                },
                enabled = dorm != null && !refreshing,
                modifier = Modifier.fillMaxWidth()
            ) { MiuixText(if (refreshing) "正在刷新…" else "刷新余额") }
            MiuixButton(onClick = { onOpen("https://jfpay.xatu.edu.cn/casLogin/") }, modifier = Modifier.fillMaxWidth()) {
                MiuixText("打开电费平台")
            }
        }
    }
}

@Composable
private fun BalanceTrend(history: List<com.xingheyuzhuan.shiguangschedule.data.repository.XatuBalanceRecord>) {
    val points = history.takeLast(7).mapNotNull { it.balance.toFloatOrNull()?.let { value -> it.at to value } }
    if (points.size < 2) {
        MiuixText("至少记录两次余额后显示趋势")
        return
    }
    val changes = points.zipWithNext().mapNotNull { (previous, current) ->
        val days = (current.first - previous.first) / 86_400_000L
        (previous.second - current.second).takeIf { days >= 1 && it >= 0f }
    }
    val average = changes.takeIf { it.isNotEmpty() }?.average()
    MiuixText("近七次余额趋势${average?.let { "，日均用电约 ${"%.2f".format(it)} 度" } ?: ""}")
    val min = points.minOf { it.second }
    val max = points.maxOf { it.second }
    val range = (max - min).coerceAtLeast(1f)
    val lineColor = MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxWidth().height(120.dp).padding(horizontal = 8.dp)) {
        val step = size.width / (points.size - 1)
        val offsets = points.mapIndexed { index, point ->
            Offset(index * step, size.height - (point.second - min) / range * size.height)
        }
        offsets.zipWithNext().forEach { (start, end) -> drawLine(lineColor, start, end, strokeWidth = 5f) }
        offsets.forEach { drawCircle(lineColor, radius = 6f, center = it) }
    }
}

@Composable
private fun OptionPicker(
    label: String,
    options: List<XatuCampusOption>,
    selected: XatuCampusOption?,
    onSelect: (XatuCampusOption) -> Unit
) {
    var open by remember(label) { mutableStateOf(false) }
    MiuixButton(onClick = { open = true }, enabled = options.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
        MiuixText("$label：${selected?.name ?: if (options.isEmpty()) "暂无选项" else "请选择"}")
    }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { MiuixText("选择$label") },
            text = {
                LazyColumn(modifier = Modifier.height(320.dp)) {
                    items(options, key = { it.id }) { option ->
                        TextButton(onClick = { open = false; onSelect(option) }, modifier = Modifier.fillMaxWidth()) {
                            MiuixText(option.name)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { open = false }) { MiuixText("取消") } }
        )
    }
}

@Composable
private fun GradesContent(page: XatuGradePage?, status: String) {
    MiuixCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SmallTitle("成绩")
            if (page == null) {
                StatusFeedback(status)
                return@Column
            }
            StatusFeedback(status)
            page.summary.count?.let { MiuixText("课程数：$it") }
            page.summary.credits?.let { MiuixText("总学分：$it") }
            page.summary.gpa?.let { MiuixText("平均绩点：$it") }
            if (page.grades.isEmpty()) MiuixText("暂无成绩")
            page.grades.forEach { GradeRow(it) }
        }
    }
}

@Composable
private fun GradeRow(grade: XatuGrade) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        MiuixText(grade.course)
        MiuixText(listOf(grade.term, grade.credit.takeIf { it.isNotBlank() }?.let { "学分 $it" }, grade.score.takeIf { it.isNotBlank() }?.let { "成绩 $it" }, grade.gpa.takeIf { it.isNotBlank() }?.let { "绩点 $it" }).filterNotNull().filter { it.isNotBlank() }.joinToString(" · "))
    }
}

@Composable
private fun ExamsContent(exams: List<XatuExam>?, status: String) {
    MiuixCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SmallTitle("考试安排")
            if (exams == null) {
                StatusFeedback(status)
                return@Column
            }
            StatusFeedback(status)
            if (exams.isEmpty()) MiuixText("暂无考试安排")
            exams.forEach { exam ->
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    MiuixText(exam.course)
                    MiuixText(listOf(exam.time, exam.room, exam.seat.takeIf { it.isNotBlank() }?.let { "座位 $it" }).filterNotNull().filter { it.isNotBlank() }.joinToString(" · "))
                }
            }
        }
    }
}

@Composable
private fun PlanContent(plan: XatuStudyPlan?, catalog: List<XatuCourseInfo>, status: String) {
    MiuixCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SmallTitle("学业完成情况")
            if (plan == null) {
                StatusFeedback(status)
                return@Column
            }
            MiuixText("已修 ${plan.earnedCredits} / 要求 ${plan.requiredCredits} 学分")
            if (plan.major.isNotBlank()) MiuixText(plan.major)
            if (plan.gpa.isNotBlank()) MiuixText("GPA：${plan.gpa}")
            StatusFeedback(status)
        }
    }
    if (plan != null) {
        plan.sections.forEach { section -> PlanSectionContent(section, catalog, 0) }
    }
}

@Composable
private fun PlanSectionContent(section: XatuPlanSection, catalog: List<XatuCourseInfo>, level: Int) {
    MiuixCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            MiuixText(section.title)
            MiuixText("已修 ${section.earnedCredits} / 要求 ${section.requiredCredits} 学分")
            section.courses.forEach { course ->
                val info = catalog.firstOrNull { it.code == course.code || it.name == course.name }
                MiuixText(
                    listOf(course.name, course.code, "${course.earnedCredits}/${course.requiredCredits} 学分", course.score.takeIf { it.isNotBlank() }?.let { "成绩 $it" }, info?.term?.takeIf { it.isNotBlank() }?.let { "第 $it 学期" })
                        .filterNotNull().filter { it.isNotBlank() }.joinToString(" · ")
                )
            }
        }
    }
    section.children.forEach { child -> PlanSectionContent(child, catalog, level + 1) }
}

@Composable
private fun LinkContent(
    description: String,
    action: String,
    url: String,
    onOpen: (String) -> Unit,
    imageUrl: String = "",
    rawText: String = "",
    semesters: List<XatuCalendarSemester> = emptyList(),
    refreshing: Boolean = false,
    statusText: String = "",
    onRefresh: (() -> Unit)? = null
) {
    MiuixCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MiuixText(description)
            StatusFeedback(statusText)
            semesters.forEach { semester ->
                MiuixText("${semester.label}：${semester.start} 至 ${semester.end}")
            }
            if (onRefresh != null) {
                MiuixButton(
                    onClick = onRefresh,
                    enabled = !refreshing,
                    modifier = Modifier.fillMaxWidth()
                ) { MiuixText(if (refreshing) "正在刷新…" else "刷新校历") }
            }
            if (imageUrl.isNotBlank()) {
                MiuixButton(onClick = { onOpen(imageUrl) }, modifier = Modifier.fillMaxWidth()) {
                    MiuixText("查看校历原图")
                }
            }
            if (rawText.isNotBlank()) {
                MiuixText("校历识别文字")
                MiuixText(rawText)
                val context = LocalContext.current
                MiuixButton(
                    onClick = {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("校历识别文字", rawText))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { MiuixText("复制识别文字") }
            }
            MiuixButton(onClick = { onOpen(url) }, modifier = Modifier.fillMaxWidth()) { MiuixText(action) }
        }
    }
}
