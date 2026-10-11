package com.imcys.bilibilias.shared.feature.tools.donate

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.imcys.bilibilias.network.model.app.AppOldDonateBean
import com.imcys.bilibilias.network.service.AppAPIService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 捐助页 ViewModel。
 *
 * 捐助二维码原本来自 api.misakamoe.com，该服务已整体停用，因此这里不再可能
 * 取得数据。保留 [loadFailed] 状态是为了让 UI 明确进入「未加载成功」分支，
 * 而不是永久停留在 shimmer 骨架屏（此前会一直闪动，且进度条会算出 0/0）。
 */
class DonateViewModel(
    appAPIService: AppAPIService
) : ViewModel() {

    @Immutable
    data class UIState(
        val oldDonateInfo: AppOldDonateBean? = null,
        /** 是否已确定无法取得捐助信息（后端已停用） */
        val loadFailed: Boolean = false
    )

    val uiState: StateFlow<UIState>
        field = MutableStateFlow(UIState())


    init {
        viewModelScope.launch(Dispatchers.IO) {
            appAPIService.getAppOldDonate()
                .onSuccess {
                    uiState.value = uiState.value.copy(oldDonateInfo = it, loadFailed = false)
                }
                .onFailure {
                    // Imcys 后端已停用，明确标记为不可用，避免 UI 一直处于加载骨架态
                    uiState.value = uiState.value.copy(oldDonateInfo = null, loadFailed = true)
                }
        }
    }


}
