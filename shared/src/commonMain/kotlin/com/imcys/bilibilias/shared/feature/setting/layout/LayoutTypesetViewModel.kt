package com.imcys.bilibilias.shared.feature.setting.layout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.imcys.bilibilias.data.repository.AppSettingsRepository
import com.imcys.bilibilias.data.repository.withoutRemovedHomeLayoutTypes
import com.imcys.bilibilias.datastore.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LayoutTypesetViewModel(
    private val appSettingsRepository: AppSettingsRepository
) : ViewModel() {
    val appSettings = appSettingsRepository.appSettingsFlow

    private val _homeLayoutTypesetList = MutableStateFlow(emptyList<AppSettings.HomeLayoutItem>())

    val homeLayoutTypesetList = _homeLayoutTypesetList.asStateFlow()

    init {

        viewModelScope.launch {
            appSettingsRepository.asyncHomeLayoutTypesetList()
            appSettings.collect {
                // 必须过滤：DataStore 里可能残留已永久停用的项（轮播图 / 公告 / 旧版更新），
                // 直接赋值会让它们在「首页排版」里重新出现。
                _homeLayoutTypesetList.value = it.homeLayoutTypesetList.withoutRemovedHomeLayoutTypes()
            }
        }
    }

    fun moveLayoutItem(fromIndex: Int, toIndex: Int) {
        val currentList = _homeLayoutTypesetList.value.toMutableList()
        val item = currentList.removeAt(fromIndex)
        currentList.add(toIndex, item)
        _homeLayoutTypesetList.value = currentList
        // 这里可以添加保存到数据存储的逻辑
        viewModelScope.launch {
            appSettingsRepository.updateHomeLayoutTypesetList(currentList)
        }
    }


    fun setLayoutItemHidden( item: AppSettings.HomeLayoutItem, hidden: Boolean) {
        val currentList = _homeLayoutTypesetList.value.toMutableList()
        val index = currentList.indexOf(item)
        if (index != -1) {
            val newItem = item.copy(is_hidden = hidden)
            currentList[index] = newItem
            _homeLayoutTypesetList.value = currentList
            viewModelScope.launch {
                appSettingsRepository.updateHomeLayoutTypesetList(currentList)
            }
        }
    }



}
