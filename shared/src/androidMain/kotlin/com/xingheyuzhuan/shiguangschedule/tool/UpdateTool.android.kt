package com.xingheyuzhuan.shiguangschedule.tool

import android.content.Context
import android.app.DownloadManager
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.Build
import androidx.core.net.toUri
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

actual object PlatformUpdateStrategy : KoinComponent {
    actual val isUpdateSupported: Boolean = true

    private val SUPPORTED_ABIS = listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86", "universal")

    actual fun parseTargetUrl(response: ApiReleaseResponse): String? {

        val abiUrlMap = mutableMapOf<String, String>()

        // 1. 精准匹配
        for (asset in response.assets) {
            val fileName = asset.name.lowercase()
            if (!fileName.endsWith(".apk")) continue

            for (abi in SUPPORTED_ABIS) {
                if (fileName.contains("-$abi-") || fileName.endsWith("-$abi.apk")) {
                    abiUrlMap[abi] = asset.downloadUrl
                    break
                }
            }
        }

        // 2. 宽松匹配
        if (abiUrlMap.isEmpty()) {
            for (asset in response.assets) {
                val fileName = asset.name.lowercase()
                if (!fileName.endsWith(".apk")) continue

                for (abi in SUPPORTED_ABIS) {
                    if (fileName.contains(abi)) {
                        abiUrlMap[abi] = asset.downloadUrl
                        break
                    }
                }
            }
        }

        return Build.SUPPORTED_ABIS.firstNotNullOfOrNull { abiUrlMap[it] }
            ?: abiUrlMap["universal"]
    }

    actual fun downloadApk(url: String): String? {
        val uri = Uri.parse(url)
        val releasePath = "/chenran0408/xatuone/releases/download/"
        if (uri.scheme != "https" || uri.host != "github.com" || !uri.path.orEmpty().startsWith(releasePath)) {
            return "下载地址不是官方 HTTPS 发布文件"
        }

        val fileName = uri.lastPathSegment?.substringAfterLast('/')
            ?.takeIf { it.endsWith(".apk", ignoreCase = true) && it != "." && it != ".." }
            ?: return "发布文件名无效"

        return try {
            val context: Context = get()
            val request = DownloadManager.Request(uri).apply {
                setTitle(fileName)
                setMimeType("application/vnd.android.package-archive")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            }
            context.getSystemService(DownloadManager::class.java).enqueue(request)
            null
        } catch (exception: Exception) {
            exception.message ?: exception::class.simpleName ?: "无法加入系统下载"
        }
    }

    actual fun openUrl(url: String) {
        try {
            val context: Context = get()
            val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
