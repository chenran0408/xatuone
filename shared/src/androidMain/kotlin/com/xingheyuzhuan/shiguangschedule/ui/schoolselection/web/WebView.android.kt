package com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web

import android.annotation.SuppressLint
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.http.SslError
import android.view.ViewGroup
import android.webkit.GeolocationPermissions
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.xingheyuzhuan.shiguangschedule.data.repository.XatuCampusRepository
import com.xingheyuzhuan.shiguangschedule.xatu.campus.XatuLocation
import com.xingheyuzhuan.shiguangschedule.xatu.campus.XatuCoordinate
import org.koin.compose.koinInject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

actual val isDesktopPlatform: Boolean = false

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}

/**
 * Android 端的 WebViewController 实现
 */
class AndroidWebViewController : WebViewController {
    var webViewInstance: WebView? = null
    private var nativeBridge: NativeBridge? = null
    private var onProgressChange: (Float) -> Unit = {}
    private var onTitleChange: (String) -> Unit = {}
    private var onSslError: (String, () -> Unit, () -> Unit) -> Unit = { _, _, cancel -> cancel() }
    private var checkinLocationProvider: () -> String = { "{\"mode\":\"native\"}" }

    fun updateBridgeHandler(handler: WebBridgeHandler) {
        nativeBridge?.handler = handler
    }

    fun updateCheckinLocationProvider(provider: () -> String) {
        checkinLocationProvider = provider
    }

    fun checkinLocation(): String = checkinLocationProvider()

    fun isCheckinPage(): Boolean = currentUrl.startsWith("https://ehall.xatu.edu.cn/xsfw/sys/swmqdzsapp/")

    fun updateCallbacks(
        onProgressChange: (Float) -> Unit,
        onTitleChange: (String) -> Unit,
        onSslError: (String, () -> Unit, () -> Unit) -> Unit
    ) {
        this.onProgressChange = onProgressChange
        this.onTitleChange = onTitleChange
        this.onSslError = onSslError
    }

    fun setNativeBridge(bridge: NativeBridge) {
        nativeBridge = bridge
    }

    fun reportProgress(value: Float) = onProgressChange(value)
    fun reportTitle(value: String) = onTitleChange(value)
    fun reportSslError(url: String, proceed: () -> Unit, cancel: () -> Unit) = onSslError(url, proceed, cancel)

    companion object {
        val shared = AndroidWebViewController()
    }

    override val currentUrl: String
        get() = webViewInstance?.url ?: ""

    override fun reload() {
        webViewInstance?.reload()
    }

    override fun goBack(): Boolean {
        return if (webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
            true
        } else {
            false
        }
    }

    override fun canGoBack(): Boolean {
        return webViewInstance?.canGoBack() == true
    }

    override fun setDevToolsEnabled(enabled: Boolean) {
        WebView.setWebContentsDebuggingEnabled(enabled)
    }

    override fun clearSession() {
        webViewInstance?.let { wv ->
            wv.clearCache(true)
            wv.clearHistory()
            wv.clearFormData()
            wv.clearSslPreferences()
        }
        android.webkit.CookieManager.getInstance().removeAllCookies {
            android.webkit.CookieManager.getInstance().flush()
        }
    }

    override fun persistSession() {
        android.webkit.CookieManager.getInstance().flush()
    }

    override fun executeScript(jsCode: String) {
        evaluateJavascript(jsCode, null)
    }

    override fun evaluateJavascript(script: String, callback: ((String?) -> Unit)?) {
        webViewInstance?.post {
            val finalScript = if (script.contains("_shiguangBridgeInjected")) {
                script
            } else {
                """
                $JS_BRIDGE_INIT
                $script
                """.trimIndent()
            }

            webViewInstance?.evaluateJavascript(finalScript) { res ->
                callback?.invoke(res)
            }
        }
    }
}

@Composable
actual fun rememberWebViewController(): WebViewController {
    return remember { AndroidWebViewController.shared }
}

class NativeBridge(var handler: WebBridgeHandler) {
    @JavascriptInterface
    @Suppress("Unused")
    fun postMessage(jsonMessage: String) {
        handler.onMessageReceived(jsonMessage)
    }
}

