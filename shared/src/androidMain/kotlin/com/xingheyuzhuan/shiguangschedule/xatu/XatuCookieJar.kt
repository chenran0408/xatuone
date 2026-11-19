package com.xingheyuzhuan.shiguangschedule.xatu

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/** CookieManager remains the single cookie store for native requests and WebView. */
internal class XatuCookieJar(
    private val read: (String) -> String?,
    private val write: (String, String) -> Unit,
    private val flush: () -> Unit
) : CookieJar {
    override fun loadForRequest(url: HttpUrl): List<Cookie> = read(url.toString())
        .orEmpty().split(';').mapNotNull { Cookie.parse(url, it.trim()) }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookies.forEach { write(url.toString(), it.toString()) }
        if (cookies.isNotEmpty()) flush()
    }
}
