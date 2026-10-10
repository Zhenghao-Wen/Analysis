package com.imcys.bilibilias.common.update

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.imcys.bilibilias.data.repository.AppSettingsRepository

/**
 * Google Play 应用内更新的无操作实现。
 *
 * 原实现依赖闭源 Play Core SDK。这里保留构造参数与全部被调用的方法签名，
 * 使 MainActivity 中的更新流程（含 `BuildConfig.ENABLED_PLAY_APP_MODE` 分支）
 * 可以原样保留而不引入任何商店 SDK。
 *
 * 所有方法均为空实现，`checkApp*Update()` 固定返回 false，即永远不触发更新。
 */
@Suppress("UNUSED_PARAMETER")
class GooglePlayAppUpdateManage(
    context: Context,
    appSettingsRepository: AppSettingsRepository
) : ASAppUpdateManage() {

    override suspend fun checkAppImmediateUpdate(): Boolean = false

    override suspend fun checkAppFlexibleUpdate(): Boolean = false

    suspend fun getUpdateVersion(): Int = 0

    fun startUpdate(
        activityResultLauncher: ActivityResultLauncher<IntentSenderRequest>,
        updateFinish: () -> Unit
    ) = Unit

    /** 完成已下载更新的安装；无商店 SDK 时为空实现。 */
    fun completeUpdate() = Unit
}
