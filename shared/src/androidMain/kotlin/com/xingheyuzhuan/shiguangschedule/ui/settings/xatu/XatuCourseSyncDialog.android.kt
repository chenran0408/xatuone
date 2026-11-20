package com.xingheyuzhuan.shiguangschedule.ui.settings.xatu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.Destination
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTable
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseConversionRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseTableRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.SchoolRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuAccountRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuSessionRepository
import com.xingheyuzhuan.shiguangschedule.ui.components.CourseTablePickerDialog
import com.xingheyuzhuan.shiguangschedule.ui.components.UiDialog
import com.xingheyuzhuan.shiguangschedule.ui.components.UiDialogAction
import com.xingheyuzhuan.shiguangschedule.ui.components.UiFeedback
import com.xingheyuzhuan.shiguangschedule.ui.components.UiFeedbackKind
import com.xingheyuzhuan.shiguangschedule.ui.components.UiSelectionCard
import com.xingheyuzhuan.shiguangschedule.ui.components.UiText
import com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web.WebBridgeHandler
import com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web.ConfirmSemesterDatesDialog
import com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web.WebUiEvent
import com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web.XatuCourseScriptRunner
import com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web.trackedCourseImportScript
import com.xingheyuzhuan.shiguangschedule.xatu.academic.XatuAcademicRepository
import com.xingheyuzhuan.shiguangschedule.xatu.academic.XatuSessionExpired
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import org.koin.compose.koinInject
import org.koin.core.qualifier.named
import school_index.AdapterCategory

@Composable
internal actual fun XatuCourseSyncDialog(initialTableId: String?, onDismiss: () -> Unit, onLogin: (Destination) -> Unit) {
    val repository: CourseTableRepository = koinInject()
    val tables by remember(repository) { repository.getAllCourseTables() }.collectAsState(initial = null)
    var chosenTable by remember { mutableStateOf<CourseTable?>(null) }
    val target = chosenTable ?: tables?.firstOrNull { it.id == initialTableId } ?: tables?.singleOrNull()

    when {
        target != null -> CourseSync(target, onDismiss, onLogin)
        tables == null -> UiDialog(onDismiss, "同步课表",
            text = { UiFeedback("正在读取课表…", UiFeedbackKind.LOADING) },
            dismissButton = UiDialogAction("取消", onDismiss))
        else -> CourseTablePickerDialog(
            title = "选择目标课表",
            onDismissRequest = { if (chosenTable == null) onDismiss() },
            onTableSelected = { chosenTable = it },
        )
    }
}

