package com.xingheyuzhuan.shiguangschedule.ui.settings.xatu

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.ui.components.*

@Composable
actual fun XatuSystemSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    XatuPage("网络设置", onBack) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
                item { XatuNetworkNotice() }
                item { UiActionButton(onClick = {
                    context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                }, modifier = Modifier.fillMaxWidth(), text = "打开系统 Wi-Fi 设置") }
        }
    }
}
