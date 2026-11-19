package com.xingheyuzhuan.shiguangschedule.ui.settings.notification

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.item_auto_mode
import shiguangschedule.shared.generated.resources.item_background_and_autostart
import shiguangschedule.shared.generated.resources.item_compat_wearable_sync
import shiguangschedule.shared.generated.resources.item_course_reminder
import shiguangschedule.shared.generated.resources.item_dnd_permission
import shiguangschedule.shared.generated.resources.item_exact_alarm_permission
import shiguangschedule.shared.generated.resources.item_ignore_battery_optimization
import shiguangschedule.shared.generated.resources.item_remind_time_before
import shiguangschedule.shared.generated.resources.remind_time_minutes_format
import shiguangschedule.shared.generated.resources.section_title_general
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

    Column(modifier = modifier) {
        val title = stringResource(Res.string.section_title_general)
        val settingsRows: @Composable (Modifier) -> Unit = { rowsModifier ->
            Column(modifier = rowsModifier) {
                // 1. 上课提醒主开关
                UiSettingRow(
                    title = stringResource(Res.string.item_course_reminder),
                    trailingContent = {
                        UiSwitch(
                            checked = uiState.reminderEnabled,
                            onCheckedChange = { targetState ->
                                if (targetState) {
                                    if (hasExactAlarmPermission(context)) {
                                        onReminderToggle(true)
                                    } else {
                                        showExactAlarmDialog = true
                                    }
                                } else {
                                    onReminderToggle(false)
                                }
                            }
                        )
                    },
                )

                UiListDivider()

                // 2. 兼容穿戴设备同步通知开关
                UiSettingRow(
                    title = stringResource(Res.string.item_compat_wearable_sync),
                    trailingContent = {
                        UiSwitch(
                            checked = uiState.compatWearableSync,
                            onCheckedChange = onCompatWearableToggle
                        )
                    },
                )

                UiListDivider()

                // 3. 上课自动模式
                SettingItemRow(
                    title = stringResource(Res.string.item_auto_mode),
                    currentValue = currentModeText,
                    onClick = onAutoModeClick
                )

                UiListDivider()

                // 4. 提前提醒时间
                SettingItemRow(
                    title = stringResource(Res.string.item_remind_time_before),
                    currentValue = stringResource(Res.string.remind_time_minutes_format, uiState.remindBeforeMinutes),
                    onClick = onRemindTimeClick
                )

                // 5. 精确闹钟权限 (Android 12+)：直接跳转页面
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    UiListDivider()
                    val statusText = if (uiState.exactAlarmStatus)
                        stringResource(Res.string.status_enabled)
                    else
                        stringResource(Res.string.status_disabled)

                    SettingItemRow(
                        title = stringResource(Res.string.item_exact_alarm_permission),
                        currentValue = statusText,
                        onClick = { openExactAlarmSettings(context) }
                    )
                }

                UiListDivider()

                // 6. 勿扰模式权限：直接跳转页面
                val dndStatusText = if (uiState.dndPermissionStatus)
                    stringResource(Res.string.status_authorized)
                else
                    stringResource(Res.string.status_unauthorized)

                SettingItemRow(
                    title = stringResource(Res.string.item_dnd_permission),
                    currentValue = dndStatusText,
                    onClick = { openDndSettings(context) }
                )

                UiListDivider()

                // 7. 后台与自启动
                SettingItemRow(
                    title = stringResource(Res.string.item_background_and_autostart),
                    onClick = onAppSettingsClick
                )

                UiListDivider()

                // 8. 忽略电池优化
                SettingItemRow(
                    title = stringResource(Res.string.item_ignore_battery_optimization),
                    onClick = onBatteryOptimizationClick
                )
            }
        }

        when (LocalUiStyle.current) {
            com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.COUI ->
                CouiSettingsGroup(title) { settingsRows(Modifier) }

            com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.MIUIX -> {
                top.yukonga.miuix.kmp.basic.SmallTitle(title)
                Spacer(Modifier.height(8.dp))
                UiCard(modifier = Modifier.fillMaxWidth()) {
                    settingsRows(Modifier.padding(16.dp))
                }
            }

            com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.MATERIAL -> {
                UiText(title, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                UiCard(modifier = Modifier.fillMaxWidth()) {
                    settingsRows(Modifier.padding(16.dp))
                }
            }
        }
    }

    // 点击开启上课提醒无权限时显示的弹窗
    if (showExactAlarmDialog) {
        ExactAlarmPermissionGuideDialog(
            onDismiss = { showExactAlarmDialog = false }
        )
    }
}
