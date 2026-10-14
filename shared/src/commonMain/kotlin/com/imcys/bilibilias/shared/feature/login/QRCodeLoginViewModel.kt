package com.imcys.bilibilias.shared.feature.login


import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.request.ImageRequest
import coil3.toBitmap
import com.imcys.bilibilias.data.model.BILILoginUserModel
import com.imcys.bilibilias.data.repository.QRCodeLoginRepository
import com.imcys.bilibilias.database.entity.BILIUserCookiesEntity
import com.imcys.bilibilias.database.entity.BILIUsersEntity
import com.imcys.bilibilias.database.entity.ASSharedCookieEncoding
import com.imcys.bilibilias.database.entity.LoginPlatform
import com.imcys.bilibilias.datastore.source.UsersDataSource
import com.imcys.bilibilias.network.AsCookiesStorage
import com.imcys.bilibilias.network.NetWorkResult
import com.imcys.bilibilias.network.emptyNetWorkResult
import com.imcys.bilibilias.network.model.QRCodeInfo
import com.imcys.bilibilias.network.model.QRCodePollInfo
import com.imcys.bilibilias.network.model.TvQRCodePollInfo
import io.ktor.http.Cookie
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.imcys.bilibilias.common.event.sendToastEvent
import com.imcys.bilibilias.common.event.sendToastEventOnBlocking
import com.imcys.bilibilias.data.repository.AppSettingsRepository
import com.imcys.bilibilias.data.repository.toDataStoreType
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readBytes
import io.ktor.client.statement.readRawBytes
import kotlinx.coroutines.IO


/**
 * 扫码登录二维码承载的回跳 deep link。
 *
 * 二维码内容 = B 站扫码登录地址 + 本后缀，扫码后由 B 站页面回跳本 App
 * 完成登录（对应 AndroidManifest 中 `bilibilias://app/loginFinish`）。
 * 屏幕显示与「保存二维码到相册」必须使用完全相同的字符串，否则扫出来无法回跳。
 */
const val QR_CODE_LOGIN_FINISH_DEEP_LINK = "bilibilias://app/loginFinish"


