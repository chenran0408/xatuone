package com.xingheyuzhuan.shiguangschedule.ui.settings.quickactions

import com.xingheyuzhuan.shiguangschedule.ui.components.UiScaffold
import com.xingheyuzhuan.shiguangschedule.ui.components.UiTopAppBar
import com.xingheyuzhuan.shiguangschedule.ui.components.LocalUiStyle
import com.xingheyuzhuan.shiguangschedule.ui.components.UiCard
import com.xingheyuzhuan.shiguangschedule.ui.components.UiListDivider
import com.xingheyuzhuan.shiguangschedule.ui.components.UiServiceRow
import com.xingheyuzhuan.shiguangschedule.data.model.UiStyle

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.Destination
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.chevron_right_24px
import shiguangschedule.shared.generated.resources.item_quick_actions
import shiguangschedule.shared.generated.resources.item_quick_delete
import shiguangschedule.shared.generated.resources.item_schedule_tweak
import shiguangschedule.shared.generated.resources.label_quick_action_category_schedule

/**
 * 快捷操作二级页面
 * 用于收纳高频使用的工具类功能，如调课、临时改动等
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionsScreen(
    onNavigate: (Destination) -> Unit,
    onBack: () -> Unit
) {
    UiScaffold(
        topBar = {
            UiTopAppBar(title = stringResource(Res.string.item_quick_actions), onBack = onBack)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = if (LocalUiStyle.current == UiStyle.COUI) 0.dp else 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (LocalUiStyle.current != UiStyle.COUI) Spacer(Modifier.height(16.dp))

            if (LocalUiStyle.current != UiStyle.MATERIAL) {
                UiCard(Modifier.fillMaxWidth()) {
                    UiServiceRow(stringResource(Res.string.item_schedule_tweak), "", { onNavigate(Destination.TweakSchedule) })
                    UiListDivider()
                    UiServiceRow(stringResource(Res.string.item_quick_delete), "", { onNavigate(Destination.QuickDelete) })
                }
            } else {
                Text(
                    text = stringResource(Res.string.label_quick_action_category_schedule),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                // 内容卡片
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // 调课功能项
                        QuickActionItem(
                            title = stringResource(Res.string.item_schedule_tweak),
                            onClick = { onNavigate(Destination.TweakSchedule) }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        // 快速删除功能项
                        QuickActionItem(
                            title = stringResource(Res.string.item_quick_delete),
                            onClick = { onNavigate(Destination.QuickDelete) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 快捷操作单项组件
 */
@Composable
private fun QuickActionItem(
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
        Icon(
            imageVector = vectorResource(Res.drawable.chevron_right_24px),
            contentDescription = null,
            modifier = Modifier.padding(start = 4.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
