package com.xingheyuzhuan.shiguangschedule.ui.settings.themesettings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.xingheyuzhuan.shiguangschedule.ui.components.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.data.model.AppThemeMode
import com.xingheyuzhuan.shiguangschedule.data.model.UiStyle
import com.xingheyuzhuan.shiguangschedule.ui.components.AdvancedColorPicker
import com.xingheyuzhuan.shiguangschedule.ui.components.ColorPickerConfig
import com.xingheyuzhuan.shiguangschedule.ui.components.UiCard
import com.xingheyuzhuan.shiguangschedule.ui.components.UiText
import com.xingheyuzhuan.shiguangschedule.ui.settings.SettingsViewModel
import com.xingheyuzhuan.shiguangschedule.ui.theme.LocalIsDarkTheme
import com.xingheyuzhuan.shiguangschedule.ui.theme.supportsDynamicColor
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.action_reset
import shiguangschedule.shared.generated.resources.arrow_drop_down_24px
import shiguangschedule.shared.generated.resources.custom_color_title
import shiguangschedule.shared.generated.resources.dark_primary_color
import shiguangschedule.shared.generated.resources.dynamic_color_title
import shiguangschedule.shared.generated.resources.light_primary_color
import shiguangschedule.shared.generated.resources.theme_mode_label

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings = uiState.appSettings
    val styleSaveStatus by viewModel.styleSaveStatus.collectAsState()
    var picker by rememberSaveable { mutableStateOf<AppearancePicker?>(null) }
    val modeLabel = stringResource(Res.string.theme_mode_label)
    val materialColorPrefix = if (settings.uiStyle == UiStyle.MATERIAL) "" else "Material 3 · "

    val surfaceColor = uiStyleColors().background

    UiScaffold(
        containerColor = surfaceColor,
        topBar = { UiTopAppBar(title = "外观设置", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize().uiContentWidth()
                .verticalScroll(rememberScrollState())
                .padding(if (settings.uiStyle == UiStyle.COUI) 0.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(UiMetrics.SectionGap)
        ) {
            val appearanceRows: @Composable ColumnScope.() -> Unit = {
                AppearancePickerRow(
                    title = modeLabel,
                    value = stringResource(settings.themeMode.labelRes),
                    onClick = { picker = AppearancePicker.MODE },
                )
                UiListDivider()
                AppearancePickerRow(
                    title = "界面风格",
                    value = settings.uiStyle.label,
                    onClick = { picker = AppearancePicker.STYLE },
                )
                if (supportsDynamicColor) {
                    UiListDivider()
                    DynamicColorToggle(
                        title = materialColorPrefix + stringResource(Res.string.dynamic_color_title),
                        enabled = settings.useDynamicColor,
                        onEnabledChange = { viewModel.onUseDynamicColorChanged(it) },
                    )
                }
                if (styleSaveStatus.isNotBlank()) {
                    UiText(
                        styleSaveStatus,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = if ("失败" in styleSaveStatus) MaterialTheme.colorScheme.error else uiStyleColors().secondaryText,
                    )
                }
            }
            when (LocalUiStyle.current) {
                UiStyle.MATERIAL -> UiCard(modifier = Modifier.fillMaxWidth()) {
                    Column(content = appearanceRows)
                }
                UiStyle.MIUIX -> MiuixSettingsGroup("主题与颜色", content = appearanceRows)
                UiStyle.COUI -> CouiSettingsGroup("主题与颜色", content = appearanceRows)
            }

            when (picker) {
                AppearancePicker.MODE -> AppearanceOptionSheet(
                    modeLabel, AppThemeMode.entries.map { stringResource(it.labelRes) }, settings.themeMode.ordinal,
                    onDismiss = { picker = null },
                ) { viewModel.onThemeModeChanged(AppThemeMode.entries[it]) }
                AppearancePicker.STYLE -> AppearanceOptionSheet(
                    "界面风格", UiStyle.entries.map { it.label }, settings.uiStyle.ordinal,
                    onDismiss = { picker = null },
                ) { viewModel.onUiStyleChanged(UiStyle.entries[it]) }
                null -> Unit
            }

            // 自定义主色调选择
            val showColorPicker = !supportsDynamicColor || !settings.useDynamicColor

            AnimatedVisibility(
                visible = showColorPicker,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = if (settings.uiStyle == UiStyle.COUI) 16.dp else 0.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    SectionHeader(materialColorPrefix + stringResource(Res.string.custom_color_title))

                    val isDark = LocalIsDarkTheme.current

                    if (isDark) {
                        ColorPickerItem(
                            label = stringResource(Res.string.dark_primary_color),
                            currentColor = Color(settings.customPrimaryColor.dark),
                            onColorChanged = { viewModel.onCustomDarkPrimaryChanged(it) },
                            onReset = { viewModel.onCustomDarkPrimaryChanged() }
                        )
                    } else {
                        ColorPickerItem(
                            label = stringResource(Res.string.light_primary_color),
                            currentColor = Color(settings.customPrimaryColor.light),
                            onColorChanged = { viewModel.onCustomLightPrimaryChanged(it) },
                            onReset = { viewModel.onCustomLightPrimaryChanged() }
                        )
                    }

                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColorPickerItem(
    label: String,
    currentColor: Color,
    onColorChanged: (Color) -> Unit,
    onReset: () -> Unit
) {
    var showSheet by remember { mutableStateOf(false) }
    UiSettingRow(
        title = label,
        onClick = { showSheet = true },
        trailingContent = {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(currentColor)
            )
        },
    )

    if (showSheet) {
        UiSheet(onDismissRequest = { showSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 40.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UiText(text = label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                    UiActionButton(onClick = onReset, text = stringResource(Res.string.action_reset))
                }
                AdvancedColorPicker(
                    initialColor = currentColor,
                    onColorChanged = onColorChanged,
                    config = ColorPickerConfig(
                        showAlpha = false,
                        showInputMode = true
                    )
                )
            }
        }
    }
}

private enum class AppearancePicker { MODE, STYLE }

@Composable
private fun AppearanceOptionSheet(
    title: String, options: List<String>, selectedIndex: Int, onDismiss: () -> Unit, onSelect: (Int) -> Unit,
) {
    UiSheet(onDismissRequest = onDismiss, skipPartiallyExpanded = true) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = UiMetrics.PageInset, vertical = UiMetrics.ContentGap)
                .padding(bottom = UiMetrics.PageInset),
            verticalArrangement = Arrangement.spacedBy(UiMetrics.ContentGap),
        ) {
            UiText(title, style = MaterialTheme.typography.titleLarge)
            options.forEachIndexed { index, label ->
                UiSelectionCard(
                    selected = selectedIndex == index,
                    onClick = {
                        onDismiss()
                        if (selectedIndex != index) onSelect(index)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { UiText(label, Modifier.padding(12.dp)) }
            }
        }
    }
}

@Composable
private fun DynamicColorToggle(
    title: String,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit
) {
    UiSettingRow(
        title = title,
        trailingContent = { UiSwitch(checked = enabled, onCheckedChange = onEnabledChange) },
    )
}

@Composable
private fun AppearancePickerRow(title: String, value: String, onClick: () -> Unit) {
    UiSettingRow(
        title = title,
        summary = value,
        trailingContent = {
            Icon(
                vectorResource(Res.drawable.arrow_drop_down_24px),
                contentDescription = null,
                tint = uiStyleColors().secondaryText,
            )
        },
        onClick = onClick,
    )
}

@Composable
private fun SectionHeader(text: String) {
    UiText(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = uiStyleColors().accent,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp),
    )
}
