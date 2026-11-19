package com.xingheyuzhuan.shiguangschedule.ui.settings.backup

import com.xingheyuzhuan.shiguangschedule.ui.components.UiScaffold
import com.xingheyuzhuan.shiguangschedule.ui.components.UiDialog
import com.xingheyuzhuan.shiguangschedule.ui.components.UiDialogAction
import com.xingheyuzhuan.shiguangschedule.ui.components.UiListDivider
import com.xingheyuzhuan.shiguangschedule.ui.components.UiSelectionCard
import com.xingheyuzhuan.shiguangschedule.ui.components.UiText
import com.xingheyuzhuan.shiguangschedule.ui.components.UiTopAppBar
import com.xingheyuzhuan.shiguangschedule.ui.components.UiInputField
import com.xingheyuzhuan.shiguangschedule.ui.components.UiLoadingIndicator
import com.xingheyuzhuan.shiguangschedule.ui.components.LocalUiStyle
import com.xingheyuzhuan.shiguangschedule.ui.components.MiuixCourseSnackbar
import com.xingheyuzhuan.shiguangschedule.ui.components.CouiCourseSnackbar
import com.xingheyuzhuan.shiguangschedule.data.model.UiStyle

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.tool.FileManagerCallbacks
import com.xingheyuzhuan.shiguangschedule.tool.rememberFileManager
import kotlinx.coroutines.launch
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.rememberCouiTextStyle
import com.itos.coui.theme.CouiTheme
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import okio.Buffer
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.a11y_back
import shiguangschedule.shared.generated.resources.action_cancel
import shiguangschedule.shared.generated.resources.action_confirm
import shiguangschedule.shared.generated.resources.action_disconnect_webdav
import shiguangschedule.shared.generated.resources.arrow_back_24px
import shiguangschedule.shared.generated.resources.backup_target_local_zip
import shiguangschedule.shared.generated.resources.backup_target_webdav
import shiguangschedule.shared.generated.resources.cloud_24px
import shiguangschedule.shared.generated.resources.desc_webdav_connected
import shiguangschedule.shared.generated.resources.desc_webdav_unconfigured
import shiguangschedule.shared.generated.resources.dialog_title_backup_target
import shiguangschedule.shared.generated.resources.dialog_title_config_webdav
import shiguangschedule.shared.generated.resources.dialog_title_disconnect_webdav
import shiguangschedule.shared.generated.resources.dialog_message_disconnect_webdav
import shiguangschedule.shared.generated.resources.dialog_title_restore_source
import shiguangschedule.shared.generated.resources.download_24px
import shiguangschedule.shared.generated.resources.error_stream_open_failed
import shiguangschedule.shared.generated.resources.error_webdav_unconfigured
import shiguangschedule.shared.generated.resources.item_backup_data
import shiguangschedule.shared.generated.resources.item_backup_restore
import shiguangschedule.shared.generated.resources.item_restore_data
import shiguangschedule.shared.generated.resources.item_webdav_config
import shiguangschedule.shared.generated.resources.label_webdav_account
import shiguangschedule.shared.generated.resources.label_webdav_path
import shiguangschedule.shared.generated.resources.label_webdav_pwd_empty
import shiguangschedule.shared.generated.resources.label_webdav_pwd_saved
import shiguangschedule.shared.generated.resources.label_webdav_url
import shiguangschedule.shared.generated.resources.section_data_maintenance
import shiguangschedule.shared.generated.resources.section_service_config
import shiguangschedule.shared.generated.resources.title_loading
import shiguangschedule.shared.generated.resources.toast_operation_failed
import shiguangschedule.shared.generated.resources.toast_operation_success
import shiguangschedule.shared.generated.resources.upload_24px
import kotlin.time.Clock

/**
 * 备份/恢复的目标媒介枚举
 */
