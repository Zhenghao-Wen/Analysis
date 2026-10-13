package com.imcys.bilibilias.data.repository

import com.imcys.bilibilias.database.currentTimeMillis
import com.imcys.bilibilias.network.AsCookiesStorage
import com.imcys.bilibilias.network.NetWorkResult
import com.imcys.bilibilias.network.config.BILIBILI_URL
import com.imcys.bilibilias.network.config.BUVID3
import com.imcys.bilibilias.network.config.BUVID4
import com.imcys.bilibilias.network.service.BILIBILIWebAPIService
import io.ktor.client.plugins.cookies.addCookie
import io.ktor.http.Cookie
import io.ktor.util.date.GMTDate

class RiskManagementRepository(
    private val webApiService: BILIBILIWebAPIService,
    private val asCookiesStorage: AsCookiesStorage
) {

    /**
     * 更新签名（BUVID3 / BUVID4）。
     *
     * 判定以 **Cookie 存储的实际内容** 为准，不在实例或伴生对象里缓存返回值：
     * 登出会清空 Cookie 存储（`SettingViewModel.logout` → `clearCookies`），
     * 若用内存字段做缓存，清空后字段仍非空，守卫会短路导致 BUVID 无法重建，
     * 匿名请求因缺少 buvid 更易触发风控，且该状态会一直持续到进程重启。
     */
    suspend fun updateWebSpiCookie() {
        if (!isSpiCookieMissing()) return
        webApiService.getWebSpiInfo().collect {
            when (it) {
                is NetWorkResult.Success<*> -> {
                    val b3 = it.data?.b3
                    val b4 = it.data?.b4
                    if (b3 != null && b4 != null) {
                        asCookiesStorage.addCookie(
                            BILIBILI_URL, Cookie(
                                name = BUVID3,
                                value = b3,
                                expires = GMTDate(currentTimeMillis() + 86400 * 1000L),
                                path = "/"
                            )
                        )
                        asCookiesStorage.addCookie(
                            BILIBILI_URL, Cookie(
                                name = BUVID4,
                                value = b4,
                                expires = GMTDate(currentTimeMillis() + 86400 * 1000L),
                                path = "/"
                            )
                        )

                    }
                }

                else -> {}
            }
        }
    }

    /** BUVID3 / BUVID4 是否缺失（以 Cookie 存储为准）。 */
    private suspend fun isSpiCookieMissing(): Boolean =
        asCookiesStorage.getCookieValue(BUVID3).isNullOrEmpty() ||
            asCookiesStorage.getCookieValue(BUVID4).isNullOrEmpty()
}
