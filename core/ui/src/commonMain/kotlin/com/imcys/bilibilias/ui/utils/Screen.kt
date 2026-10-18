// 文件级抑制：`WindowWidthSizeClass` / `WindowHeightSizeClass` 已在上游被标记废弃
// （提示改用 `WindowSizeClass`），而**废弃警告会记在 import 语句上**，
// 函数级 @Suppress 覆盖不到 import，所以必须用 @file:Suppress。
//
// 为什么暂不直接迁移到 WindowSizeClass：
//   1. 其替代 API（如 isWidthAtLeastBreakpoint）无法在本机验证（开发者环境无编译能力，
//      且该类型的 Android 变体未独立发布），盲改会再消耗一轮 CI 并可能引入行为差异；
//   2. 真正的收益是"对外不再暴露废弃类型"，这一点已由下方 ASSizeClass 达成——
//      本文件是**全仓库唯一**仍接触这两个废弃类型的地方（仅 2 处），
//      迁移的边际收益很小。
// 迁移建议：确认 WindowSizeClass 的断点 API 后，只需改下面两个函数即可。
@file:Suppress("DEPRECATION")

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
 * 这里收敛为本地枚举：废弃类型只在下方两个函数内部出现（由文件级 @Suppress 抑制），
 * 对外只暴露本枚举，使用方不再引用废弃符号。取值与原类型一一对应，行为不变。
 *
 * 注意：`WindowWidthSizeClass` / `WindowHeightSizeClass` **不是 Kotlin enum，
 * 而是带静态常量的普通类**（所以既没有 `.name`，也不能用于穷尽 `when`），
 * 因此这里只能用 `==` 与它们的静态常量逐一比较。
 */
enum class ASSizeClass { COMPACT, MEDIUM, EXPANDED }

@Composable
fun rememberWidthSizeClass(): ASSizeClass {
    val widthSizeClass = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass
    return when {
        widthSizeClass == WindowWidthSizeClass.EXPANDED -> ASSizeClass.EXPANDED
        widthSizeClass == WindowWidthSizeClass.MEDIUM -> ASSizeClass.MEDIUM
        else -> ASSizeClass.COMPACT
    }
}

@Composable
fun rememberHeightSizeClass(): ASSizeClass {
    val heightSizeClass = currentWindowAdaptiveInfo().windowSizeClass.windowHeightSizeClass
    return when {
        heightSizeClass == WindowHeightSizeClass.EXPANDED -> ASSizeClass.EXPANDED
        heightSizeClass == WindowHeightSizeClass.MEDIUM -> ASSizeClass.MEDIUM
        else -> ASSizeClass.COMPACT
    }
}
