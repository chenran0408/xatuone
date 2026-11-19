package com.xingheyuzhuan.shiguangschedule.xatu.campus

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.location.LocationRequest
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** One foreground request, cancelled when the requesting page leaves. Never changes system GPS. */
@SuppressLint("MissingPermission")
internal suspend fun currentXatuLocation(context: Context): Location {
    val manager = context.getSystemService(LocationManager::class.java)
    val precise = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    check(precise || coarse) { "请先允许定位权限" }
    check(manager.isLocationEnabled) { "系统定位未开启，请在系统设置中开启定位" }
    val provider = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER).firstOrNull {
        (it != LocationManager.GPS_PROVIDER || precise) && it in manager.allProviders && manager.isProviderEnabled(it)
    } ?: error("没有可用的定位服务，请检查系统定位或允许精确定位")
    return suspendCancellableCoroutine { continuation ->
        val cancellation = CancellationSignal()
        continuation.invokeOnCancellation { cancellation.cancel() }
        val request = LocationRequest.Builder(0L).setDurationMillis(20_000).build()
        manager.getCurrentLocation(provider, request, cancellation, context.mainExecutor) { location ->
            if (continuation.isActive) {
                if (location == null) continuation.resumeWithException(IllegalStateException("未获取到当前位置，请移至开阔处后重试"))
                else continuation.resume(location)
            }
        }
    }
}
