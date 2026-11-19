package com.xingheyuzhuan.shiguangschedule.data.di

import android.webkit.CookieManager
import android.os.Handler
import android.os.Looper
import okhttp3.OkHttpClient
import com.xingheyuzhuan.shiguangschedule.xatu.XatuCookieJar
import org.koin.core.annotation.Single
import java.util.concurrent.CountDownLatch

@Single
fun provideXatuCookieManager(): CookieManager = CookieManager.getInstance().also {
    it.setAcceptCookie(true)
}

@Single
fun provideXatuHttpClient(cookieManager: CookieManager): OkHttpClient = OkHttpClient.Builder()
    .cookieJar(XatuCookieJar(cookieManager::getCookie, { url, value -> setCookieAndFlush(cookieManager, url, value) }, cookieManager::flush))
    .followRedirects(true)
    .build()

private fun setCookieAndFlush(cookieManager: CookieManager, url: String, value: String) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
        cookieManager.setCookie(url, value) { cookieManager.flush() }
        return
    }

    val completed = CountDownLatch(1)
    Handler(Looper.getMainLooper()).post {
        cookieManager.setCookie(url, value) {
            cookieManager.flush()
            completed.countDown()
        }
    }
    completed.await()
}
