package com.xingheyuzhuan.shiguangschedule.widget.compact

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import com.xingheyuzhuan.shiguangschedule.widget.WorkManagerHelper
import com.xingheyuzhuan.shiguangschedule.widget.updateAllWidgets
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * 今日课程（紧凑版）原生小组件接收器
 */
class CompactNativeProvider : AppWidgetProvider() {
    private val scope = MainScope()

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        scope.launch {
            // 触发全局刷新逻辑
            updateAllWidgets(context)
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WorkManagerHelper.updateWidgetPresence(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WorkManagerHelper.updateWidgetPresence(context)
    }
}