private class CheckinLocationBridge(private val controller: AndroidWebViewController) {
    @JavascriptInterface
    fun getLocation(): String = if (controller.isCheckinPage()) controller.checkinLocation() else "{\"mode\":\"native\"}"
}

private const val CHECKIN_LOCATION_SCRIPT = """
(function(){
  if(window.__xatuCheckinLocationInstalled)return;
  var b=window.XatuCheckinLocation;if(!b)return;
  window.__xatuCheckinLocationInstalled=true;
  var geo=navigator.geolocation;if(!geo)return;
  var nativeGet=geo.getCurrentPosition.bind(geo), nativeWatch=geo.watchPosition.bind(geo), nativeClear=geo.clearWatch.bind(geo);
  function custom(ok,fail,opts){
    var raw=b.getLocation(), v; try{v=JSON.parse(raw)}catch(e){v={mode:'native'}}
    if(v.mode==='custom'){ok({coords:{latitude:v.latitude,longitude:v.longitude,accuracy:10,altitude:null,altitudeAccuracy:null,heading:null,speed:null},timestamp:Date.now()});return true}
    if(v.mode==='error'){if(fail)fail({code:2,message:v.message||'签到位置无效',PERMISSION_DENIED:1,POSITION_UNAVAILABLE:2,TIMEOUT:3});return true}
    return false;
  }
  geo.getCurrentPosition=function(ok,fail,opts){if(!custom(ok,fail,opts))nativeGet(ok,fail,opts)};
  geo.watchPosition=function(ok,fail,opts){if(custom(ok,fail,opts))return 0;return nativeWatch(ok,fail,opts)};
  geo.clearWatch=function(id){if(id)nativeClear(id)};
})();
"""

