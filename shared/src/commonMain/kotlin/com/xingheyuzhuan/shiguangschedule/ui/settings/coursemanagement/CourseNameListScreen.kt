package com.xingheyuzhuan.shiguangschedule.ui.settings.coursemanagement

import com.xingheyuzhuan.shiguangschedule.ui.components.UiScaffold
import com.xingheyuzhuan.shiguangschedule.ui.components.UiTopAppBar

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.itos.coui.effect.couiButtonFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.Destination
import com.xingheyuzhuan.shiguangschedule.navigation.AddEditCourseChannel
import com.xingheyuzhuan.shiguangschedule.navigation.PresetCourseData
import com.xingheyuzhuan.shiguangschedule.ui.components.UiCourseIconButton
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.a11y_back
import shiguangschedule.shared.generated.resources.a11y_cancel_selection
import shiguangschedule.shared.generated.resources.a11y_delete
import shiguangschedule.shared.generated.resources.a11y_enter_selection_mode
import shiguangschedule.shared.generated.resources.a11y_exit_selection_mode
import shiguangschedule.shared.generated.resources.action_add
import shiguangschedule.shared.generated.resources.action_deselect_all
import shiguangschedule.shared.generated.resources.action_select_all
import shiguangschedule.shared.generated.resources.add_24px
import shiguangschedule.shared.generated.resources.arrow_back_24px
import shiguangschedule.shared.generated.resources.check_circle_24px
import shiguangschedule.shared.generated.resources.close_24px
import shiguangschedule.shared.generated.resources.delete_24px
import shiguangschedule.shared.generated.resources.done_all_24px
import shiguangschedule.shared.generated.resources.item_course_management
import shiguangschedule.shared.generated.resources.menu_open_24px
import shiguangschedule.shared.generated.resources.text_no_unique_courses_hint
import shiguangschedule.shared.generated.resources.title_selected_items_count
import com.xingheyuzhuan.shiguangschedule.data.model.UiStyle
import com.xingheyuzhuan.shiguangschedule.ui.components.LocalUiStyle