@Composable
private fun CourseSync(target: CourseTable, onDismiss: () -> Unit, onLogin: (Destination) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accounts: XatuAccountRepository = koinInject()
    val session: XatuSessionRepository = koinInject()
    val academic: XatuAcademicRepository = koinInject()
    val schools: SchoolRepository = koinInject()
    val conversion: CourseConversionRepository = koinInject()
    val filesDir: Path = koinInject(qualifier = named("FilesDir"))
    val fileSystem: FileSystem = koinInject()

    val events = remember { Channel<WebUiEvent>(Channel.UNLIMITED) }
    var runner by remember { mutableStateOf<XatuCourseScriptRunner?>(null) }
    var accountNumber by remember { mutableStateOf("") }
    var selection by remember { mutableStateOf<WebUiEvent.ShowSingleSelection?>(null) }
    var semesterDates by remember { mutableStateOf<WebUiEvent.ConfirmSemesterDates?>(null) }
    var submitted by remember { mutableStateOf(false) }
    var loginRequired by remember { mutableStateOf(false) }
    var firstLogin by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val bridge = remember(conversion) {
        WebBridgeHandler(scope, events, conversion, onTaskCompleted = {}, evaluateJs = { script, callback ->
            runner?.evaluate(script, callback)
        })
    }
    val progress by bridge.importProgress.collectAsState()

    LaunchedEffect(events) {
        for (event in events) {
            if (event is WebUiEvent.ShowSingleSelection && event.data.items.isNotEmpty()) {
                selection = event
            } else if (event is WebUiEvent.ConfirmSemesterDates) {
                semesterDates = event
            } else {
                bridge.importFailed("课表适配器未返回可选学期")
            }
        }
    }

    LaunchedEffect(target.id) {
        try {
            val account = accounts.account.first()
            accountNumber = account.studentNumber
            if (account.loggedIn) academic.officialWeek()
            else if (account.canRestore) session.restoreSavedLogin(account.studentNumber)
            else {
                loginRequired = true
                errorMessage = "请先登录教务账号"
                return@LaunchedEffect
            }

            val adapter = schools.getAdaptersForSchool("XATU").singleOrNull {
                it.category == AdapterCategory.BACHELOR_AND_ASSOCIATE
            } ?: error("找不到西安工业大学课表适配器")
            val scriptName = adapter.asset_js_path.ifBlank { "${adapter.adapter_id}.js" }
            val source = withContext(Dispatchers.IO) {
                fileSystem.read(filesDir / "repo" / "schools" / "resources" / "XATU" / scriptName) { readUtf8() }
            }
            val engine = XatuCourseScriptRunner(context, bridge::onMessageReceived, bridge::importFailed)
            runner = engine
            try {
                bridge.prepareCourseImport()
                engine.start(trackedCourseImportScript(target.id, source))
                bridge.importProgress.first { !it.running }
            } finally {
                engine.close()
                runner = null
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: XatuSessionExpired) {
            errorMessage = error.message
            loginRequired = true
            accounts.markLoggedOut(accountNumber)
        } catch (error: Exception) {
            errorMessage = error.message ?: "同步课表失败"
        }
    }

    val pending = selection?.takeIf { errorMessage == null && progress.error == null }
    val saving = submitted && progress.running && errorMessage == null
    val dismiss = { if (!submitted || !bridge.importProgress.value.running) onDismiss() }
    val dates = semesterDates
    if (dates != null) {
        ConfirmSemesterDatesDialog(dates) { result ->
            semesterDates = null
            dates.onResult(result)
            if (result == null) onDismiss()
        }
    } else if (pending != null) {
        var selectedIndex by remember(pending) { mutableIntStateOf(pending.data.defaultSelectedIndex) }
        UiDialog(
            onDismissRequest = dismiss,
            title = pending.data.title,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    UiText("教务账号：$accountNumber")
                    UiText("将替换「${target.name}」中的课程")
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
                        state = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex.coerceAtLeast(0)),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        itemsIndexed(pending.data.items) { index, name ->
                            UiSelectionCard(selectedIndex == index, { selectedIndex = index }, Modifier.fillMaxWidth()) {
                                UiText(name)
                            }
                        }
                    }
                }
            },
            confirmButton = UiDialogAction("同步", {
                if (!submitted) {
                    submitted = true
                    selection = null
                    bridge.setImportTableId(target.id, trackCourseImport = true)
                    pending.callback(selectedIndex)
                }
            }, enabled = selectedIndex in pending.data.items.indices),
            dismissButton = UiDialogAction("取消", dismiss),
        )
    } else if (firstLogin) {
        XatuFirstLoginDialog(
            onDismiss = { firstLogin = false },
            courseSyncTableId = target.id,
            onNavigate = onLogin,
        )
    } else {
        val failure = errorMessage ?: progress.error
        UiDialog(
            onDismissRequest = dismiss,
            title = "同步课表",
            text = {
                when {
                    failure != null -> UiFeedback(failure, UiFeedbackKind.ERROR)
                    progress.completed -> UiFeedback(progress.message, UiFeedbackKind.SUCCESS)
                    else -> UiFeedback(if (submitted) "正在同步…" else "正在读取学期…", UiFeedbackKind.LOADING)
                }
            },
            confirmButton = when {
                loginRequired -> UiDialogAction("登录教务", {
                    if (accountNumber.isBlank()) firstLogin = true
                    else onLogin(xatuLoginDestination(target.id))
                })
                progress.completed -> UiDialogAction("完成", dismiss)
                saving -> UiDialogAction("同步中", dismiss, enabled = false)
                else -> null
            },
            dismissButton = when {
                saving || progress.completed -> null
                else -> UiDialogAction(if (failure == null || loginRequired) "取消" else "关闭", dismiss)
            },
        )
    }
}
