package com.imcys.bilibilias.shared.feature.setting.storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.imcys.bilibilias.shared.util.StorageInfoData
import com.imcys.bilibilias.data.repository.AppSettingsRepository
import com.imcys.bilibilias.shared.platform.storage.StoragePlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** 存储信息统计的超时时间，避免 SAF 遍历过慢时界面卡死。 */
private const val STORAGE_INFO_TIMEOUT_MS = 10_000L

class StorageManagementViewModel(
    private val appSettingsRepository: AppSettingsRepository
) : ViewModel() {

    sealed interface StorageManagementUIState {
        object Loading : StorageManagementUIState
        data class Success(
            val storageInfoData: StorageInfoData,
            val hasDownloadSAFPermission: Boolean
        ) : StorageManagementUIState

        data class Error(val errorMsg: String) : StorageManagementUIState
    }

    private val _uiState =
        MutableStateFlow<StorageManagementUIState>(StorageManagementUIState.Loading)

    val uiState = _uiState.asStateFlow()

    fun loadStorageInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            // 统计本身可能很慢（SAF 目录遍历），加超时兜底：
            // 超时后给出可重试的错误状态，避免界面永远停在「加载中」。
            val storageInfoData = withTimeoutOrNull(STORAGE_INFO_TIMEOUT_MS) {
                runCatching { StoragePlatform.getStorageInfoData() }.getOrNull()
            }
            if (storageInfoData == null) {
                _uiState.emit(
                    StorageManagementUIState.Error("统计存储占用超时或被中断，请重试")
                )
                return@launch
            }
            val hasDownloadSAFPermission = runCatching {
                StoragePlatform.hasDownloadSAFPermission()
            }.getOrDefault(false)
            _uiState.emit(
                StorageManagementUIState.Success(
                    storageInfoData,
                    hasDownloadSAFPermission
                )
            )
        }
    }

    fun cleanAppCache() {
        viewModelScope.launch(Dispatchers.IO) {
            if (_uiState.value is StorageManagementUIState.Success) {
                StoragePlatform.clearCache()
                loadStorageInfo()
            }
        }
    }

    fun saveDownloadUri(uri: String) {
        viewModelScope.launch(Dispatchers.IO) {
            appSettingsRepository.saveDownloadSAFUriString(uri)
            loadStorageInfo()
        }
    }


}
