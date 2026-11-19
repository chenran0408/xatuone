package com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web

import com.xingheyuzhuan.shiguangschedule.ui.components.UiScaffold
import com.xingheyuzhuan.shiguangschedule.ui.components.UiTopAppBar
import com.xingheyuzhuan.shiguangschedule.ui.components.UiCourseIconButton
import com.xingheyuzhuan.shiguangschedule.ui.components.LocalUiStyle
import com.xingheyuzhuan.shiguangschedule.data.model.UiStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.layout.height

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Surface
import com.xingheyuzhuan.shiguangschedule.ui.components.UiButton
import com.xingheyuzhuan.shiguangschedule.ui.settings.xatu.XatuCourseSyncContext
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.Destination
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseConversionRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuSessionRepository
import com.xingheyuzhuan.shiguangschedule.ui.components.CourseTablePickerDialog
import com.xingheyuzhuan.shiguangschedule.ui.components.ToastManager
import com.xingheyuzhuan.shiguangschedule.ui.components.UiCard
import com.xingheyuzhuan.shiguangschedule.ui.components.UiText
import com.xingheyuzhuan.shiguangschedule.ui.components.UiActionButton
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.receiveAsFlow
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.a11y_back
import shiguangschedule.shared.generated.resources.a11y_cancel_editing
import shiguangschedule.shared.generated.resources.a11y_devtools
import shiguangschedule.shared.generated.resources.a11y_enter_url
import shiguangschedule.shared.generated.resources.a11y_load
import shiguangschedule.shared.generated.resources.a11y_more_options
import shiguangschedule.shared.generated.resources.a11y_refresh
import shiguangschedule.shared.generated.resources.action_execute_import
import shiguangschedule.shared.generated.resources.action_refresh
import shiguangschedule.shared.generated.resources.action_switch_to_desktop_mode
import shiguangschedule.shared.generated.resources.action_switch_to_phone_mode
import shiguangschedule.shared.generated.resources.arrow_back_24px
import shiguangschedule.shared.generated.resources.arrow_forward_24px
import shiguangschedule.shared.generated.resources.build_24px
import shiguangschedule.shared.generated.resources.desktop_windows_24px
import shiguangschedule.shared.generated.resources.dialog_title_select_table_for_import
import shiguangschedule.shared.generated.resources.item_devtools_debug
import shiguangschedule.shared.generated.resources.link_24px
import shiguangschedule.shared.generated.resources.more_vert_24px
import shiguangschedule.shared.generated.resources.phone_android_24px
import shiguangschedule.shared.generated.resources.placeholder_enter_url_full
import shiguangschedule.shared.generated.resources.refresh_24px
import shiguangschedule.shared.generated.resources.status_disabled
import shiguangschedule.shared.generated.resources.status_enabled
import shiguangschedule.shared.generated.resources.title_enter_url
import shiguangschedule.shared.generated.resources.title_loading
import shiguangschedule.shared.generated.resources.toast_devtools_enabled_format
import shiguangschedule.shared.generated.resources.toast_executing_import_script
import shiguangschedule.shared.generated.resources.toast_import_script_not_found
import shiguangschedule.shared.generated.resources.toast_load_import_script_failed
import shiguangschedule.shared.generated.resources.toast_no_script_manual_import
import shiguangschedule.shared.generated.resources.toast_switched_to_desktop
import shiguangschedule.shared.generated.resources.toast_switched_to_phone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewScreen(
    onNavigate: (Destination) -> Unit,
    onBack: () -> Unit,
    initialUrl: String?,
    assetJsPath: String?,
    xatuLogin: Boolean = false,
    serviceTitle: String? = null,
    onLoginComplete: () -> Unit = onBack,
    viewModel: WebViewModel = koinViewModel()
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    val isDeveloperModeEnabled by viewModel.isDeveloperModeEnabled.collectAsState()
    val embeddedTitle = if (xatuLogin) "教务登录" else serviceTitle
    val startedEmpty = remember { initialUrl.isNullOrBlank() || initialUrl == "about:blank" }
    val showAddressBarToggleButton = !xatuLogin && (startedEmpty || isDeveloperModeEnabled)

    val titleEnterUrl = stringResource(Res.string.title_enter_url)
    val titleLoading = stringResource(Res.string.title_loading)
    val toastSwitchedToDesktop = stringResource(Res.string.toast_switched_to_desktop)
    val toastSwitchedToPhone = stringResource(Res.string.toast_switched_to_phone)
    val toastExecutingImport = stringResource(Res.string.toast_executing_import_script)
    val toastImportNotFoundFmt = stringResource(Res.string.toast_import_script_not_found, "%s")
    val toastLoadImportFailedFmt = stringResource(Res.string.toast_load_import_script_failed, "%s")
    val statusEnabled = stringResource(Res.string.status_enabled)
    val statusDisabled = stringResource(Res.string.status_disabled)
    val toastDevToolsEnabled = stringResource(Res.string.toast_devtools_enabled_format, statusEnabled)
    val toastDevToolsDisabled = stringResource(Res.string.toast_devtools_enabled_format, statusDisabled)

    var currentUrl by remember { mutableStateOf(initialUrl ?: "about:blank") }
    var inputUrl by remember { mutableStateOf(if (startedEmpty) "" else (initialUrl ?: "")) }
    var loadingProgress by remember { mutableFloatStateOf(0f) }
    var pageTitle by remember { mutableStateOf(if (startedEmpty) titleEnterUrl else titleLoading) }

    var expanded by remember { mutableStateOf(false) }
    var isDesktopMode by remember { mutableStateOf(false) }
    var isEditingUrl by remember { mutableStateOf(startedEmpty) }
    var isDevToolsEnabled by remember { mutableStateOf(false) }
    var showCourseTablePicker by remember { mutableStateOf(false) }
    var loginReady by remember { mutableStateOf(false) }
    var entryHistoryReady by remember(embeddedTitle) { mutableStateOf(embeddedTitle == null) }
    var loginFailure by remember { mutableStateOf<String?>(null) }
    var sessionActionInProgress by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var showSyncContext by remember { mutableStateOf(true) }
    var targetTableName by remember { mutableStateOf<String?>(null) }
    val isXatuImport = assetJsPath?.startsWith("XATU/") == true

    val webViewController = rememberWebViewController()

    val coroutineScope = rememberCoroutineScope()
    val courseConversionRepository: CourseConversionRepository = koinInject()
    val xatuSessionRepository: XatuSessionRepository = koinInject()
    suspend fun completeLogin() {
        if (sessionActionInProgress) return
        sessionActionInProgress = true
        loginFailure = null
        try {
            xatuSessionRepository.completeLogin()
            onLoginComplete()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            loginFailure = "登录确认失败：${error.message ?: "请重试"}"
        } finally {
            sessionActionInProgress = false
        }
    }
    LaunchedEffect(loginReady) {
        if (loginReady) completeLogin()
    }
    val uiEventChannel = remember { Channel<WebUiEvent>(Channel.UNLIMITED) }
    val uiEventsFlow = remember(uiEventChannel) { uiEventChannel.receiveAsFlow() }

    val bridgeHandler = remember(coroutineScope, courseConversionRepository, webViewController) {
        WebBridgeHandler(
            coroutineScope = coroutineScope,
            uiEventChannel = uiEventChannel,
            courseConversionRepository = courseConversionRepository,
            onTaskCompleted = { if (!isXatuImport) onNavigate(Destination.CourseSchedule) },
            evaluateJs = { script, callback ->
                webViewController.evaluateJavascript(script, callback)
            }
        )
    }

    val importProgress by bridgeHandler.importProgress.collectAsState()

    val handleBackAction: () -> Unit = {
        if (isEditingUrl) {
            isEditingUrl = false
            val rawUrl = webViewController.currentUrl
            inputUrl = if (rawUrl.isBlank() || rawUrl == "about:blank") "" else rawUrl
            keyboardController?.hide()
        } else {
            if (!xatuLogin && entryHistoryReady && webViewController.canGoBack()) {
                webViewController.goBack()
            } else {
                onBack()
            }
        }
    }

    PlatformBackHandler(enabled = true, onBack = handleBackAction)

    val onSearch: (String) -> Unit = { query ->
        val trimmed = query.trim()
        if (trimmed.isNotBlank()) {
            val formattedUrl = if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                "https://$trimmed"
            } else {
                trimmed
            }
            keyboardController?.hide()
            currentUrl = formattedUrl
            isEditingUrl = false
            pageTitle = titleLoading
        }
    }

    UiScaffold(
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            if (embeddedTitle != null) UiTopAppBar(
                title = embeddedTitle,
                onBack = handleBackAction,
                actions = {
                    if (xatuLogin && loginFailure != null && loginReady) {
                        UiButton("重试确认", { coroutineScope.launch { completeLogin() } }, enabled = !sessionActionInProgress)
                    } else UiCourseIconButton(
                        icon = vectorResource(Res.drawable.refresh_24px),
                        description = stringResource(Res.string.a11y_refresh),
                        inToolbar = true,
                        onClick = {
                            loadError = null
                            webViewController.reload()
                        },
                        enabled = !sessionActionInProgress,
                    )
                },
            ) else TopAppBar(
                navigationIcon = {
                    IconButton(onClick = handleBackAction) {
                        Icon(
                            vectorResource(Res.drawable.arrow_back_24px),
                            contentDescription = stringResource(
                                if (isEditingUrl) Res.string.a11y_cancel_editing else Res.string.a11y_back
                            )
                        )
                    }
                },
                title = {
                    if (isEditingUrl) {
                        OutlinedTextField(
                            value = inputUrl,
                            onValueChange = { inputUrl = it },
                            placeholder = { Text(stringResource(Res.string.placeholder_enter_url_full)) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(
                                onGo = {
                                    onSearch(inputUrl)
                                }
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                errorBorderColor = Color.Transparent,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            ),
                            textStyle = MaterialTheme.typography.bodyLarge,
                        )
                    } else {
                        Text(
                            text = pageTitle,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isEditingUrl) {
                            IconButton(
                                onClick = { onSearch(inputUrl) },
                                enabled = inputUrl.trim().isNotBlank() && inputUrl.trim() != "https://"
                            ) {
                                Icon(vectorResource(Res.drawable.arrow_forward_24px), contentDescription = stringResource(Res.string.a11y_load))
                            }
                        } else if (showAddressBarToggleButton) {
                            IconButton(onClick = {
                                isEditingUrl = true
                                val rawUrl = webViewController.currentUrl
                                inputUrl = if (rawUrl.isBlank() || rawUrl == "about:blank") "" else rawUrl
                                keyboardController?.show()
                            }) {
                                Icon(vectorResource(Res.drawable.link_24px), contentDescription = stringResource(Res.string.a11y_enter_url))
                            }
                        }

                        IconButton(onClick = { expanded = true }) {
                            Icon(vectorResource(Res.drawable.more_vert_24px), contentDescription = stringResource(Res.string.a11y_more_options))
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.action_refresh)) },
                                onClick = {
                                    webViewController.reload()
                                    expanded = false
                                },
                                leadingIcon = { Icon(vectorResource(Res.drawable.refresh_24px), contentDescription = stringResource(Res.string.a11y_refresh)) }
                            )

                            DropdownMenuItem(
                                text = { Text("退出登录") },
                                enabled = !sessionActionInProgress,
                                onClick = {
                                    expanded = false
                                    sessionActionInProgress = true
                                    coroutineScope.launch {
                                        try {
                                            xatuSessionRepository.clear()
                                            loginReady = false
                                            ToastManager.show("已退出登录")
                                            onBack()
                                        } catch (error: CancellationException) {
                                            throw error
                                        } catch (error: Exception) {
                                            ToastManager.show("退出登录失败：${error.message ?: "请重试"}")
                                        } finally {
                                            sessionActionInProgress = false
                                        }
                                    }
                                }
                            )

                            if (!xatuLogin) {
                                val switchTextId = if (isDesktopMode) Res.string.action_switch_to_phone_mode else Res.string.action_switch_to_desktop_mode
                                val switchIcon = if (isDesktopMode) vectorResource(Res.drawable.phone_android_24px) else vectorResource(Res.drawable.desktop_windows_24px)

                                DropdownMenuItem(
                                    text = { Text(stringResource(switchTextId)) },
                                    onClick = {
                                        val realUrl = webViewController.currentUrl
                                        if (realUrl.isNotBlank() && realUrl != "about:blank") {
                                            currentUrl = realUrl
                                        }
                                        isDesktopMode = !isDesktopMode
                                        val tText = if (isDesktopMode) toastSwitchedToDesktop else toastSwitchedToPhone
                                        ToastManager.show(tText)
                                        expanded = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            switchIcon,
                                            contentDescription = stringResource(switchTextId)
                                        )
                                    }
                                )
                            }

                            if (isDeveloperModeEnabled) {
                                DropdownMenuItem(
                                    onClick = {
                                        isDevToolsEnabled = !isDevToolsEnabled
                                        webViewController.setDevToolsEnabled(isDevToolsEnabled)

                                        val tText = if (isDevToolsEnabled) toastDevToolsEnabled else toastDevToolsDisabled
                                        ToastManager.show(tText)
                                    },
                                    leadingIcon = { Icon(vectorResource(Res.drawable.build_24px), contentDescription = stringResource(Res.string.a11y_devtools)) },
                                    text = { Text(stringResource(Res.string.item_devtools_debug)) },
                                    trailingIcon = { Switch(checked = isDevToolsEnabled, onCheckedChange = null) }
                                )
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (assetJsPath != null) Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Column(Modifier.fillMaxWidth().imePadding().heightIn(max = 220.dp)
                    .verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (isXatuImport && showSyncContext) XatuCourseSyncContext { onNavigate(Destination.XatuLogin) }
                    targetTableName?.let { UiText("目标课表：$it", style = MaterialTheme.typography.bodySmall) }
                    importProgress.selection?.let { UiText("已确认：$it", style = MaterialTheme.typography.bodySmall) }
                    if (importProgress.error != null || importProgress.message.isNotBlank()) UiText(importProgress.error ?: importProgress.message, style = MaterialTheme.typography.bodySmall,
                        color = if (importProgress.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                    if (importProgress.running) LinearProgressIndicator(Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (isXatuImport) UiActionButton({ showSyncContext = !showSyncContext }, Modifier.weight(1f), enabled = !importProgress.running, text = if (showSyncContext) "收起账号／学期" else "账号／学期")
                        UiActionButton({
                            if (importProgress.completed) onNavigate(Destination.CourseSchedule) else showCourseTablePicker = true
                        }, Modifier.weight(1f), enabled = !importProgress.running, text = if (importProgress.completed) "查看课表" else if (importProgress.running) "正在同步…" else "同步课程表")
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            PlatformWebView(
                modifier = Modifier.fillMaxSize(),
                url = currentUrl,
                isDesktopMode = isDesktopMode,
                isDevToolsEnabled = isDevToolsEnabled,
                controller = webViewController,
                bridgeHandler = bridgeHandler,
                onProgressChange = { loadingProgress = it },
                onTitleChange = { pageTitle = it },
                onLoadError = { loadError = it },
                onPageFinished = { url ->
                    entryHistoryReady = true
                    if (xatuLogin && loadError == null) loginReady =
                        url.startsWith("http://jwgl2018.xatu.edu.cn/eams/") ||
                            url.startsWith("https://jwgl2018.xatu.edu.cn/eams/")
                },
                resetHistoryOnEntry = embeddedTitle != null,
                onNavigateToSchedule = { onNavigate(Destination.CourseSchedule) },
                onSslError = { failingUrl, onProceed, onCancel ->
                    uiEventChannel.trySend(
                        WebUiEvent.ShowSslWarning(
                            SslErrorData(
                                primaryErrorUrl = failingUrl,
                                onProceed = onProceed,
                                onCancel = onCancel
                            )
                        )
                    )
                }
            )

            if (loadingProgress < 1.0f) {
                if (LocalUiStyle.current == UiStyle.COUI) {
                    Box(Modifier.fillMaxWidth().align(Alignment.TopCenter).progressSemantics(loadingProgress)) {
                        Box(Modifier.fillMaxWidth(loadingProgress).height(2.dp)
                            .background(com.itos.coui.theme.CouiTheme.colorScheme.accent))
                    }
                } else {
                    LinearProgressIndicator(
                        progress = { loadingProgress },
                        modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.Transparent
                    )
                }
            }

            (loadError ?: loginFailure ?: "正在确认教务登录…".takeIf { xatuLogin && sessionActionInProgress })?.let { message ->
                UiCard(Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(12.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        UiText(message)
                        if (loadError != null) {
                            UiActionButton(onClick = { loadError = null; webViewController.reload() }, text = "重新加载")
                        }
                    }
                }
            }

            if (showCourseTablePicker && assetJsPath != null) {
                CourseTablePickerDialog(
                    title = stringResource(Res.string.dialog_title_select_table_for_import),
                    onDismissRequest = { showCourseTablePicker = false },
                    onTableSelected = { selectedTable ->
                        showCourseTablePicker = false
                        val tableId = selectedTable.id
                        targetTableName = selectedTable.name
                        showSyncContext = false

                        try {
                            val fileSystem = viewModel.fileSystem
                            val jsFilePath = viewModel.filesDir / "repo" / "schools" / "resources" / assetJsPath

                            if (fileSystem.exists(jsFilePath)) {
                                val jsCode = fileSystem.read(jsFilePath) { readUtf8() }
                                bridgeHandler.setImportTableId(tableId, trackCourseImport = isXatuImport)

                                val fullJsScript = if (isXatuImport) trackedCourseImportScript(tableId, jsCode) else "window.currentTableId = '$tableId';\n$jsCode"
                                webViewController.executeScript(fullJsScript)

                                ToastManager.show(toastExecutingImport)
                            } else {
                                bridgeHandler.importFailed(toastImportNotFoundFmt.replace("%s", jsFilePath.toString()))
                            }
                        } catch (e: Exception) {
                            bridgeHandler.importFailed(toastLoadImportFailedFmt.replace("%s", e.message ?: ""))
                        }
                    }
                )
            }
            WebDialogHost(uiEvents = uiEventsFlow)
        }
    }
}
