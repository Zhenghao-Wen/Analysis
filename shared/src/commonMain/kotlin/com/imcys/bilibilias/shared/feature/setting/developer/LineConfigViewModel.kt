package com.imcys.bilibilias.shared.feature.setting.developer

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.imcys.bilibilias.data.repository.AppSettingsRepository
import com.imcys.bilibilias.data.repository.VideoInfoRepository
import com.imcys.bilibilias.datastore.*
import com.imcys.bilibilias.network.ApiStatus
import com.imcys.bilibilias.network.config.BROWSER_USER_AGENT
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.headers
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.encodedPath
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.time.Clock

@Immutable
data class LineConfigUIState(
    val currentLineHost: String = "",
)


data class BILILineHostItem(
    val name: String,
    val host: String,
    var speed: String? = null,
    val checkSpeeding: Boolean = false
)


/**
 * B站多线路列表
 * 参考：https://github.com/yujincheng08/BiliRoaming
 */
val biliLineHostList = listOf(
    BILILineHostItem("默认线路", ""),
    BILILineHostItem("ali（阿里）", "upos-sz-mirrorali.bilivideo.com"),
    BILILineHostItem("alib（阿里）", "upos-sz-mirroralib.bilivideo.com"),
    BILILineHostItem("alio1（阿里）", "upos-sz-mirroralio1.bilivideo.com"),
    BILILineHostItem("bos（百度）", "upos-sz-mirrorbos.bilivideo.com"),
    BILILineHostItem("cos（腾讯）", "upos-sz-mirrorcos.bilivideo.com"),
    BILILineHostItem("cosb（腾讯）", "upos-sz-mirrorcosb.bilivideo.com"),
    BILILineHostItem("coso1（腾讯）", "upos-sz-mirrorcoso1.bilivideo.com"),
    BILILineHostItem("hw（华为）", "upos-sz-mirrorhw.bilivideo.com"),
    BILILineHostItem("hwb（华为）", "upos-sz-mirrorhwb.bilivideo.com"),
    BILILineHostItem("hwo1（华为）", "upos-sz-mirrorhwo1.bilivideo.com"),
    BILILineHostItem("08c（华为）", "upos-sz-mirror08c.bilivideo.com"),
    BILILineHostItem("08h（华为）", "upos-sz-mirror08h.bilivideo.com"),
    BILILineHostItem("08ct（华为）", "upos-sz-mirror08ct.bilivideo.com"),
    BILILineHostItem("tf_hw（华为）", "upos-tf-all-hw.bilivideo.com"),
    BILILineHostItem("tf_tx（腾讯）", "upos-tf-all-tx.bilivideo.com"),
    BILILineHostItem("akamai（Akamai海外）", "upos-hz-mirrorakam.akamaized.net"),
    BILILineHostItem("aliov（阿里海外）", "upos-sz-mirroraliov.bilivideo.com"),
    BILILineHostItem("cosov（腾讯海外）", "upos-sz-mirrorcosov.bilivideo.com"),
    BILILineHostItem("hwov（华为海外）", "upos-sz-mirrorhwov.bilivideo.com"),
    BILILineHostItem("hk_bcache（Bilibili海外）", "cn-hk-eq-bcache-01.bilivideo.com")
)


class LineConfigViewModel(
    private val appSettingsRepository: AppSettingsRepository,
    private val videoInfoRepository: VideoInfoRepository,
    private val httpClient: HttpClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LineConfigUIState())
    val uiState = _uiState

    private val _biliLineHostList = MutableStateFlow(biliLineHostList)
    val biliLineHostListState = _biliLineHostList

    fun loadLineConfig() {
        viewModelScope.launch {
            appSettingsRepository.appSettingsFlow.collect {
                _uiState.value = _uiState.value.copy(
                    currentLineHost = it.biliLineHost
                )
            }
        }
    }

    fun updateLineHost(lineHost: String) {
        viewModelScope.launch {
            appSettingsRepository.updateLineHost(lineHost)
        }
    }

    fun startSpeedTest(lineHost: BILILineHostItem? = null) {
        viewModelScope.launch {

            if (lineHost != null) {
                setLineHost(lineHost.copy(speed = null, checkSpeeding = true))
            } else {
                biliLineHostList.forEach { line ->
                    setLineHost(line.copy(speed = null, checkSpeeding = true))
                }
            }
            videoInfoRepository.getVideoPlayerInfo(cid = 1622441837L, bvId = "BV1Cf421q78E")
                .collect {
                    if (it.status == ApiStatus.SUCCESS) {
                        biliLineHostList.run {
                            if (lineHost != null) listOf(lineHost) else this
                        }.filter { line -> line.host.isNotEmpty() }.forEach { line ->
                            val speed =
                                speedTest(
                                    line,
                                    it.data?.dash?.video?.firstOrNull()?.finalUrl ?: ""
                                )
                            setLineHost(
                                line.copy(
                                    speed = if (speed > 0) "$speed KB/s" else "测速失败",
                                    checkSpeeding = false
                                )
                            )
                        }
                    }
                }
        }
    }

    fun setLineHost(lineHost: BILILineHostItem) {
        val index = _biliLineHostList.value.indexOfFirst { item -> item.host == lineHost.host }
        if (index != -1) {
            val updatedList = _biliLineHostList.value.toMutableList()
            updatedList[index] = lineHost
            _biliLineHostList.value = updatedList
        }
    }

    private suspend fun speedTest(lineHost: BILILineHostItem, rawUrl: String) = try {
        val platformType = appSettingsRepository.getVideoParsePlatform()

        withContext(Dispatchers.IO) {
            val upos = lineHost.host
            val testUrl = if (upos == "\$1") {
                rawUrl
            } else {
                val originalUrl = Url(rawUrl)
                URLBuilder().apply {
                    protocol = originalUrl.protocol
                    host = upos
                    port = originalUrl.port
                    encodedPath = originalUrl.encodedPath
                    parameters.appendAll(originalUrl.parameters)
                    encodedFragment = originalUrl.encodedFragment
                }.buildString()
            }

            withTimeout(5000) {
                val startTime = Clock.System.now().toEpochMilliseconds()
                var totalBytes = 0L

                httpClient.prepareGet(testUrl) {
                    timeout {
                        connectTimeoutMillis = 5000
                        requestTimeoutMillis = 5000
                    }
                    headers {
                        append(
                            HttpHeaders.UserAgent, when (platformType) {
                                AppSettings.VideoParsePlatform.Mobile,
                                AppSettings.VideoParsePlatform.Web -> BROWSER_USER_AGENT

                                AppSettings.VideoParsePlatform.TV -> "Mozilla/5.0"
                            }
                        )
                        if (platformType != AppSettings.VideoParsePlatform.TV) {
                            append(
                                HttpHeaders.Referrer,
                                "https://www.bilibili.com/video/BV1Cf421q78E/"
                            )
                        }
                    }
                }.execute { response ->
                    val channel = response.bodyAsChannel()
                    val buffer = ByteArray(2048)

                    while (isActive) {
                        val read = channel.readAvailable(buffer, 0, buffer.size)
                        if (read <= 0) break
                        totalBytes += read
                    }
                }

                val elapsed = Clock.System.now().toEpochMilliseconds() - startTime
                if (elapsed > 0) totalBytes / elapsed else 0L
            }
        }
    } catch (e: Throwable) {
        0L
    }

}