@SuppressLint("JavascriptInterface", "SetJavaScriptEnabled")
@Composable
actual fun PlatformWebView(
    modifier: Modifier,
    url: String,
    isDesktopMode: Boolean,
    isDevToolsEnabled: Boolean,
    controller: WebViewController,
    bridgeHandler: WebBridgeHandler,
    onProgressChange: (Float) -> Unit,
    onTitleChange: (String) -> Unit,
    onNavigateToSchedule: () -> Unit,
    onSslError: (failingUrl: String, onProceed: () -> Unit, onCancel: () -> Unit) -> Unit
) {
    val androidController = controller as AndroidWebViewController
    val campusRepository: XatuCampusRepository = koinInject()
    val campusSnapshot by campusRepository.snapshot.collectAsState(initial = com.xingheyuzhuan.shiguangschedule.data.repository.XatuCampusSnapshot())
    androidController.updateBridgeHandler(bridgeHandler)
    androidController.updateCallbacks(onProgressChange, onTitleChange, onSslError)
    androidController.updateCheckinLocationProvider {
        val location = campusSnapshot.checkinLocation
        if (!location.enabled) "{\"mode\":\"native\"}"
        else {
            val point = XatuLocation.toWgs84(XatuCoordinate(location.latitude, location.longitude, location.system))
            buildJsonObject {
                put("mode", "custom")
                put("latitude", point.latitude)
                put("longitude", point.longitude)
            }.toString()
        }
    }

    var loadedBaseUrl by remember { mutableStateOf("") }
    var isFirstLaunch by remember { mutableStateOf(true) }
    var pendingLocationOrigin by remember { mutableStateOf<String?>(null) }
    var pendingLocationCallback by remember { mutableStateOf<GeolocationPermissions.Callback?>(null) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        pendingLocationCallback?.invoke(pendingLocationOrigin, granted, false)
        pendingLocationOrigin = null
        pendingLocationCallback = null
    }

    // 监听外部强制 URL 变更
    LaunchedEffect(url) {
        if (url.isNotBlank() && url != "about:blank" && url != loadedBaseUrl) {
            loadedBaseUrl = url
            androidController.webViewInstance?.let { wv ->
                if (wv.url != url) {
                    wv.loadUrl(url)
                }
            }
        }
    }

    // 监听 桌面/移动 模式动态切换
    LaunchedEffect(isDesktopMode) {
        if (isFirstLaunch) {
            isFirstLaunch = false
            return@LaunchedEffect
        }

        androidController.webViewInstance?.let { wv ->
            val delegate = WebCompatDelegate(wv)
            delegate.enhanceSettings(isDesktopMode)
            if (isDesktopMode) {
                wv.setInitialScale(100)
            } else {
                wv.setInitialScale(0)
            }

            val currentRealUrl = wv.url?.takeIf { it.isNotBlank() && it != "about:blank" } ?: url
            if (currentRealUrl.isNotBlank() && currentRealUrl != "about:blank") {
                wv.loadUrl(currentRealUrl)
            }
        }
    }

    // 监听开发者工具开关
    LaunchedEffect(isDevToolsEnabled) {
        WebView.setWebContentsDebuggingEnabled(isDevToolsEnabled)
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                val existingWebView = androidController.webViewInstance
                (existingWebView ?: WebView(context)).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    if (isDesktopMode) {
                        setInitialScale(100)
                    } else {
                        setInitialScale(0)
                    }

                    androidController.webViewInstance = this

                    val delegate = WebCompatDelegate(this)
                    delegate.enhanceSettings(isDesktopMode)

                    if (existingWebView == null) {
                        val bridge = NativeBridge(bridgeHandler)
                        androidController.setNativeBridge(bridge)
                        addJavascriptInterface(bridge, "_shiguangNativeBridge")
                        addJavascriptInterface(CheckinLocationBridge(androidController), "XatuCheckinLocation")
                    }

                    val baseChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            androidController.reportProgress(newProgress / 100f)
                        }

                        override fun onReceivedTitle(view: WebView?, title: String?) {
                            if (!title.isNullOrBlank() && !title.startsWith("http")) {
                                androidController.reportTitle(title)
                            }
                        }

                        override fun onGeolocationPermissionsShowPrompt(
                            origin: String?,
                            callback: GeolocationPermissions.Callback?
                        ) {
                            val page = androidController.webViewInstance?.url.orEmpty()
                            val allowed = origin == "https://ehall.xatu.edu.cn" &&
                                page.startsWith("https://ehall.xatu.edu.cn/xsfw/sys/swmqdzsapp/")
                            if (!allowed || callback == null) {
                                callback?.invoke(origin, false, false)
                                return
                            }
                            val context = androidController.webViewInstance?.context ?: return
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.ACCESS_COARSE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                                context, Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPermission) {
                                callback.invoke(origin, true, false)
                            } else {
                                pendingLocationOrigin = origin
                                pendingLocationCallback = callback
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    )
                                )
                            }
                        }
                    }

                    val baseViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            androidController.reportProgress(0.1f)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            androidController.reportProgress(1.0f)
                            if (url?.startsWith("https://ehall.xatu.edu.cn/xsfw/sys/swmqdzsapp/") == true) {
                                view?.evaluateJavascript(CHECKIN_LOCATION_SCRIPT, null)
                            }
                            view?.evaluateJavascript("document.title") { value ->
                                val unquoted = value?.trim('"')?.replace("\\\"", "\"")?.trim() ?: ""
                                if (unquoted.isNotBlank() && unquoted != "null") {
                                    androidController.reportTitle(unquoted)
                                }
                            }
                        }

                        override fun onReceivedSslError(
                            view: WebView?,
                            handler: SslErrorHandler?,
                            error: SslError?
                        ) {
                            val failingUrl = error?.url ?: view?.url ?: ""
                            androidController.reportSslError(
                                failingUrl,
                                { handler?.proceed() },
                                { handler?.cancel() }
                            )
                        }
                    }

                    webChromeClient = delegate.wrapWebChromeClient(baseChromeClient) { progressInt ->
                        androidController.reportProgress(progressInt.toFloat())
                    }

                    webViewClient = delegate.wrapWebViewClient(baseViewClient)

                    if (url.isNotBlank() && url != "about:blank") {
                        loadedBaseUrl = url
                        loadUrl(url)
                    }
                }
            },
            update = { webView ->
            androidController.webViewInstance = webView
            androidController.updateBridgeHandler(bridgeHandler)
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            (androidController.webViewInstance?.parent as? ViewGroup)?.removeView(androidController.webViewInstance)

            android.webkit.CookieManager.getInstance().flush()
        }
    }
}
