package com.xingheyuzhuan.shiguangschedule.ui.settings.notification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import com.xingheyuzhuan.shiguangschedule.ui.components.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.item_auto_mode
import shiguangschedule.shared.generated.resources.item_background_and_autostart
import shiguangschedule.shared.generated.resources.item_compat_wearable_sync
import shiguangschedule.shared.generated.resources.item_course_reminder
import shiguangschedule.shared.generated.resources.item_notification_permission
import shiguangschedule.shared.generated.resources.item_dnd_permission
import shiguangschedule.shared.generated.resources.item_exact_alarm_permission
import shiguangschedule.shared.generated.resources.item_ignore_battery_optimization
import shiguangschedule.shared.generated.resources.item_remind_time_before
import shiguangschedule.shared.generated.resources.remind_time_minutes_format
import com.xingheyuzhuan.shiguangschedule.data.model.UiStyle
import shiguangschedule.shared.generated.resources.section_reminder_options
import shiguangschedule.shared.generated.resources.section_reminder_permissions
import shiguangschedule.shared.generated.resources.status_authorized
import shiguangschedule.shared.generated.resources.status_disabled
import shiguangschedule.shared.generated.resources.status_enabled
import shiguangschedule.shared.generated.resources.status_unauthorized

/**
 * 常规设置卡片 UI 组件 (Android 专属)
 */
@Composable
fun GeneralSettingsCard(
    uiState: NotificationSettingsUiState,
    currentModeText: String?,
    notificationsGranted: Boolean,
    onNotificationSettingsClick: () -> Unit,
    onReminderToggle: (Boolean) -> Unit,
    onCompatWearableToggle: (Boolean) -> Unit,
    onAutoModeClick: () -> Unit,
    onRemindTimeClick: () -> Unit,
    onAppSettingsClick: () -> Unit,
    onBatteryOptimizationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // 仅在开启提醒开关但无权限时弹窗引导
    var showExactAlarmDialog by remember { mutableStateOf(false) }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(if (LocalUiStyle.current == UiStyle.COUI) 0.dp else 12.dp)) {
        ReminderSection(stringResource(Res.string.section_reminder_options)) {
            UiSettingRow(
                title = stringResource(Res.string.item_course_reminder),
                modifier = Modifier.toggleable(uiState.reminderEnabled, role = Role.Switch) { enable ->
                    if (enable && !hasExactAlarmPermission(context)) showExactAlarmDialog = true
                    else onReminderToggle(enable)
                },
                trailingContent = { UiSwitch(uiState.reminderEnabled, onCheckedChange = null) },
            )
            SettingItemRow(
                title = stringResource(Res.string.item_remind_time_before),
                currentValue = stringResource(Res.string.remind_time_minutes_format, uiState.remindBeforeMinutes),
                onClick = onRemindTimeClick,
            )
            SettingItemRow(stringResource(Res.string.item_auto_mode), currentValue = currentModeText, onClick = onAutoModeClick)
            UiSettingRow(
                title = stringResource(Res.string.item_compat_wearable_sync),
                modifier = Modifier.toggleable(uiState.compatWearableSync, role = Role.Switch, onValueChange = onCompatWearableToggle),
                trailingContent = { UiSwitch(uiState.compatWearableSync, onCheckedChange = null) },
            )
        }
        ReminderSection(stringResource(Res.string.section_reminder_permissions)) {
            SettingItemRow(
                title = stringResource(Res.string.item_notification_permission),
                currentValue = stringResource(if (notificationsGranted) Res.string.status_authorized else Res.string.status_unauthorized),
                onClick = onNotificationSettingsClick,
            )
            SettingItemRow(
                title = stringResource(Res.string.item_exact_alarm_permission),
                currentValue = stringResource(if (uiState.exactAlarmStatus) Res.string.status_enabled else Res.string.status_disabled),
                onClick = { openExactAlarmSettings(context) },
            )
            SettingItemRow(
                title = stringResource(Res.string.item_dnd_permission),
                currentValue = stringResource(if (uiState.dndPermissionStatus) Res.string.status_authorized else Res.string.status_unauthorized),
                onClick = { openDndSettings(context) },
            )
            SettingItemRow(stringResource(Res.string.item_background_and_autostart), onClick = onAppSettingsClick)
            SettingItemRow(stringResource(Res.string.item_ignore_battery_optimization), onClick = onBatteryOptimizationClick)
        }
    }

    // 点击开启上课提醒无权限时显示的弹窗
    if (showExactAlarmDialog) {
        ExactAlarmPermissionGuideDialog(
            onDismiss = { showExactAlarmDialog = false }
        )
    }
}

@Composable
private fun ReminderSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    when (LocalUiStyle.current) {
        UiStyle.MIUIX -> MiuixSettingsGroup(title, content)
        UiStyle.COUI -> CouiSettingsGroup(title, content)
        UiStyle.MATERIAL -> Column(Modifier.fillMaxWidth()) {
            UiText(title, Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}
