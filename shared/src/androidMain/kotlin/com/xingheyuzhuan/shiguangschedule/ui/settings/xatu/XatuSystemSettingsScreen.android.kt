package com.xingheyuzhuan.shiguangschedule.ui.settings.xatu

import android.app.usage.StorageStatsManager
import android.content.Intent
import android.provider.Settings
import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.ui.components.*
import com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web.AndroidWebViewController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

@Composable
actual fun XatuSystemSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var storage by remember { mutableStateOf("") }
    var refresh by remember { mutableStateOf(0) }
    var cacheRequested by remember { mutableStateOf(false) }
    LaunchedEffect(refresh) {
        storage = withContext(Dispatchers.IO) {
            try {
                val manager = context.getSystemService(StorageStatsManager::class.java)
                val stats = manager.queryStatsForUid(context.applicationInfo.storageUuid, context.applicationInfo.uid)
                val data = Formatter.formatShortFileSize(context, stats.dataBytes - stats.cacheBytes)
                val cache = Formatter.formatShortFileSize(context, stats.cacheBytes)
                "用户数据 $data · 缓存 $cache"
            } catch (error: IOException) {
                "无法读取存储占用"
            }
        }
    }
    XatuPage("网络与存储", onBack) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { XatuNetworkNotice() }
            item { UiActionButton(onClick = {
                context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            }, modifier = Modifier.fillMaxWidth(), text = "打开系统 Wi-Fi 设置") }
            item {
                UiServiceRow("存储占用", storage, { refresh++ })
            }
            item {
                UiActionButton(onClick = {
                    AndroidWebViewController.shared.clearResourceCache(context)
                    cacheRequested = true
                    refresh++
                }, modifier = Modifier.fillMaxWidth(), text = "清理网页缓存")
            }
            if (cacheRequested) item { UiFeedback("已请求清理网页缓存") }
        }
    }
}
