package com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.UiThread
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

/** 在应用自有文档中运行适配脚本，使用教务来源和共享的 WebView Cookie。 */
@UiThread
internal class XatuCourseScriptRunner(
    private val context: Context,
    private val onMessage: (String) -> Unit,
    private val onFailure: (String) -> Unit,
) : AutoCloseable {
    private var startedOrClosed = false
    private var webView: WebView? = null

    @SuppressLint("SetJavaScriptEnabled")
    fun start(script: String) {
        check(!startedOrClosed)
        startedOrClosed = true
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            onFailure("请更新系统 WebView 后再同步课表")
            return
        }

        val view = WebView(context)
        webView = view
        view.settings.apply {
            javaScriptEnabled = true
            allowFileAccess = false
            allowContentAccess = false
        }
        WebViewCompat.addWebMessageListener(view, BRIDGE_NAME, setOf(ORIGIN)) { sender, message, _, mainFrame, _ ->
            if (sender === webView && mainFrame && sender.url == DOCUMENT_URL && message.type == WebMessageCompat.TYPE_STRING) {
                message.data?.let(onMessage)
            }
        }
        view.webViewClient = object : WebViewClient() {
            private var pendingScript: String? = script

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = true

            override fun onPageFinished(view: WebView, url: String) {
                val source = pendingScript ?: return
                if (view !== webView) return
                pendingScript = null
                if (url != DOCUMENT_URL) {
                    fail("无法打开课表同步环境")
                    return
                }
                view.evaluateJavascript("typeof window.$BRIDGE_NAME?.postMessage === 'function'") { ready ->
                    if (view !== webView) return@evaluateJavascript
                    if (ready == "true") view.evaluateJavascript("$JS_BRIDGE_INIT\n$source", null)
                    else fail("课表同步组件未能启动")
                }
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (view === webView && request.isForMainFrame) fail("无法打开课表同步环境")
            }

            override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                if (view === webView) fail("课表同步组件已停止，请重试")
                return true
            }
        }
        view.loadDataWithBaseURL(DOCUMENT_URL, DOCUMENT, "text/html", null, DOCUMENT_URL)
    }

    fun evaluate(script: String, callback: ((String?) -> Unit)?) {
        val view = webView ?: return
        view.evaluateJavascript(script) { result ->
            if (view === webView) callback?.invoke(result)
        }
    }

    override fun close() {
        startedOrClosed = true
        val view = webView ?: return
        webView = null
        view.destroy()
    }

    private fun fail(message: String) {
        close()
        onFailure(message)
    }

    private companion object {
        const val ORIGIN = "http://jwgl2018.xatu.edu.cn"
        const val DOCUMENT_URL = "$ORIGIN/eams/homeExt.action"
        const val BRIDGE_NAME = "_shiguangNativeBridge"
        const val DOCUMENT = """<!doctype html><meta charset="utf-8"><meta http-equiv="Content-Security-Policy" content="default-src 'none'; connect-src 'self'; script-src 'unsafe-eval'; form-action 'none'; base-uri 'none'">"""
    }
}
