package com.imcys.bilibilias.shared.feature.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.imcys.bilibilias.common.data.CommonBuildConfig
import com.imcys.bilibilias.common.event.sendToastEvent
import com.imcys.bilibilias.data.model.BILILoginUserModel
import com.imcys.bilibilias.data.repository.AppSettingsRepository
import com.imcys.bilibilias.data.repository.withoutRemovedHomeLayoutTypes
import com.imcys.bilibilias.data.repository.QRCodeLoginRepository
import com.imcys.bilibilias.data.repository.RiskManagementRepository
import com.imcys.bilibilias.database.entity.BILIUsersEntity
import com.imcys.bilibilias.datastore.*
import com.imcys.bilibilias.datastore.AppSettingsSerializer
import com.imcys.bilibilias.datastore.source.UsersDataSource
import com.imcys.bilibilias.shared.download.runtime.SharedDownloadManager
import com.imcys.bilibilias.network.ApiStatus
import com.imcys.bilibilias.network.NetWorkResult
import com.imcys.bilibilias.network.emptyNetWorkResult
import com.imcys.bilibilias.network.model.app.AppUpdateConfigInfo
import com.imcys.bilibilias.network.model.app.BannerConfigInfo
import com.imcys.bilibilias.network.model.app.BulletinConfigInfo
import com.imcys.bilibilias.network.service.AppAPIService
import com.imcys.bilibilias.shared.platform.runtime.getAppVersion
import bilibilias.shared.ASBuildConfig
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

