package com.baidu.mobstat

import android.content.Context

/**
 * 百度统计 SDK 的无操作替代实现。
 *
 * 原实现来自闭源 jar（`Baidu_Mtj_android_*.jar`），由外部地址在构建期下载。
 * 为保证项目完全不依赖闭源产物、且构建过程不访问外部网络，
 * 这里以同名同签名的空实现接管；调用方（Application / MainActivity）无需改动。
 */
@Suppress("UNUSED_PARAMETER")
object StatService {

    @JvmStatic
    fun init(context: Context, appKey: String, channel: String) = Unit

    @JvmStatic
    fun setAuthorizedState(context: Context, authorized: Boolean) = Unit

    @JvmStatic
    fun start(context: Context) = Unit

    @JvmStatic
    fun onResume(context: Context) = Unit

    @JvmStatic
    fun onPause(context: Context) = Unit
}
