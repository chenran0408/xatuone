package com.xingheyuzhuan.shiguangschedule.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import org.jetbrains.compose.resources.vectorResource
import shiguangschedule.shared.generated.resources.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.Destination
import com.xingheyuzhuan.shiguangschedule.ui.components.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private data class SettingsEntry(
    val title: String,
    val summary: String = "",
    val destination: Destination,
    val icon: org.jetbrains.compose.resources.DrawableResource,
)

private data class SettingsSection(val title: String, val entries: List<SettingsEntry>)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHomeScreen(onNavigate: (Destination) -> Unit) {
    val viewModel: SettingsViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsState()
    val style = LocalUiStyle.current
    val appearance = "${state.appSettings.uiStyle.label} · ${stringResource(state.appSettings.themeMode.labelRes)}"
    val sections = listOf(
        SettingsSection("学习与校园", listOf(
            SettingsEntry("课程表设置", destination = Destination.CourseTools, icon = Res.drawable.view_week_24px),
            SettingsEntry("教务设置", destination = Destination.AcademicSettings, icon = Res.drawable.school_24px),
            SettingsEntry("校园服务设置", destination = Destination.CampusSettings, icon = Res.drawable.location_on_24px),
        )),
        SettingsSection("应用与账号", listOf(
            SettingsEntry("外观设置", appearance, Destination.ThemeSettings, Res.drawable.palette_24px),
            SettingsEntry("账号与隐私", destination = Destination.XatuLogin, icon = Res.drawable.account_circle_24px),
            SettingsEntry("系统设置", destination = Destination.MoreOptions, icon = Res.drawable.tune_24px),
        )),
    )
    UiScaffold(
        containerColor = uiStyleColors().background,
        topBar = { UiTopAppBar(title = "设置") }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().uiContentWidth().padding(padding),
            contentPadding = PaddingValues(start = if (style == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.COUI) 0.dp else 16.dp,
                end = if (style == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.COUI) 0.dp else 16.dp, bottom = UiMetrics.PageInset),
            verticalArrangement = Arrangement.spacedBy(if (style == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.COUI) 0.dp else 16.dp)
        ) {
            if (style == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.MATERIAL) {
                sections.forEach { section ->
                    item {
                        Text(section.title, Modifier.padding(start = 56.dp, top = 8.dp, bottom = 8.dp),
                            style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        section.entries.forEach { entry ->
                            UiSettingRow(
                                title = entry.title,
                                summary = entry.summary,
                                leadingContent = {
                                    Icon(vectorResource(entry.icon), null, tint = MaterialTheme.colorScheme.primary)
                                },
                                trailingContent = {
                                    Icon(vectorResource(Res.drawable.chevron_right_24px), null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                },
                                onClick = { onNavigate(entry.destination) },
                            )
                        }
                        HorizontalDivider(Modifier.padding(top = 8.dp))
                    }
                }
            } else if (style == com.xingheyuzhuan.shiguangschedule.data.model.UiStyle.MIUIX) {
                sections.forEach { section ->
                    item {
                        MiuixSettingsGroup(section.title) {
                            section.entries.forEach { entry ->
                                BoxWithConstraints(Modifier.fillMaxWidth()) {
                                    val inlineValue = entry.summary.isNotBlank() && maxWidth / LocalDensity.current.fontScale >= 320.dp
                                    UiSettingRow(
                                        title = entry.title,
                                        summary = if (inlineValue) "" else entry.summary,
                                        leadingContent = {
                                            val accent = uiStyleColors().accent
                                            Box(Modifier.size(32.dp).background(accent.copy(alpha = .10f), RoundedCornerShape(10.dp)),
                                                contentAlignment = Alignment.Center) {
                                                Icon(vectorResource(entry.icon), null, Modifier.size(20.dp), tint = accent)
                                            }
                                        },
                                        trailingContent = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (inlineValue) UiText(entry.summary, modifier = Modifier.padding(end = 4.dp),
                                                    style = MaterialTheme.typography.bodySmall, color = uiStyleColors().secondaryText)
                                                Icon(vectorResource(Res.drawable.chevron_right_24px), null, Modifier.size(20.dp), tint = uiStyleColors().secondaryText)
                                            }
                                        },
                                        onClick = { onNavigate(entry.destination) },
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                sections.forEach { section ->
                    item {
                        CouiSettingsGroup(section.title) {
                            section.entries.forEachIndexed { index, entry ->
                                if (index > 0) UiListDivider()
                                UiSettingRow(
                                    title = entry.title,
                                    summary = entry.summary,
                                    leadingContent = { Icon(vectorResource(entry.icon), null) },
                                    trailingContent = {
                                        Icon(vectorResource(Res.drawable.chevron_right_24px), null, tint = uiStyleColors().secondaryText)
                                    },
                                    onClick = { onNavigate(entry.destination) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
