package com.imcys.bilibilias.shared.platform.runtime

import androidx.compose.runtime.Composable

/**
 * 跳转网页
 */
expect fun openLink(url: String): Boolean

/**
 * 字符串模板
 */
expect fun format(format: String, vararg args: Any?): String

/**
 * 获取AppSignature
 */
@Composable
expect fun rememberAppSignature(): String?

/**
 * 获取App版本
 */
expect fun getAppVersion(): Pair<Long, String>

/**
 * 当前运行环境的 Android API Level（即 `Build.VERSION.SDK_INT`）。
 *
 * 非 Android 平台返回 0，调用方按「不支持」处理即可。
 * 用于在设置页按系统版本把能力项置灰（例如动态主题需要 API 31+，
 * 预测性返回手势需要 API 33+）。
 */
expect fun getAndroidApiLevel(): Int

/**
 * URL编码
 */
expect fun urlEncode(url: String): String

/**
 * URL解码
 */
expect fun urlDecode(url: String): String
