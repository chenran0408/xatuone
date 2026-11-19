package com.xingheyuzhuan.shiguangschedule.ui.settings.xatu

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.xingheyuzhuan.shiguangschedule.ui.components.UiText
import com.xingheyuzhuan.shiguangschedule.ui.components.uiStyleColors

@Composable
internal fun XatuNetworkNotice(compact: Boolean = false) {
    val context = LocalContext.current
    val manager = remember(context) { context.getSystemService(ConnectivityManager::class.java) }
    fun currentStatus(): String {
        val caps = manager.getNetworkCapabilities(manager.activeNetwork) ?: return "无网络连接"
        val transport = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "移动网络"
            else -> "网络"
        }
        return "$transport 已连接"
    }
    var status by remember { mutableStateOf(currentStatus()) }
    DisposableEffect(manager) {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { status = currentStatus() }
            override fun onLost(network: Network) { status = currentStatus() }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) { status = currentStatus() }
        }
        manager.registerDefaultNetworkCallback(callback, Handler(Looper.getMainLooper()))
        onDispose { manager.unregisterNetworkCallback(callback) }
    }
    UiText(status, style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = uiStyleColors().secondaryText)
}
