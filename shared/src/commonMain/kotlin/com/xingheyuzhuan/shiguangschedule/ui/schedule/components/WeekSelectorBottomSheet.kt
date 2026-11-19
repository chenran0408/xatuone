package com.xingheyuzhuan.shiguangschedule.ui.schedule.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import com.xingheyuzhuan.shiguangschedule.ui.components.UiText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.foundation.layout.heightIn
import com.xingheyuzhuan.shiguangschedule.ui.components.UiSheet
import com.xingheyuzhuan.shiguangschedule.ui.components.UiSelectionCard
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.title_select_week
import shiguangschedule.shared.generated.resources.title_current_week
import shiguangschedule.shared.generated.resources.format_week_display

/**
 * 周选择器底部动作条。
 *
 * @param totalWeeks 学期总周数。
 * @param currentWeek 当前的自然周数。
 * @param onWeekSelected 当用户选择周次时触发的回调。
 * @param onDismissRequest 当底部动作条被关闭时触发的回调。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekSelectorBottomSheet(
    totalWeeks: Int,
    currentWeek: Int?,
    selectedWeek: Int?,
    onWeekSelected: (Int) -> Unit,
    onDismissRequest: () -> Unit
) {
    val firstWeek = selectedWeek?.takeIf { it in 1..totalWeeks }
        ?: currentWeek?.takeIf { it in 1..totalWeeks }
        ?: 1
    val gridState = rememberLazyGridState(initialFirstVisibleItemIndex = firstWeek - 1)

    UiSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            UiText(
                text = stringResource(Res.string.title_select_week),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )

            // 网格状的周次选择器
            LazyVerticalGrid(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                columns = GridCells.Adaptive(minSize = maxOf(56.dp, 48.dp * LocalDensity.current.fontScale)),
                state = gridState,
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(totalWeeks) { weekIndex ->
                    val weekNumber = weekIndex + 1
                    val isCurrentWeek = weekNumber == currentWeek
                    val isSelectedWeek = weekNumber == selectedWeek

                    val weekLabel = stringResource(Res.string.format_week_display, weekNumber)
                    val currentLabel = stringResource(Res.string.title_current_week, weekNumber.toString())
                    UiSelectionCard(
                        selected = isSelectedWeek,
                        highlighted = isCurrentWeek,
                        compact = true,
                        onClick = { onWeekSelected(weekNumber) },
                        modifier = Modifier.semantics {
                            if (isCurrentWeek) stateDescription = currentLabel
                        }
                    ) {
                        Box(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(8.dp), contentAlignment = Alignment.Center) {
                            UiText(text = weekLabel, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
