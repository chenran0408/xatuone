package com.xingheyuzhuan.shiguangschedule.ui.settings.notification

import androidx.compose.material3.Icon
import com.xingheyuzhuan.shiguangschedule.ui.components.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.vectorResource
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.chevron_right_24px

/**
 * 设置项单行通用 UI 组件
 *
 * @param title 设置项标题
 * @param modifier 修饰符
 * @param currentValue 当前选中的状态或数值描述
 * @param onClick 行点击事件
 * @param trailing 尾部控件（如 Switch 或 RadioButton）
 */
@Composable
fun SettingItemRow(
    title: String,
    modifier: Modifier = Modifier,
    currentValue: String? = null,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit = {}
) {
    UiSettingRow(
        title = title,
        summary = currentValue.orEmpty(),
        modifier = modifier,
        onClick = onClick,
        trailingContent = {
            trailing()
            Icon(
                imageVector = vectorResource(Res.drawable.chevron_right_24px),
                contentDescription = null,
                tint = uiStyleColors().secondaryText,
            )
        },
    )
}