class QRCodeLoginViewModel(
    private val qrCodeLoginRepository: QRCodeLoginRepository,
    private val usersDataSource: UsersDataSource,
    private val asCookiesStorage: AsCookiesStorage,
    private val appSettingsRepository: AppSettingsRepository,
    private val httpClient: HttpClient
) : ViewModel() {

    data class UIState(
        var selectedLoginPlatform: LoginPlatform = LoginPlatform.WEB,
        var isLoginLoading: Boolean = false
    )

    private val currentCookies = mutableListOf<Cookie>()
    private var currentQrCodePollInfo: QRCodePollInfo? = null


    var uiState by mutableStateOf(UIState())
        private set

    private val _qrCodeInfoState =
        MutableStateFlow<NetWorkResult<QRCodeInfo?>>(emptyNetWorkResult())
    val qrCodeInfoState = _qrCodeInfoState.asStateFlow()

    private val _qrCodeScanInfoState =
        MutableStateFlow<NetWorkResult<QRCodePollInfo?>>(emptyNetWorkResult())
    val qrCodeScanInfoState = _qrCodeScanInfoState.asStateFlow()

    private val _loginUserInfoState =
        MutableStateFlow<NetWorkResult<BILILoginUserModel?>>(emptyNetWorkResult())

    val loginUserInfoState = _loginUserInfoState.asStateFlow()


    /**
     * 加载登录二维码
     */
    fun getLoadLoginQRCodeInfo() {
        viewModelScope.launch {
            qrCodeLoginRepository.getLoginQRCodeInfo(uiState.selectedLoginPlatform).collect {
                _qrCodeInfoState.emit(it)
                when (it) {
                    is NetWorkResult.Success<*> -> {
                        it.data?.let { data ->
                            checkScanQRState(data.qrcodeKey)
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    /**
     * 更新登录平台
     */
    fun updateLoginPlatform(loginPlatform: LoginPlatform) {
        uiState = uiState.copy(selectedLoginPlatform = loginPlatform)
        getLoadLoginQRCodeInfo()
    }


    /**
     * 获取登录信息
     */
    fun getLoginUserInfo(
        qrCodePollInfo: QRCodePollInfo? = null,
    ) {
        uiState = uiState.copy(isLoginLoading = true)
        viewModelScope.launch {
            currentQrCodePollInfo = qrCodePollInfo
            qrCodeLoginRepository.getLoginUserInfo(
                uiState.selectedLoginPlatform,
                qrCodePollInfo?.accessToken
            )
                .collect {
                    _loginUserInfoState.emit(it)
                }
        }
    }

    /**
     * 保存登录用户信息
     */
    fun saveLoginInfo(biliLoginUserModel: BILILoginUserModel?, onSaveFinish: () -> Unit) {
        if (biliLoginUserModel == null) return

        viewModelScope.launch(Dispatchers.IO) {
            if (biliLoginUserModel.mid == 0L) return@launch
            val oldUserInfo = qrCodeLoginRepository.getBILIUserByMidAndPlatform(
                biliLoginUserModel.mid!!,
                uiState.selectedLoginPlatform,
            )
            val newLoginUserInfo = BILIUsersEntity(
                name = biliLoginUserModel.name ?: "",
                mid = biliLoginUserModel.mid ?: 0L,
                face = biliLoginUserModel.face ?: "",
                level = biliLoginUserModel.level ?: 0,
                vipState = biliLoginUserModel.vipState ?: 0,
                loginPlatform = uiState.selectedLoginPlatform,
                accessToken = currentQrCodePollInfo?.accessToken,
                refreshToken = currentQrCodePollInfo?.refreshToken
            )

            val userId = if (oldUserInfo == null) {
                qrCodeLoginRepository.saveLoginInfo(newLoginUserInfo)
            } else {
                newLoginUserInfo.apply { newLoginUserInfo.id = oldUserInfo.id }
                qrCodeLoginRepository.updateBILIUser(newLoginUserInfo)
                newLoginUserInfo.id
            }

            // 新增Cookie存储
            qrCodeLoginRepository.deleteBILICookiesByUid(userId)

            // 新增
            currentCookies.forEach {
                val cookie = BILIUserCookiesEntity(
                    userId = userId,
                    name = it.name,
                    value = it.value,
                    path = it.path,
                    secure = it.secure,
                    domain = it.domain,
                    encoding = ASSharedCookieEncoding.valueOf(it.encoding.name),
                    httpOnly = it.httpOnly
                )
                qrCodeLoginRepository.insertBILIUserCookie(cookie)
            }

            usersDataSource.setUserId(userId)
            asCookiesStorage.syncDataBaseCookies()
            appSettingsRepository.updateVideoParsePlatform(uiState.selectedLoginPlatform.toDataStoreType())

            uiState = uiState.copy(isLoginLoading = false)


            launch(Dispatchers.Main) {
                onSaveFinish.invoke()
            }

        }
    }

    fun saveQRCodeImageToGallery() {
        // 传入的是「待编码内容」而非图片地址：二维码由本地生成，不再请求云端出图服务
        saveQRCodeImageToGalleryByPlatform(getQRCodeContentForScan())
    }

    fun goToScanQR() {
        goToScanQRByPlatform()
    }

    fun dispose() {
        checkScanQRJob?.cancel()
        checkScanQRJob = null
    }

    /**
     * 二维码实际承载的内容：B 站的扫码登录地址 + 回跳本 App 的 deep link。
     *
     * 原本该内容需要交给 pan.misakamoe.com 生成图片；现在改为本地编码，
     * 因此这里直接返回原始字符串，屏幕显示与保存到相册共用同一份内容。
     */
    fun getQRCodeContentForScan(): String? {
        val url = _qrCodeInfoState.value.data?.url ?: return null
        return url + QR_CODE_LOGIN_FINISH_DEEP_LINK
    }

    private var checkScanQRJob: Job? = null

    /**
     * 检测登录状态
     */
    private fun checkScanQRState(qrcodeKey: String) {

        var finish = false
        if (checkScanQRJob != null) {
            checkScanQRJob?.cancel()
            checkScanQRJob = null
        }

        checkScanQRJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive && !finish) {
                qrCodeLoginRepository.getQRScanState(uiState.selectedLoginPlatform, qrcodeKey)
                    .collect {
                        _qrCodeScanInfoState.emit(it)
                        when (it) {
                            is NetWorkResult.Success<*> -> {
                                handleHeaderCookie(it.responseData?.responseHeader)
                                handleResponseCookie(it.data?.cookieInfo)
                                if (it.data?.code == 0) {
                                    finish = true
                                }
                            }

                            else -> {}
                        }
                    }
                delay(3000L)
            }
        }
    }

    /**
     * 保存Cookie
     */
    private fun handleResponseCookie(cookieInfo: TvQRCodePollInfo.CookieInfo?) {
        if (cookieInfo == null) return
        viewModelScope.launch {
            currentCookies.clear()
            cookieInfo.cookies.forEach { cookie ->
                val cookieInfo = Cookie(
                    name = cookie.name,
                    value = cookie.value,
                    httpOnly = cookie.httpOnly == 1L,
                    secure = cookie.secure == 1L,
                    domain = "bilibili.com",
                    path = "/"
                )
                currentCookies.add(cookieInfo)
            }
        }

    }

    /**
     * 保存Cookie
     */
    private suspend fun handleHeaderCookie(responseHeader: Set<Map.Entry<String, List<String>>>?) {
        responseHeader?.forEach {
            if (it.key == "Set-Cookie") {
                currentCookies.clear()
                currentCookies.addAll(asCookiesStorage.getAllCookies())
            }
        }
    }


}