enum class BackupTarget(val stringRes: StringResource) {
    WEBDAV(Res.string.backup_target_webdav),
    LOCAL_ZIP(Res.string.backup_target_local_zip)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onBack: () -> Unit,
    viewModel: BackupViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showConfigDialog by remember { mutableStateOf(false) }
    var showBackupTargetDialog by remember { mutableStateOf(false) }
    var showRestoreTargetDialog by remember { mutableStateOf(false) }

    val streamOpenFailedMsg = stringResource(Res.string.error_stream_open_failed)
    val webdavUnconfiguredMsg = stringResource(Res.string.error_webdav_unconfigured)
    val opSuccessMsg = stringResource(Res.string.toast_operation_success)
    val opFailedPrefix = stringResource(Res.string.toast_operation_failed, "")

    // 初始化 KMP 平台的 FileManager 回调
    val fileManager = rememberFileManager(
        FileManagerCallbacks(
            onFileImported = { bytes, _ ->
                if (bytes != null) {
                    scope.launch {
                        try {
                            // 将 ByteArray 转化为 Okio 的 Buffer/BufferedSource
                            val buffer = Buffer().write(bytes)
                            viewModel.importFromLocalZip(buffer)
                        } catch (_: Exception) {
                            snackbarHostState.showSnackbar(streamOpenFailedMsg)
                        }
                    }
                }
            },
            onFileExported = { success ->
                if (!success) {
                    scope.launch { snackbarHostState.showSnackbar(streamOpenFailedMsg) }
                }
            }
        )
    )

    LaunchedEffect(state.testResult) {
        when (val result = state.testResult) {
            is TestResult.Error -> {
                snackbarHostState.showSnackbar(
                    message = opFailedPrefix + result.message,
                    duration = SnackbarDuration.Short
                )
            }
            is TestResult.Success -> {
                snackbarHostState.showSnackbar(
                    message = opSuccessMsg,
                    duration = SnackbarDuration.Short
                )
            }
            TestResult.Idle -> {}
        }
    }

