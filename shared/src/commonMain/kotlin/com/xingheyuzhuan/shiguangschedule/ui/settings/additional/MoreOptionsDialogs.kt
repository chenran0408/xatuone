package com.xingheyuzhuan.shiguangschedule.ui.settings.additional

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import com.xingheyuzhuan.shiguangschedule.ui.components.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.data.model.StartScreen
import com.xingheyuzhuan.shiguangschedule.tool.UpdateStatus
import org.jetbrains.compose.resources.stringResource
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.action_cancel
import shiguangschedule.shared.generated.resources.action_confirm
import shiguangschedule.shared.generated.resources.btn_download_update
import shiguangschedule.shared.generated.resources.dialog_checking_update
import shiguangschedule.shared.generated.resources.dialog_current_version_latest
import shiguangschedule.shared.generated.resources.dialog_download_failed
import shiguangschedule.shared.generated.resources.dialog_download_queued
import shiguangschedule.shared.generated.resources.dialog_new_version_found
import shiguangschedule.shared.generated.resources.dialog_no_release
import shiguangschedule.shared.generated.resources.dialog_select_start_screen
import shiguangschedule.shared.generated.resources.dialog_update_check_failed
import shiguangschedule.shared.generated.resources.btn_view_release_page
import shiguangschedule.shared.generated.resources.label_error_message
import shiguangschedule.shared.generated.resources.label_version_prefix
import shiguangschedule.shared.generated.resources.text_download_queued
import shiguangschedule.shared.generated.resources.text_no_release

/**
 * 启动页面选择弹窗
 */
@Composable
fun StartScreenSelectionDialog(
    showDialog: Boolean,
    currentSelected: StartScreen,
    onDismiss: () -> Unit,
    onConfirm: (StartScreen) -> Unit
) {
    if (!showDialog) return

    UiDialog(
        onDismissRequest = onDismiss,
        title = stringResource(Res.string.dialog_select_start_screen),
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                StartScreen.entries.forEach { screen ->
                    UiSelectionCard(
                        selected = screen == currentSelected,
                        onClick = { onConfirm(screen) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { UiText(stringResource(screen.labelRes), Modifier.padding(12.dp)) }
                }
            }
        },
        confirmButton = UiDialogAction(onClick = onDismiss, text = stringResource(Res.string.action_cancel))
    )
}

/**
 * 更新检查结果弹窗
 */
@Composable
fun UpdateResultDialog(
    showDialog: Boolean,
    updateStatus: UpdateStatus,
    onDismiss: () -> Unit,
    onDownloadClick: (String) -> Unit,
    onOpenReleasePage: (String) -> Unit
) {
    if (!showDialog || updateStatus is UpdateStatus.Idle || updateStatus is UpdateStatus.NotSupported) return

    if (updateStatus is UpdateStatus.Checking) {
        UiDialog(
            onDismissRequest = { },
            title = stringResource(Res.string.dialog_checking_update),
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            },
            confirmButton = UiDialogAction(text = stringResource(Res.string.dialog_checking_update), onClick = {}, enabled = false)
        )
        return
    }

    val (title, text, confirmBtn) = when (updateStatus) {
        is UpdateStatus.Found -> Triple(
            stringResource(Res.string.dialog_new_version_found, updateStatus.versionName),
            updateStatus.changelog,
            if (updateStatus.isDirectDownload) {
                UiDialogAction(onClick = { onDownloadClick(updateStatus.targetUrl) }, text = stringResource(Res.string.btn_download_update))
            } else {
                UiDialogAction(onClick = { onOpenReleasePage(updateStatus.targetUrl) }, text = stringResource(Res.string.btn_view_release_page))
            }
        )
        is UpdateStatus.Latest -> Triple(
            stringResource(Res.string.dialog_current_version_latest),
            stringResource(Res.string.label_version_prefix, updateStatus.currentVersion),
            null
        )
        UpdateStatus.NoRelease -> Triple(
            stringResource(Res.string.dialog_no_release),
            stringResource(Res.string.text_no_release),
            null
        )
        is UpdateStatus.Error -> Triple(
            stringResource(Res.string.dialog_update_check_failed),
            stringResource(Res.string.label_error_message, updateStatus.message),
            null
        )
        UpdateStatus.DownloadQueued -> Triple(
            stringResource(Res.string.dialog_download_queued),
            stringResource(Res.string.text_download_queued),
            null
        )
        is UpdateStatus.DownloadFailed -> Triple(
            stringResource(Res.string.dialog_download_failed),
            stringResource(Res.string.label_error_message, updateStatus.message),
            null
        )
        UpdateStatus.Checking, UpdateStatus.Idle, UpdateStatus.NotSupported -> return
    }

    UiDialog(
        onDismissRequest = onDismiss,
        title = title,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 350.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                UiText(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = confirmBtn,
        dismissButton = UiDialogAction(onClick = onDismiss, text = stringResource(if (updateStatus is UpdateStatus.Found) Res.string.action_cancel else Res.string.action_confirm))
    )
}
