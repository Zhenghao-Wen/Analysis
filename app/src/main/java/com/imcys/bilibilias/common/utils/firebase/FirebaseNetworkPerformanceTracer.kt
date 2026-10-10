package com.imcys.bilibilias.common.utils.firebase

import com.imcys.bilibilias.network.plugin.NetworkPerformanceTracer

/**
 * 网络埋点的无操作实现。
 *
 * 原本由 Firebase Performance 上报，现改为空实现：保留类名与接口实现，
 * 使 Koin 中 `NetworkPerformanceTracer` 的绑定链与调用路径保持原样，
 * 同时产物内不再包含任何 Firebase 代码。
 */
@Suppress("UNUSED_PARAMETER")
class FirebaseNetworkPerformanceTracer : NetworkPerformanceTracer {

    override fun onRequest(
        traceNamePrefix: String,
        method: String,
        path: String,
        requestPayloadSize: Long?,
    ) = Unit

    override fun recordSuccess(
        responseCode: Int,
        responsePayloadSize: Long?,
        responseContentType: String?,
    ) = Unit

    override fun recordFailure(error: Throwable?) = Unit
}
