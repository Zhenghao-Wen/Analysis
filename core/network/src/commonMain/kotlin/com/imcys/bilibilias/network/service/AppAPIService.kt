package com.imcys.bilibilias.network.service

import com.imcys.bilibilias.network.model.app.AppOldApplyRoamBean
import com.imcys.bilibilias.network.model.app.AppOldCommonBean
import com.imcys.bilibilias.network.model.app.AppOldDonateBean
import com.imcys.bilibilias.network.model.app.AppOldHomeBannerDataBean
import com.imcys.bilibilias.network.model.app.AppOldSoFreezeBean
import com.imcys.bilibilias.network.model.app.AppOldUpdateDataBean
import io.ktor.client.HttpClient

/**
 * 旧版自建后端（api.misakamoe.com）的客户端 —— **已停用**。
 *
 * 本仓库不再请求任何 imcys.com / misakamoe.com 的第三方网络服务，因此该服务的
 * 全部方法都改为直接返回 [Result.failure]，不再发起任何请求，也不再持有
 * HttpClient。
 *
 * 之所以保留类与方法签名（而不是删除该类）：调用方分布在 Home / User / Donate /
 * Roam / Analysis / Download 等多个模块，逐处删除会带来大范围的编译风险。
 * 保留签名后，所有调用方通过既有的 `onSuccess` 判断自动跳过对应流程，
 * 好处是**网络层面一刀切断**，且不会出现编译错误。
 *
 * 这些方法对应的功能现状：
 * - getAppOldBanner        首页轮播图数据源 → 已彻底移除（见 HomeScreen / 首页布局默认值）
 * - getAppOldUpdateInfo    旧版应用更新与公告 → 已停用。注意：本仓库当前没有
 *                          可用的应用内更新数据源，更新提示不会再出现
 *                          （GitHub 通道仅用于设置页展示提交信息，不是更新检查）
 * - getAppOldDonate        捐助二维码 → 已停用
 * - checkApplyRoam / applyRoam  漫游申请 → 已停用（漫游的域名替换插件也已移除）
 * - submitASDownloadData   下载数据上报 → 已停用（属于用户数据上报）
 * - freezeUpAllVideo / freezeUpVideo / checkVideoSoFreeze  视频冻结 → 已停用
 * - getAppOldBoostVideoInfo 旧版充电视频信息 → 已停用
 */
@Suppress("UNUSED_PARAMETER")
class AppAPIService(
    @Suppress("unused") val httpClient: HttpClient
) {

    private fun <T> disabled(): Result<T> =
        Result.failure(IllegalStateException("Imcys 后端服务已停用"))

    /**
     * 检查视频冻结
     */
    suspend fun checkVideoSoFreeze(bvid: String, mid: Long): Result<AppOldSoFreezeBean> = disabled()

    /**
     * 冻结UP主全部视频
     */
    suspend fun freezeUpAllVideo(mid: Long): Result<AppOldSoFreezeBean> = disabled()

    /**
     * 冻结UP主个别视频
     */
    suspend fun freezeUpVideo(mid: Long, bvid: String): Result<AppOldSoFreezeBean> = disabled()

    /**
     * 检测是否申请了漫游
     */
    suspend fun checkApplyRoam(mid: Long): Result<AppOldApplyRoamBean> = disabled()

    /**
     * 申请漫游
     */
    suspend fun applyRoam(mid: Long, reason: String): Result<AppOldApplyRoamBean> = disabled()

    /**
     * 请求捐款二维码
     */
    suspend fun getAppOldDonate(): Result<AppOldDonateBean> = disabled()

    /**
     * 请求首页 banner
     */
    suspend fun getAppOldBanner(): Result<AppOldHomeBannerDataBean> = disabled()

    suspend fun getAppOldUpdateInfo(version: String): Result<AppOldUpdateDataBean> = disabled()

    suspend fun submitASDownloadData(
        aid: Long,
        bvid: String,
        mid: Long,
        upName: String,
        tName: String,
        copy: Int,
        userName: String?,
        userId: Long?
    ): Result<Unit> = disabled()

    suspend fun getAppOldBoostVideoInfo(): Result<AppOldCommonBean> = disabled()
}