    UiScaffold(
        topBar = {
            UiTopAppBar(title = stringResource(Res.string.item_backup_restore), onBack = onBack)
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                if (LocalUiStyle.current == UiStyle.MIUIX) MiuixCourseSnackbar(data)
                else if (LocalUiStyle.current == UiStyle.COUI) CouiCourseSnackbar(data)
                else androidx.compose.material3.Snackbar(data)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (state.isBusy || state.isTesting) {
                when (LocalUiStyle.current) {
                    UiStyle.MIUIX -> top.yukonga.miuix.kmp.basic.CircularProgressIndicator(Modifier.size(24.dp))
                    UiStyle.COUI -> Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        UiLoadingIndicator(Modifier.size(24.dp))
                        UiText(stringResource(Res.string.title_loading))
                    }
                    UiStyle.MATERIAL ->
                        androidx.compose.material3.LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            CardGroup(title = stringResource(Res.string.section_data_maintenance)) {
                MenuActionItem(
                    title = stringResource(Res.string.item_backup_data),
                    icon = vectorResource(Res.drawable.upload_24px),
                    enabled = !state.isBusy,
                    onClick = { showBackupTargetDialog = true }
                )
                UiListDivider()
                MenuActionItem(
                    title = stringResource(Res.string.item_restore_data),
                    icon = vectorResource(Res.drawable.download_24px),
                    enabled = !state.isBusy,
                    onClick = { showRestoreTargetDialog = true }
                )
            }

            CardGroup(title = stringResource(Res.string.section_service_config)) {
                MenuActionItem(
                    title = stringResource(Res.string.item_webdav_config),
                    subtitle = if (state.baseUrl.isBlank()) {
                        stringResource(Res.string.desc_webdav_unconfigured)
                    } else {
                        stringResource(Res.string.desc_webdav_connected, state.baseUrl)
                    },
                    icon = vectorResource(Res.drawable.cloud_24px),
                    onClick = { showConfigDialog = true }
                )
            }
        }
    }

    if (showConfigDialog) {
        WebDavConfigDialog(
            state = state,
            onDismiss = { showConfigDialog = false },
            onSave = { u, n, p, r ->
                viewModel.testWebDavConnection(u, n, p, r)
                showConfigDialog = false
            },
            onDisconnect = {
                viewModel.disconnectWebDav()
                showConfigDialog = false
            }
        )
    }

    if (showBackupTargetDialog) {
        TargetSelectionDialog(
            title = stringResource(Res.string.dialog_title_backup_target),
            onDismiss = { showBackupTargetDialog = false },
            onTargetSelected = { target ->
                showBackupTargetDialog = false
                when (target) {
                    BackupTarget.WEBDAV -> {
                        if (state.baseUrl.isNotBlank()) {
                            viewModel.backupToWebDav()
                        } else {
                            scope.launch { snackbarHostState.showSnackbar(webdavUnconfiguredMsg) }
                        }
                    }
                    BackupTarget.LOCAL_ZIP -> {
                        scope.launch {
                            val buffer = Buffer()
                            val isSuccess = viewModel.exportToLocalZip(buffer)

                            if (isSuccess) {
                                val bytes = buffer.readByteArray()

                                if (bytes.isNotEmpty()) {
                                    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                                    val year = now.year
                                    val month = now.month.number.toString().padStart(2, '0')
                                    val day = now.day.toString().padStart(2, '0')
                                    val defaultFileName = "XATU_One_Backup_${year}${month}${day}.zip"
                                    fileManager.exportFile(defaultFileName, bytes)
                                } else {
                                    snackbarHostState.showSnackbar(streamOpenFailedMsg)
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    if (showRestoreTargetDialog) {
        TargetSelectionDialog(
            title = stringResource(Res.string.dialog_title_restore_source),
            onDismiss = { showRestoreTargetDialog = false },
            onTargetSelected = { target ->
                showRestoreTargetDialog = false
                when (target) {
                    BackupTarget.WEBDAV -> {
                        if (state.baseUrl.isNotBlank()) {
                            viewModel.restoreFromWebDav()
                        } else {
                            scope.launch { snackbarHostState.showSnackbar(webdavUnconfiguredMsg) }
                        }
                    }
                    BackupTarget.LOCAL_ZIP -> {
                        fileManager.importFile(listOf("zip"))
                    }
                }
            }
        )
    }
}

@Composable
fun CardGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (LocalUiStyle.current) {
            UiStyle.MIUIX -> {
                top.yukonga.miuix.kmp.basic.SmallTitle(title)
                top.yukonga.miuix.kmp.basic.Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth(), content = content)
                }
            }
            UiStyle.COUI -> com.itos.coui.layout.Group(title = title, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth(), content = content)
            }
            UiStyle.MATERIAL -> {
                Text(title, style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) { Column(Modifier.fillMaxWidth(), content = content) }
            }
        }
    }
}

@Composable
fun MenuActionItem(
    title: String,
    subtitle: String = "",
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    when (LocalUiStyle.current) {
        UiStyle.MIUIX -> top.yukonga.miuix.kmp.basic.BasicComponent(
            modifier = Modifier.fillMaxWidth(),
            title = title,
            summary = subtitle.takeIf { it.isNotEmpty() },
            startAction = { top.yukonga.miuix.kmp.basic.Icon(icon, contentDescription = null) },
            onClick = onClick,
            enabled = enabled,
            insideMargin = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        )
        UiStyle.COUI -> com.itos.coui.layout.Item(
            modifier = Modifier.fillMaxWidth(),
            icon = { Icon(icon, contentDescription = null) },
            title = { com.itos.coui.text.Text(text = title, kind = com.itos.coui.text.CouiTextKind.PreferenceTitle) },
            summary = subtitle.takeIf { it.isNotEmpty() }?.let {
                { com.itos.coui.text.Text(text = it, kind = com.itos.coui.text.CouiTextKind.PreferenceSummary) }
            },
            enabled = enabled,
            onClick = onClick,
        )
        UiStyle.MATERIAL -> ListItem(
            headlineContent = { Text(title, style = MaterialTheme.typography.titleMedium) },
            supportingContent = if (subtitle.isNotEmpty()) {
                { Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else null,
            leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) },
            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
            modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick).padding(vertical = 4.dp)
        )
    }
}

@Composable
fun TargetSelectionDialog(
    title: String,
    onDismiss: () -> Unit,
    onTargetSelected: (BackupTarget) -> Unit
) {
    var selectedTarget by rememberSaveable { mutableStateOf(BackupTarget.LOCAL_ZIP) }

    UiDialog(
        onDismissRequest = onDismiss,
        title = title,
        text = {
            listOf(BackupTarget.LOCAL_ZIP, BackupTarget.WEBDAV).forEach { target ->
                UiSelectionCard(
                    selected = target == selectedTarget,
                    onClick = { selectedTarget = target },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    UiText(stringResource(target.stringRes), Modifier.padding(12.dp))
                }
            }
        },
        dismissButton = UiDialogAction(stringResource(Res.string.action_cancel), onDismiss),
        confirmButton = UiDialogAction(stringResource(Res.string.action_confirm), { onTargetSelected(selectedTarget) }),
    )
}

@Composable
fun WebDavConfigDialog(
    state: BackupUiState,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit,
    onDisconnect: () -> Unit
) {
    var inputUrl by remember { mutableStateOf(state.baseUrl) }
    var inputUsername by remember { mutableStateOf(state.username) }
    var inputPassword by remember { mutableStateOf("") }
    var inputRootPath by remember { mutableStateOf(state.rootPath) }
    var showDisconnectConfirmation by remember { mutableStateOf(false) }
    val hasSavedConfig = state.baseUrl.isNotBlank()

    if (LocalUiStyle.current == UiStyle.COUI) {
        UiDialog(
            onDismissRequest = { if (!state.isTesting) onDismiss() },
            title = stringResource(Res.string.dialog_title_config_webdav),
            text = {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    UiInputField(
                        value = inputUrl,
                        onValueChange = { inputUrl = it },
                        label = stringResource(Res.string.label_webdav_url),
                        singleLine = true,
                    )
                    UiInputField(
                        value = inputUsername,
                        onValueChange = { inputUsername = it },
                        label = stringResource(Res.string.label_webdav_account),
                        singleLine = true,
                    )
                    CouiPasswordInputField(
                        value = inputPassword,
                        onValueChange = { inputPassword = it },
                        label = stringResource(
                            if (state.hasSavedPassword) Res.string.label_webdav_pwd_saved
                            else Res.string.label_webdav_pwd_empty
                        ),
                    )
                    UiInputField(
                        value = inputRootPath,
                        onValueChange = { inputRootPath = it },
                        label = stringResource(Res.string.label_webdav_path),
                        singleLine = true,
                    )
                }
            },
            neutralButton = if (hasSavedConfig) UiDialogAction(
                text = stringResource(Res.string.action_disconnect_webdav),
                enabled = !state.isTesting,
                onClick = { showDisconnectConfirmation = true },
            ) else null,
            confirmButton = UiDialogAction(
                text = if (state.isTesting) stringResource(Res.string.title_loading)
                else stringResource(Res.string.action_confirm),
                enabled = !state.isTesting,
                onClick = { onSave(inputUrl, inputUsername, inputPassword, inputRootPath) },
            ),
            dismissButton = UiDialogAction(
                text = stringResource(Res.string.action_cancel),
                enabled = !state.isTesting,
                onClick = onDismiss,
            ),
        )
    } else if (LocalUiStyle.current == UiStyle.MIUIX) {
        UiDialog(
            onDismissRequest = { if (!state.isTesting) onDismiss() },
            title = stringResource(Res.string.dialog_title_config_webdav),
            text = {
                Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    top.yukonga.miuix.kmp.basic.TextField(
                        value = inputUrl,
                        onValueChange = { inputUrl = it },
                        label = stringResource(Res.string.label_webdav_url),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    top.yukonga.miuix.kmp.basic.TextField(
                        value = inputUsername,
                        onValueChange = { inputUsername = it },
                        label = stringResource(Res.string.label_webdav_account),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    top.yukonga.miuix.kmp.basic.TextField(
                        value = inputPassword,
                        onValueChange = { inputPassword = it },
                        label = stringResource(
                            if (state.hasSavedPassword) Res.string.label_webdav_pwd_saved
                            else Res.string.label_webdav_pwd_empty
                        ),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    top.yukonga.miuix.kmp.basic.TextField(
                        value = inputRootPath,
                        onValueChange = { inputRootPath = it },
                        label = stringResource(Res.string.label_webdav_path),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            neutralButton = if (hasSavedConfig) UiDialogAction(
                text = stringResource(Res.string.action_disconnect_webdav),
                enabled = !state.isTesting,
                onClick = { showDisconnectConfirmation = true },
            ) else null,
            confirmButton = UiDialogAction(
                text = if (state.isTesting) stringResource(Res.string.title_loading)
                else stringResource(Res.string.action_confirm),
                enabled = !state.isTesting,
                onClick = { onSave(inputUrl, inputUsername, inputPassword, inputRootPath) },
            ),
            dismissButton = UiDialogAction(
                text = stringResource(Res.string.action_cancel),
                enabled = !state.isTesting,
                onClick = onDismiss,
            ),
        )
    } else AlertDialog(
        onDismissRequest = { if (!state.isTesting) onDismiss() },
        title = { Text(stringResource(Res.string.dialog_title_config_webdav)) },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = inputUrl,
                    onValueChange = { inputUrl = it },
                    label = { Text(stringResource(Res.string.label_webdav_url)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = inputUsername,
                    onValueChange = { inputUsername = it },
                    label = { Text(stringResource(Res.string.label_webdav_account)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = inputPassword,
                    onValueChange = { inputPassword = it },
                    label = {
                        Text(
                            if (state.hasSavedPassword) stringResource(Res.string.label_webdav_pwd_saved)
                            else stringResource(Res.string.label_webdav_pwd_empty)
                        )
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = inputRootPath,
                    onValueChange = { inputRootPath = it },
                    label = { Text(stringResource(Res.string.label_webdav_path)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (hasSavedConfig) {
                    TextButton(
                        onClick = { showDisconnectConfirmation = true },
                        enabled = !state.isTesting,
                    ) {
                        Text(
                            stringResource(Res.string.action_disconnect_webdav),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(inputUrl, inputUsername, inputPassword, inputRootPath) },
                enabled = !state.isTesting
            ) {
                Text(
                    if (state.isTesting) stringResource(Res.string.title_loading)
                    else stringResource(Res.string.action_confirm)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !state.isTesting) {
                Text(stringResource(Res.string.action_cancel))
            }
        }
    )

    if (showDisconnectConfirmation) {
        UiDialog(
            onDismissRequest = { showDisconnectConfirmation = false },
            title = stringResource(Res.string.dialog_title_disconnect_webdav),
            text = { UiText(stringResource(Res.string.dialog_message_disconnect_webdav)) },
            dismissButton = UiDialogAction(
                text = stringResource(Res.string.action_cancel),
                onClick = { showDisconnectConfirmation = false }
            ),
            confirmButton = UiDialogAction(
                text = stringResource(Res.string.action_disconnect_webdav),
                onClick = {
                    showDisconnectConfirmation = false
                    onDisconnect()
                }
            )
        )
    }
}

@Composable
private fun CouiPasswordInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = PasswordVisualTransformation(),
        textStyle = rememberCouiTextStyle(CouiTextKind.Body).copy(color = CouiTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(CouiTheme.colorScheme.accent),
        decorationBox = { innerTextField ->
            com.itos.coui.layout.Item(
                title = {
                    com.itos.coui.text.Text(
                        text = label,
                        kind = CouiTextKind.PreferenceTitle,
                    )
                },
                summary = {
                    Box(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        innerTextField()
                    }
                },
            )
        },
    )
}
