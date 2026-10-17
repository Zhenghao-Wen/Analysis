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
 * 这里收敛为本地枚举：废弃类型只在下方两个函数内部出现（并已局部抑制），
 * 对外只暴露本枚举，使用方不再引用废弃符号。取值与原类型一一对应，行为不变。
 *
 * 注意：`WindowWidthSizeClass` / `WindowHeightSizeClass` **不是 Kotlin enum，
 * 而是带静态常量的普通类**（所以既没有 `.name`，也不能用于穷尽 `when`），
 * 因此这里只能用 `==` 与它们的静态常量逐一比较。
 *
 * 待 Compose Multiplatform 提供稳定的 `WindowSizeClass` 替代 API 后，
 * 只需改造本文件的两个函数即可完成真正的迁移。
 */
enum class ASSizeClass { COMPACT, MEDIUM, EXPANDED }

@Suppress("DEPRECATION")
@Composable
fun rememberWidthSizeClass(): ASSizeClass {
    val widthSizeClass = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass
    return when {
        widthSizeClass == WindowWidthSizeClass.EXPANDED -> ASSizeClass.EXPANDED
        widthSizeClass == WindowWidthSizeClass.MEDIUM -> ASSizeClass.MEDIUM
        else -> ASSizeClass.COMPACT
    }
}

@Suppress("DEPRECATION")
@Composable
fun rememberHeightSizeClass(): ASSizeClass {
    val heightSizeClass = currentWindowAdaptiveInfo().windowSizeClass.windowHeightSizeClass
    return when {
        heightSizeClass == WindowHeightSizeClass.EXPANDED -> ASSizeClass.EXPANDED
        heightSizeClass == WindowHeightSizeClass.MEDIUM -> ASSizeClass.MEDIUM
        else -> ASSizeClass.COMPACT
    }
}