class HomeViewModel(
    private val httpClient: HttpClient,
    private val qrCodeLoginRepository: QRCodeLoginRepository,
    private val usersDataSource: UsersDataSource,
    private val riskManagementRepository: RiskManagementRepository,
    private val downloadManager: SharedDownloadManager,
    private val appSettingsRepository: AppSettingsRepository,
    private val appAPIService: AppAPIService

) : ViewModel() {
    companion object {
        val UNKNOWN_APP_SIGN_WARNING_HIDE_DURATION_MS = 30.days
    }

    @Immutable
    data class UIState(
        val fromLoginEventConsumed: Boolean = false,
        val shownAppUpdate: Boolean = false,
        val fetchedAppUpdateInfo: Boolean = false,
        val shownPackageSourceWarningThisLaunch: Boolean = false
    )

    val appSettings = appSettingsRepository.appSettingsFlow

    val appSettingsState = appSettingsRepository.appSettingsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AppSettings.getDefaultInstance()
    )

    private var _uiState = MutableStateFlow(UIState())
    val uiState = _uiState.asStateFlow()

    private val _loginUserInfoState =
        MutableStateFlow<NetWorkResult<BILILoginUserModel?>>(emptyNetWorkResult())
    val loginUserInfoState = _loginUserInfoState.asStateFlow()

    private val _userLoginPlatformList =
        MutableStateFlow<List<BILIUsersEntity>>(emptyList())
    val userLoginPlatformList = _userLoginPlatformList.asStateFlow()

    val downloadListState = downloadManager.getAllDownloadTasks()


    private val _homeLayoutTypesetList = MutableStateFlow(emptyList<AppSettings.HomeLayoutItem>())

    var homeLayoutTypesetList = _homeLayoutTypesetList.asStateFlow()

    private val _bannerList = MutableStateFlow<List<BannerConfigInfo>>(emptyList())
    val bannerList = _bannerList.asStateFlow()


    private val _bulletinInfo = MutableStateFlow<BulletinConfigInfo?>(null)
    val bulletinInfo = _bulletinInfo.asStateFlow()

    private val _appUpdateInfo = MutableStateFlow<AppUpdateConfigInfo?>(null)
    val appUpdateInfo = _appUpdateInfo.asStateFlow()

    private val _useToolHistoryList =
        MutableStateFlow(toToolInfoList(AppSettingsSerializer.appSettingsDefault.useToolHistoryList))
    val useToolHistoryList = _useToolHistoryList.asStateFlow()

    // 使用工具历史列表
    init {
        initLayoutTypeset()
        initDownloadList()
        // requestSSEEvent()
        monitorUserState()
        updateWebSpi()
    }

    /**
     * 监听用户登录状态变化
     */
    private fun monitorUserState() {
        viewModelScope.launch(Dispatchers.IO) {
            usersDataSource.users.collect {
                if (it.currentUserId == 0L) {
                    _loginUserInfoState.value = emptyNetWorkResult()
                    _userLoginPlatformList.value = emptyList()
                } else {
                    showBILIUserInfo()
                }
            }
        }
    }

    fun initOldAppInfo() {
        // 已停用：首页轮播图 / 旧版更新信息 / 公告原本来自 api.misakamoe.com。
        // 本仓库不再请求任何 imcys.com / misakamoe.com 的第三方网络服务，
        // 因此这里不再发起请求，bannerList / appUpdateInfo / bulletinInfo 恒为空。
        // 对应的 UI（首页轮播图、公告、旧版更新提示）已在 HomeScreen 中一并移除。
        if (_uiState.value.fetchedAppUpdateInfo) return
        _uiState.value = uiState.value.copy(fetchedAppUpdateInfo = true)
    }

    fun getPackageSourceWarningKey(): String {
        return "${getAppVersion().second}+${buildCommitHash()}"
    }

    /**
     * 打包时写入的源码提交哈希（7 位，小写）。
     *
     * 来源是构建期的 `git rev-parse --short=7 HEAD`，见 app / shared 的 build.gradle.kts。
     * 优先用 `ASBuildConfig`（编译期常量，随构建产物固化，不依赖初始化顺序），
     * 为空时回退到 `CommonBuildConfig`（由 Application.onCreate 写入）。
     *
     * 之所以只用前 7 位：让用户从源码仓库的提交列表里就能直接比对，无需读完整 40 位。
     */
    fun buildCommitHash(): String {
        val fromBuildConfig = ASBuildConfig.GIT_COMMIT_HASH
        val raw = fromBuildConfig.ifBlank { CommonBuildConfig.gitCommitHash }
        return raw.trim().lowercase().take(7)
    }

    /**
     * 校验用户输入的提交哈希是否与本安装包一致。
     *
     * 容错：允许带 `#` 前缀、带前后空格、任意大小写；只取前 7 位比较。
     */
    fun verifyBuildCommitHash(input: String): Boolean {
        val expected = buildCommitHash()
        if (expected.isBlank()) return false
        val normalized = input.trim().removePrefix("#").trim().lowercase()
        if (normalized.length < 7) return false
        return normalized.take(7) == expected
    }

    fun markPackageSourceWarningShownThisLaunch() {
        if (_uiState.value.shownPackageSourceWarningThisLaunch) return
        _uiState.value = uiState.value.copy(shownPackageSourceWarningThisLaunch = true)
    }

    /**
     * 是否需要弹出安装包来源校验。
     *
     * 语义：**每个构建只弹一次**——记录里保存「版本号 + 提交哈希」，
     * 只有当它与当前安装包不一致（升级到新版本 / 换另一个提交构建）或为空（全新安装）
     * 时才需要校验；同一构建校验通过后不再打扰。
     *
     * 这里**直接读一次持久化值**，而不是用界面里 `collectAsStateWithLifecycle` 的
     * StateFlow：后者首帧是 `AppSettings.getDefaultInstance()`（skipKey 为空），
     * 会被误判为「未校验」，导致每次启动都弹窗。
     */
    suspend fun needsPackageSourceVerification(): Boolean {
        val savedKey = runCatching {
            appSettingsRepository.appSettingsFlow.first().packageSourceWarningSkipKey
        }.getOrDefault("")
        return savedKey != getPackageSourceWarningKey()
    }

    /**
     * 校验通过后记录「当前构建已确认来源」。
     *
     * 存的是 `版本号+提交哈希`，所以升级版本或换成另一个提交构建的包后，
     * 该值会变化 → 下次启动仍会重新要求校验（符合「更新后首次使用新版本」的要求）。
     */
    fun markPackageSourceVerifiedForCurrentBuild() {
        viewModelScope.launch(Dispatchers.IO) {
            appSettingsRepository.updatePackageSourceWarningSkipKey(getPackageSourceWarningKey())
        }
    }

    fun updateLastBulletinContent() {
        viewModelScope.launch {
            appSettingsRepository.updateLastBulletinContent(_bulletinInfo.value?.content ?: "")
        }
    }

    private fun toToolInfoList(toolNameList: List<String>): List<ToolInfo> {
        return toolNameList.mapNotNull { name ->
            runCatching {
                ToolInfo.valueOf(name)
            }.getOrNull()
        }.filter { info -> info.isScreen }
    }

    private fun initLayoutTypeset() {
        viewModelScope.launch {
            appSettingsRepository.asyncHomeLayoutTypesetList()
            appSettings.collect {
                // 过滤已永久停用的布局项（轮播图 / 公告 / 旧版更新），
                // 保证首页即使读到历史数据也不会渲染它们。
                _homeLayoutTypesetList.value =
                    it.homeLayoutTypesetList.withoutRemovedHomeLayoutTypes()
                _useToolHistoryList.value = toToolInfoList(it.useToolHistoryList)
            }

        }
    }

    private fun initDownloadList() {
        viewModelScope.launch(Dispatchers.IO) {
            downloadManager.initDownloadList()
        }
    }

    fun showBILIUserInfo() {
        viewModelScope.launch {
            initCurrentUserInfo()
        }
    }


    fun onNavigatedFromLogin() {
        viewModelScope.launch {
            if (loginUserInfoState.value.status != ApiStatus.SUCCESS) {
                showBILIUserInfo()
                _uiState.emit(uiState.value.copy(fromLoginEventConsumed = true))
            }
        }
    }


    fun updateWebSpi() {
        // 更新校验
        viewModelScope.launch(Dispatchers.IO) {
            riskManagementRepository.updateWebSpiCookie()
        }
    }

    /**
     * 暂停下载任务
     * [segmentId] 下载任务的ID
     */
    fun pauseDownloadTask(segmentId: Long) {
        viewModelScope.launch { downloadManager.pauseTask(segmentId) }
    }

    fun resumeDownloadTask(segmentId: Long) {
        viewModelScope.launch { downloadManager.resumeTask(segmentId) }
    }

    fun cancelDownloadTask(segmentId: Long) {
        viewModelScope.launch { downloadManager.cancelTask(segmentId) }
    }

    suspend fun initCurrentUserInfo() {
        if (!usersDataSource.isLogin()) return
        val biliUserList =
            qrCodeLoginRepository.getBILIUserListByUid(usersDataSource.getUserId())
        if (biliUserList.isEmpty()) return

        val currentUser = biliUserList.firstOrNull { it.id == usersDataSource.getUserId() }
            ?: biliUserList.firstOrNull()
            ?: return

        qrCodeLoginRepository.getLoginUserInfo(
            currentUser.loginPlatform,
            currentUser.accessToken
        )
            .collect {
                _loginUserInfoState.emit(it)

                // 更新本地记录
                when (it) {
                    is NetWorkResult.Success<*> -> {
                        biliUserList.forEach { user ->
                            qrCodeLoginRepository.updateBILIUser(
                                user.copy(
                                    mid = it.data?.mid ?: 0L,
                                    name = it.data?.name ?: "",
                                    face = it.data?.face ?: "",
                                    level = it.data?.level ?: 0,
                                    vipState = it.data?.vipState ?: 0,
                                )
                            )
                            _userLoginPlatformList.emit(biliUserList)
                        }
                    }

                    else -> {}
                }
            }


    }

    // 更新已经显示过
    fun onAppUpdateDialogShown() {
        viewModelScope.launch {
            _uiState.emit(uiState.value.copy(shownAppUpdate = true))
        }
    }

    // 更新使用工具记录
    fun updateUseToolRecord(tool: ToolInfo) {
        viewModelScope.launch {
            appSettingsRepository.updateUseToolRecord(tool.name)
        }
    }

    fun closeUnknownAppSignWarning() {
        viewModelScope.launch {
            appSettingsRepository.updateUnknownAppSignWarningCloseTime(
                Clock.System.now().toEpochMilliseconds()
            )
            sendToastEvent("一个月后将再次提醒您", "了解", onResult = {

            })
        }
    }


}
