package com.xingheyuzhuan.shiguangschedule.data.di

import android.webkit.CookieManager
import okhttp3.OkHttpClient
import org.koin.core.annotation.Single

@Single
fun provideXatuCookieManager(): CookieManager = CookieManager.getInstance()

@Single
fun provideXatuHttpClient(): OkHttpClient = OkHttpClient.Builder()
    .followRedirects(true)
    .build()
