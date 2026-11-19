package com.xingheyuzhuan.shiguangschedule.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.Destination
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseTableRepository
import com.xingheyuzhuan.shiguangschedule.ui.components.*
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.chevron_right_24px

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseToolsScreen(onNavigate: (Destination) -> Unit, onBack: () -> Unit) {
    val viewModel: SettingsViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsState()
    val tables: CourseTableRepository = koinInject()
    val courseTables by tables.getAllCourseTables().collectAsState(initial = emptyList())
    val current = courseTables.firstOrNull { it.id == state.appSettings.currentCourseTableId }
    val groups = listOf(
        "课表配置" to listOf(
            Triple("当前课表", current?.name ?: "尚未选择课表", Destination.ManageCourseTables),
            Triple("同步课程表", "", com.xingheyuzhuan.shiguangschedule.ui.settings.xatu.xatuCourseImport),
            Triple("日期与周次", "", Destination.CourseSettings),
            Triple("课程显示与颜色", "", Destination.StyleSettings),
            Triple("节次与作息", "", Destination.TimeScheduleManagement),
            Triple("假期与调休", "", Destination.HolidayManagement)),
        "管理与导入导出" to listOf(
            Triple("课程管理", "", Destination.CourseManagementList),
            Triple("批量调整课程", "", Destination.QuickActions),
            Triple("导入、导出与系统日历", "", Destination.CourseTableConversion))
    )
    val miuix = LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.MIUIX
    val material = LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.MATERIAL
    UiScaffold(
        containerColor = uiStyleColors().background,
        topBar = { UiTopAppBar(title = "课程表设置", onBack = onBack) }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().uiContentWidth().padding(padding),
            contentPadding = PaddingValues(horizontal = if (LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.COUI) 0.dp else 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(
                if (LocalUiStyle.current == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.COUI) 0.dp else 16.dp
            )
        ) {
            groups.forEach { (title, entries) ->
                item {
                    val rows: @Composable () -> Unit = {
                        entries.forEach { (label, value, destination) ->
                            if (material) ListItem(headlineContent = { Text(label) },
                                supportingContent = if (value.isNotEmpty()) ({ Text(value) }) else null,
                                trailingContent = { Icon(vectorResource(Res.drawable.chevron_right_24px), null) },
                                modifier = Modifier.clickable(role = androidx.compose.ui.semantics.Role.Button) { onNavigate(destination) })
                            else UiServiceRow(label, value, { onNavigate(destination) })
                        }
                    }
                    if (material) {
                        Text(title, Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        rows()
                        HorizontalDivider()
                    } else if (miuix) MiuixSettingsGroup(title) { rows() }
                    else CouiSettingsGroup(title) { rows() }
                }
            }
        }
    }
}
