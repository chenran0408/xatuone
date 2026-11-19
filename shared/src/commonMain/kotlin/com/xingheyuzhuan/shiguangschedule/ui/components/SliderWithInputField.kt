package com.xingheyuzhuan.shiguangschedule.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.action_cancel
import shiguangschedule.shared.generated.resources.action_confirm
import shiguangschedule.shared.generated.resources.label_range
import shiguangschedule.shared.generated.resources.placeholder_input_value
import kotlin.math.roundToInt

/**
 * 通用带数字手动输入弹窗的 Slider 组件
 *
 * @param label 标题文本
 * @param value 当前浮点数值
 * @param range 数值可选范围
 * @param stepValue 步进粒度，小于 1f 时按小数格式化，大于等于 1f 时按整数处理
 * @param modifier 外部修饰符
 * @param onValueChange 数值变更回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SliderWithInputField(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    stepValue: Float = 1f,
    modifier: Modifier = Modifier,
    onValueChange: (Float) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    val isIntegerStep = stepValue >= 1f

    fun formatValue(v: Float): String {
        return if (isIntegerStep) {
            "${v.toInt()}"
        } else {
            val factor = (1f / stepValue).roundToInt().coerceAtLeast(10)
            val rounded = (v * factor).roundToInt() / factor.toDouble()
            if (rounded % 1.0 == 0.0) "${rounded.toInt()}.0" else "$rounded"
        }
    }

    val steps = remember(range, stepValue) {
        if (stepValue > 0f) {
            ((range.endInclusive - range.start) / stepValue).toInt() - 1
        } else 0
    }

    if (showDialog) {
        var textFieldValue by remember { mutableStateOf(formatValue(value)) }

        val entered = textFieldValue.toFloatOrNull()
        UiDialog(
            onDismissRequest = { showDialog = false },
            title = label,
            text = {
                Column {
                    UiText(
                        text = "${stringResource(Res.string.label_range)}: ${formatValue(range.start)} - ${formatValue(range.endInclusive)}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    UiInputField(
                        value = textFieldValue,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() || (!isIntegerStep && it == '.') }) textFieldValue = input
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = if (isIntegerStep) KeyboardType.Number else KeyboardType.Decimal),
                        label = stringResource(Res.string.placeholder_input_value),
                    )
                }
            },
            confirmButton = UiDialogAction(stringResource(Res.string.action_confirm), {
                entered?.let { number ->
                    val clamped = number.coerceIn(range.start, range.endInclusive)
                    val stepped = if (stepValue > 0f) {
                        range.start + ((clamped - range.start) / stepValue).roundToInt() * stepValue
                    } else clamped
                    onValueChange(stepped.coerceIn(range.start, range.endInclusive))
                    showDialog = false
                }
            }, enabled = entered != null),
            dismissButton = UiDialogAction(stringResource(Res.string.action_cancel), { showDialog = false }),
        )
    }

    Column(modifier = modifier) {
        if (LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.COUI) {
            com.itos.coui.layout.Item(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp),
                title = { com.itos.coui.text.Text(text = label, kind = com.itos.coui.text.CouiTextKind.PreferenceTitle) },
                trailing = { com.itos.coui.text.Text(text = formatValue(value), kind = com.itos.coui.text.CouiTextKind.PreferenceValue,
                    color = com.itos.coui.theme.CouiTheme.colorScheme.accentOnSurface) },
                onClick = { showDialog = true },
            )
            com.itos.coui.slider.Slider(
                modifier = Modifier.fillMaxWidth(), value = value, onValueChange = onValueChange,
                valueRange = range, steps = steps.coerceAtLeast(0),
            )
            return@Column
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { showDialog = true }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = formatValue(value),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = if (steps > 0) steps else 0,
            modifier = Modifier.height(32.dp),
            thumb = {
                Surface(
                    modifier = Modifier.size(16.dp),
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 1.dp,
                    border = BorderStroke(0.5.dp, Color.LightGray.copy(alpha = 0.5f))
                ) {}
            },
            track = { sliderState ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(22.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.CenterStart
                ) {
                    SliderDefaults.Track(
                        sliderState = sliderState,
                        modifier = Modifier.fillMaxWidth().height(22.dp),
                        colors = SliderDefaults.colors(
                            activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            inactiveTrackColor = Color.Transparent
                        ),
                        thumbTrackGapSize = 0.dp,
                        trackInsideCornerSize = 0.dp
                    )
                }
            }
        )
    }
}