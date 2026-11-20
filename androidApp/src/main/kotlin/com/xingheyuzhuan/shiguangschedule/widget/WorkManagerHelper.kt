package com.xingheyuzhuan.shiguangschedule.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Process
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.xingheyuzhuan.shiguangschedule.data.model.AppSettingsModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import java.util.concurrent.TimeUnit

/**
 * 负责调度和取消小组件相关定期任务的通用帮助类。
 */
object WorkManagerHelper {

    private const val UI_UPDATE_WORK_NAME = "WidgetUiUpdateWorker_Periodic"
    private const val FULL_DATA_SYNC_WORK_NAME = "FullDataSyncWorker_Periodic"

    private val hasWidgets = MutableStateFlow(false)

    fun updateWidgetPresence(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        hasWidgets.value = manager.getInstalledProvidersForPackage(context.packageName, Process.myUserHandle())
            .any { manager.getAppWidgetIds(it.provider).isNotEmpty() }
    }

    fun monitor(context: Context, settings: Flow<AppSettingsModel>): Flow<Unit> {
        val needsAlarms = settings.map { it.reminderEnabled || it.autoModeEnabled }.distinctUntilChanged()
        return combine(hasWidgets, needsAlarms) { widgets, alarms ->
            val manager = WorkManager.getInstance(context)
            if (widgets) {
                manager.enqueueUniquePeriodicWork(
                    UI_UPDATE_WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    PeriodicWorkRequestBuilder<WidgetUiUpdateWorker>(15, TimeUnit.MINUTES).build()
                )
            } else {
                manager.cancelUniqueWork(UI_UPDATE_WORK_NAME)
            }
            if (widgets || alarms) {
                manager.enqueueUniquePeriodicWork(
                    FULL_DATA_SYNC_WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    PeriodicWorkRequestBuilder<FullDataSyncWorker>(1, TimeUnit.DAYS)
                        .setInitialDelay(1, TimeUnit.DAYS)
                        .build()
                )
            } else {
                manager.cancelUniqueWork(FULL_DATA_SYNC_WORK_NAME)
            }
            Unit
        }.onStart { updateWidgetPresence(context) }
    }
}
