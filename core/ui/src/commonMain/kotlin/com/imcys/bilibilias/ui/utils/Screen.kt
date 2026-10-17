package com.imcys.bilibilias.ui.utils

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.window.core.layout.WindowHeightSizeClass
import androidx.window.core.layout.WindowWidthSizeClass

/**
 * 窗口尺寸档位。
 *
 * 此前对外直接暴露 androidx.window 的 `WindowWidthSizeClass` / `WindowHeightSizeClass`。
 * 这两个类型已被上游标记为废弃（建议改用 `WindowSizeClass`），导致**每一个使用方**
 * 在 `when`/比较时都会产生废弃警告。
 *
 * 这里收敛为本地枚举：废弃类型只在下方两个辅助函数内部出现（并已局部抑制），
 * 对外只暴露本枚举，使用方不再引用废弃符号。取值与原枚举一一对应，行为不变。
 *
 * 待 Compose Multiplatform 提供稳定的 `WindowSizeClass` 替代 API 后，
 * 只需改造本文件的两个函数即可完成真正的迁移。
 */
enum class ASSizeClass { COMPACT, MEDIUM, EXPANDED }

/**
 * 按名称映射，避免对废弃枚举写穷尽 `when`（那样一旦上游新增取值就会编译失败，
 * 而减少分支又会产生"else 冗余"警告）。名称不匹配时回退到 COMPACT。
 */
private fun String.toASSizeClass(): ASSizeClass =
    ASSizeClass.entries.firstOrNull { it.name == this } ?: ASSizeClass.COMPACT

@Suppress("DEPRECATION")
@Composable
fun rememberWidthSizeClass(): ASSizeClass =
    currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass.name.toASSizeClass()

@Suppress("DEPRECATION")
@Composable
fun rememberHeightSizeClass(): ASSizeClass =
    currentWindowAdaptiveInfo().windowSizeClass.windowHeightSizeClass.name.toASSizeClass()
