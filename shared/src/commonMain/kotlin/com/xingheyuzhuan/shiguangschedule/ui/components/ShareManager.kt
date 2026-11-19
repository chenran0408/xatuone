package com.xingheyuzhuan.shiguangschedule.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.action_cancel
import shiguangschedule.shared.generated.resources.action_share
import shiguangschedule.shared.generated.resources.dialog_title_file_saved

/**
 * Android 底层文件分享执行逻辑
 */
expect fun platformShareFile(filePath: String, mimeType: String)

/**
 * 文件保存成功后的分享确认弹窗（分享管理器的 UI 组件之一）
 */
@Composable
fun ShareDialog(
    filePath: String,
    mimeType: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.dialog_title_file_saved)) },
        confirmButton = {
            TextButton(
                onClick = {
                    platformShareFile(filePath, mimeType)
                    onDismiss()
                }
            ) {
                Text(stringResource(Res.string.action_share))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel))
            }
        }
    )
}
