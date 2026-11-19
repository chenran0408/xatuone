package com.xingheyuzhuan.shiguangschedule.ui.settings.notification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import com.xingheyuzhuan.shiguangschedule.ui.components.UiScaffold
import com.xingheyuzhuan.shiguangschedule.ui.components.UiTopAppBar
import com.xingheyuzhuan.shiguangschedule.ui.components.uiStyleColors
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.title_course_notification_settings

@Composable
fun NotificationSettingsScreen(
    onBack: () -> Unit,
    viewModel: NotificationSettingsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    UiScaffold(
        containerColor = uiStyleColors().background,
        topBar = {
            UiTopAppBar(
                title = stringResource(Res.string.title_course_notification_settings),
                onBack = onBack,
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // 常规卡片由于各平台差异巨大，采用 expect 隔离由各平台自行实现
            item {
                PlatformGeneralSettingsSection(
                    uiState = uiState,
                    viewModel = viewModel
                )
            }
        }
    }

    // 平台专属的弹窗派发器也通过 expect 隔离
    PlatformNotificationDialogDispatcher(
        uiState = uiState,
        viewModel = viewModel
    )
}

/**
 * 平台专属的常规设置区域声明
 */
expect @Composable
fun PlatformGeneralSettingsSection(
    uiState: NotificationSettingsUiState,
    viewModel: NotificationSettingsViewModel
)

/**
 * 平台专属的弹窗派发器声明
 */
expect @Composable
fun PlatformNotificationDialogDispatcher(
    uiState: NotificationSettingsUiState,
    viewModel: NotificationSettingsViewModel
)