/**
 * 一级页面：展示所有不重复的课程名称列表 (Master View)。
 * 现使用两列网格 (LazyVerticalGrid)。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseNameListScreen(
    onNavigate: (Destination) -> Unit,
    onBack: () -> Unit,
    viewModel: CourseNameListViewModel = koinViewModel()
) {
    val uniqueCourseNames by viewModel.uniqueCourseNames.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedCourseNames = remember { mutableStateListOf<String>() }

    val exitSelectionMode: () -> Unit = {
        isSelectionMode = false
        selectedCourseNames.clear()
    }
    val onAddCourse: () -> Unit = {
        coroutineScope.launch {
            AddEditCourseChannel.sendEvent(PresetCourseData(startSection = 1, endSection = 2))
            onNavigate(Destination.AddEditCourse(courseId = null))
        }
    }
    val onCourseClick: (String) -> Unit = { clickedName ->
        if (isSelectionMode) {
            if (clickedName in selectedCourseNames) selectedCourseNames.remove(clickedName)
            else selectedCourseNames.add(clickedName)
        } else {
            onNavigate(Destination.CourseManagementDetail(courseName = clickedName))
        }
    }
    val onCourseLongClick: (String) -> Unit = { clickedName ->
        if (!isSelectionMode) {
            isSelectionMode = true
            selectedCourseNames.add(clickedName)
        }
    }

    UiScaffold(
        topBar = {
            UiTopAppBar(title = if (isSelectionMode) {
                            stringResource(Res.string.title_selected_items_count, selectedCourseNames.size)
                        } else {
                            stringResource(Res.string.item_course_management)
                        },
                navigationIcon = {
                    val icon = if (isSelectionMode) vectorResource(Res.drawable.close_24px) else vectorResource(Res.drawable.arrow_back_24px)
                    val description = if (isSelectionMode) {
                        stringResource(Res.string.a11y_cancel_selection)
                    } else {
                        stringResource(Res.string.a11y_back)
                    }
                    UiCourseIconButton(
                        icon = icon,
                        description = description,
                        inToolbar = true,
                        onClick = {
                            if (isSelectionMode) {
                                exitSelectionMode()
                            } else {
                                onBack()
                            }
                        }
                    )
                },
                actions = {
                    val totalCount = uniqueCourseNames.size
                    val selectedCount = selectedCourseNames.size
                    val isAllSelected = totalCount > 0 && selectedCount == totalCount

                    if (!isSelectionMode && LocalUiStyle.current == UiStyle.COUI) {
                        UiCourseIconButton(
                            icon = vectorResource(Res.drawable.add_24px),
                            description = stringResource(Res.string.action_add),
                            inToolbar = true,
                            onClick = onAddCourse,
                        )
                    }

                    if (isSelectionMode) {
                        UiCourseIconButton(
                            icon = vectorResource(Res.drawable.done_all_24px),
                            description = stringResource(
                                if (isAllSelected) Res.string.action_deselect_all else Res.string.action_select_all
                            ),
                            inToolbar = true,
                            enabled = totalCount > 0,
                            onClick = {
                                if (isAllSelected) {
                                    selectedCourseNames.clear()
                                } else {
                                    selectedCourseNames.clear()
                                    selectedCourseNames.addAll(uniqueCourseNames.map { it.name })
                                }
                            }
                        )
                    }

                    if (isSelectionMode) {
                        UiCourseIconButton(
                            icon = vectorResource(Res.drawable.delete_24px),
                            description = stringResource(Res.string.a11y_delete),
                            inToolbar = true,
                            enabled = selectedCourseNames.isNotEmpty(),
                            onClick = {
                                if (selectedCourseNames.isNotEmpty()) {
                                    coroutineScope.launch {
                                        viewModel.deleteSelectedCourses(selectedCourseNames)
                                        exitSelectionMode()
                                    }
                                }
                            }
                        )
                    }

                    UiCourseIconButton(
                        icon = vectorResource(Res.drawable.menu_open_24px),
                        description = stringResource(
                            if (isSelectionMode) Res.string.a11y_exit_selection_mode else Res.string.a11y_enter_selection_mode
                        ),
                        inToolbar = true,
                        onClick = {
                            if (isSelectionMode) {
                                exitSelectionMode()
                            } else {
                                isSelectionMode = true
                            }
                        }
                    )
                })
        },
        floatingActionButton = {
            if (!isSelectionMode && LocalUiStyle.current != UiStyle.COUI) {
                FloatingActionButton(onClick = onAddCourse) {
                    Icon(vectorResource(Res.drawable.add_24px), contentDescription = stringResource(Res.string.action_add))
                }
            }
        }
    ) { paddingValues ->
        if (uniqueCourseNames.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                if (LocalUiStyle.current == UiStyle.COUI) {
                    com.itos.coui.text.Text(
                        text = stringResource(Res.string.text_no_unique_courses_hint),
                        kind = com.itos.coui.text.CouiTextKind.Body,
                    )
                } else {
                    Text(stringResource(Res.string.text_no_unique_courses_hint), style = MaterialTheme.typography.bodyLarge)
                }
            }
        } else if (LocalUiStyle.current == UiStyle.COUI) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(uniqueCourseNames, key = { it.name }) { item ->
                    CourseNameCard(
                        name = item.name,
                        instanceCount = item.count,
                        isSelected = item.name in selectedCourseNames,
                        onCourseClick = onCourseClick,
                        onCourseLongClick = onCourseLongClick,
                    )
                }
            }
        } else {
            // LazyVerticalGrid 实现两列网格布局
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uniqueCourseNames, key = { it.name }) { item ->
                    val isSelected = item.name in selectedCourseNames

                    CourseNameCard(
                        name = item.name,
                        instanceCount = item.count,
                        isSelected = isSelected,
                        onCourseClick = { clickedName ->
                            if (isSelectionMode) {
                                if (selectedCourseNames.contains(clickedName)) {
                                    selectedCourseNames.remove(clickedName)
                                } else {
                                    selectedCourseNames.add(clickedName)
                                }
                            } else {
                                onNavigate(Destination.CourseManagementDetail(courseName = clickedName))
                            }
                        },
                        onCourseLongClick = { clickedName ->
                            if (!isSelectionMode) {
                                isSelectionMode = true
                                selectedCourseNames.add(clickedName)
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * 课程名称卡片 Composable
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CourseNameCard(
    name: String,
    instanceCount: Int,
    isSelected: Boolean,
    onCourseClick: (String) -> Unit,
    onCourseLongClick: (String) -> Unit
) {
    if (LocalUiStyle.current == UiStyle.COUI) {
        val colors = com.itos.coui.layout.CouiPageDefaults.themeColors()
        val interactionSource = remember { MutableInteractionSource() }
        com.itos.coui.layout.Item(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { selected = isSelected }
                .couiButtonFeedback(interactionSource = interactionSource, list = true)
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = { onCourseClick(name) },
                    onLongClick = { onCourseLongClick(name) },
                ),
            icon = if (isSelected) {
                {
                    Icon(
                        vectorResource(Res.drawable.check_circle_24px),
                        contentDescription = null,
                        tint = colors.accent,
                    )
                }
            } else null,
            title = {
                com.itos.coui.text.Text(
                    text = name,
                    kind = com.itos.coui.text.CouiTextKind.PreferenceTitle,
                )
            },
            summary = {
                com.itos.coui.text.Text(
                    text = instanceCount.toString(),
                    kind = com.itos.coui.text.CouiTextKind.PreferenceSummary,
                )
            },
        )
        return
    }

    if (LocalUiStyle.current == UiStyle.MIUIX) {
        val colors = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
        val cardColor = if (isSelected) colors.primaryContainer else colors.surfaceContainer
        val contentColor = if (isSelected) colors.onPrimaryContainer else colors.onSurfaceContainer
        top.yukonga.miuix.kmp.basic.Card(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 96.dp)
                .then(if (isSelected) Modifier.border(2.dp, colors.primary, RoundedCornerShape(16.dp)) else Modifier),
            colors = top.yukonga.miuix.kmp.basic.CardDefaults.defaultColors(
                color = cardColor,
                contentColor = contentColor,
            ),
            onClick = { onCourseClick(name) },
            onLongPress = { onCourseLongClick(name) },
            pressFeedbackType = top.yukonga.miuix.kmp.utils.PressFeedbackType.Sink,
        ) {
            Box(Modifier.fillMaxWidth().padding(12.dp)) {
                top.yukonga.miuix.kmp.basic.Text(
                    text = name,
                    modifier = Modifier.fillMaxWidth().padding(end = 36.dp, bottom = 16.dp),
                    style = top.yukonga.miuix.kmp.theme.MiuixTheme.textStyles.title4,
                    color = if (isSelected) contentColor else colors.primary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                top.yukonga.miuix.kmp.basic.Badge(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    containerColor = colors.primaryContainer,
                    contentColor = colors.onPrimaryContainer,
                ) {
                    top.yukonga.miuix.kmp.basic.Text(instanceCount.toString())
                }
            }
        }
        return
    }

    val cardColors = if (isSelected) {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    } else {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }

    val cardShape = MaterialTheme.shapes.medium

    Card(
        shape = cardShape,
        colors = cardColors,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 96.dp)
            .clip(cardShape)
            .combinedClickable(
                onClick = { onCourseClick(name) },
                onLongClick = { onCourseLongClick(name) }
            )
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, cardShape)
                } else Modifier
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // 课程名称和实例数量
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 32.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
            }

            // 右下角：实例数量 Badge
            Badge(
                content = { Text(instanceCount.toString(), style = MaterialTheme.typography.labelSmall) },
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .sizeIn(minWidth = 20.dp, minHeight = 20.dp)
            )
        }
    }
}
