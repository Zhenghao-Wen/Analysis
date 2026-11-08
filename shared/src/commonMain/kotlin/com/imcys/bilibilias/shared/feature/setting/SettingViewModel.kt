package com.imcys.bilibilias.shared.feature.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bilibilias.shared.ASBuildConfig
import com.imcys.bilibilias.data.model.github.GithubCodeVersionUpdateInfo
import com.imcys.bilibilias.data.repository.AppSettingsRepository
import com.imcys.bilibilias.data.repository.GithubInfoRepository
import com.imcys.bilibilias.data.repository.UserInfoRepository
import com.imcys.bilibilias.database.dao.BILIUserCookiesDao
import com.imcys.bilibilias.database.dao.BILIUsersDao
import com.imcys.bilibilias.datastore.*
import com.imcys.bilibilias.datastore.source.UsersDataSource
import com.imcys.bilibilias.network.AsCookiesStorage
import com.imcys.bilibilias.network.NetWorkResult
import com.imcys.bilibilias.network.emptyNetWorkResult
import com.imcys.bilibilias.shared.platform.firebase.FirebaseExt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.launch

class SettingViewModel(
    private val appSettingsRepository: AppSettingsRepository,
    private val usersDataSource: UsersDataSource,
    private val biliUsersDao: BILIUsersDao,
    private val biliUserCookiesDao: BILIUserCookiesDao,
    private val userInfoRepository: UserInfoRepository,
    private val asCookiesStorage: AsCookiesStorage,
    private val githubInfoRepository: GithubInfoRepository
) : ViewModel() {

    val appSettings = appSettingsRepository.appSettingsFlow

    private val _uiState = MutableStateFlow(SettingUIState())
    val uiState = _uiState.asStateFlow()

    val lastGitCommitInfo: StateFlow<NetWorkResult<GithubCodeVersionUpdateInfo>>
        field = MutableStateFlow<NetWorkResult<GithubCodeVersionUpdateInfo>>(emptyNetWorkResult())

    init {
        viewModelScope.launch {
            usersDataSource.users.collect {
                var mid = 0L
                if (it.currentUserId != 0L) {
                    val user = biliUsersDao.getBILIUserByUid(it.currentUserId)
                    mid = user?.mid ?: 0L
                }
                _uiState.value = _uiState.value.copy(
                    isLogin = it.currentUserId != 0L,
                    currentMid = mid
                )
            }
        }
        loadGitInfo()
    }

    private fun loadGitInfo() {
        viewModelScope.launch {
            githubInfoRepository.getLastCommitInfo(
                ASBuildConfig.GITHUB_ORG,
                ASBuildConfig.GITHUB_REPOSITORY
            ).collect {
                lastGitCommitInfo.value = it
            }
        }
    }

    fun updatePrivacyPolicyAgreement(agreed: AppSettings.AgreePrivacyPolicyState) {
        viewModelScope.launch {
            appSettingsRepository.updatePrivacyPolicyAgreement(agreed)
            FirebaseExt.setDataCollectionEnabled(
                agreed == AppSettings.AgreePrivacyPolicyState.Agreed
            )
        }
    }

    fun updateEnabledDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            appSettingsRepository.updateEnabledDynamicColor(enabled)
        }
    }

    fun updateEnabledOnBackInvokedCallback(enabled: Boolean){
        viewModelScope.launch {
            appSettingsRepository.updateEnabledOnBackInvokedCallback(enabled)
        }
    }

    fun updateEnabledNavAnimation(enabled: Boolean){
        viewModelScope.launch {
            appSettingsRepository.updateEnabledNavAnimation(enabled)
        }
    }
    fun updateClipboardAutoHandling(enabled: Boolean) {
        viewModelScope.launch {
            appSettingsRepository.updateClipboardAutoHandling(enabled)
        }
    }

    suspend fun logout() {
        val currentUserId = usersDataSource.getUserId()
        val user = biliUsersDao.getBILIUserByUid(currentUserId)
        val cookies = user?.let { biliUserCookiesDao.getBILIUserCookiesByUid(it.id) }.orEmpty()
        val biliJct = cookies.firstOrNull { cookie -> cookie.name == "bili_jct" }?.value

        // 先做「本地登出」，且不依赖是否拿得到 bili_jct：
        // 之前只有当 bili_jct 存在时才置空 currentUserId（否则直接 return），
        // 没拿到该 cookie 时整段逻辑被跳过，界面会一直显示已登录。
        usersDataSource.setUserId(0L)
        user?.let {
            biliUserCookiesDao.deleteBILICookiesByUid(it.id)
            biliUsersDao.deleteBILIUserByUid(it.id)
        }
        // 立即更新本页状态（其它页面通过 usersDataSource.users 流同步刷新）
        _uiState.value = _uiState.value.copy(
            isLogin = false,
            currentMid = 0
        )
        asCookiesStorage.clearCookies()
        asCookiesStorage.syncDataBaseCookies()

        // 最后尽力通知服务端登出：网络失败不影响「本地已登出」这个结果，
        // 否则异常会向上冒泡，导致界面卡在「正在退出登录」。
        if (!biliJct.isNullOrBlank()) {
            runCatching { userInfoRepository.logout(biliJct).last() }
        }
    }

}
