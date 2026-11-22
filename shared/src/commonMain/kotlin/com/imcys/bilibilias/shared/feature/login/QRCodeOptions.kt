package com.imcys.bilibilias.shared.feature.login

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import io.github.alexzhirkevich.qrose.matrix.qr.QrEncoder
import io.github.alexzhirkevich.qrose.matrix.qr.QrErrorCorrection
import io.github.alexzhirkevich.qrose.options.QrBackground
import io.github.alexzhirkevich.qrose.options.QrBallShape
import io.github.alexzhirkevich.qrose.options.QrBrush
import io.github.alexzhirkevich.qrose.options.QrColors
import io.github.alexzhirkevich.qrose.options.QrErrorCorrectionLevel
import io.github.alexzhirkevich.qrose.options.QrFrameShape
import io.github.alexzhirkevich.qrose.options.QrLogo
import io.github.alexzhirkevich.qrose.options.QrOptions
import io.github.alexzhirkevich.qrose.options.QrPixelShape
import io.github.alexzhirkevich.qrose.options.QrShapes
import io.github.alexzhirkevich.qrose.options.circle
import io.github.alexzhirkevich.qrose.options.roundCorners
import io.github.alexzhirkevich.qrose.options.solid

/** 二维码前景色（模块）与背景色（静区/画布）。 */
@Immutable
data class QrColorPalette(
    val foreground: Color,
    val background: Color,
)

/** 二维码四周应保留的静区宽度（单位：模块数），按规范至少 4 个模块。 */
const val QrQuietZoneModules: Int = 4

/** 前景/背景对比度下限，低于此值时退回更稳妥的 onSurface 组合。 */
private const val QrMinContrastRatio = 4.5f

/**
 * 从 Material 配色里挑一对「深前景 + 浅背景」的颜色。
 *
 * 关键点是**保证非反色**：二维码必须深色模块压浅色底，反色（浅模块 + 深底）
 * 并非所有扫码器都支持。但应用有浅色/深色两套主题，同一个语义色在两套主题下
 * 明暗是相反的，因此按主题分别取色：
 *
 * - 浅色主题：前景 `primary`（深），背景 `surfaceContainerLowest`（浅）
 * - 深色主题：前景 `inversePrimary`（深），背景 `inverseSurface`（浅）
 *
 * 深色主题之所以用 `inverse*`，是因为 Material 已经为「与当前表面相反」的场景
 * 生成了一组颜色：`inverseSurface` 在深色主题下是浅色，`inversePrimary` 是它的
 * 深色前景——正好满足深前景 + 浅背景。
 *
 * 另外加了两道兜底，避免动态取色产生极端调色板时出现反色或看不清：
 * 1. 若前景不比背景暗，直接交换两者；
 * 2. 若对比度低于 [QrMinContrastRatio]，退回 `onSurface` / `inverseOnSurface` 组合。
 */
fun qrColorPalette(scheme: ColorScheme): QrColorPalette {
    val isLightTheme = scheme.surface.luminance() >= 0.5f

    val lightBackground = scheme.surfaceContainerLowest
    val darkBackground = scheme.inverseSurface

    var background = if (isLightTheme) lightBackground else darkBackground
    var foreground = if (isLightTheme) scheme.primary else scheme.inversePrimary

    // 兜底 1：确保「深前景、浅背景」
    if (foreground.luminance() >= background.luminance()) {
        val swapped = foreground
        foreground = background
        background = swapped
    }

    // 兜底 2：对比度不足时改用 onSurface 系列（同主题下对比度最高的一对）
    if (contrastRatio(foreground, background) < QrMinContrastRatio) {
        foreground = if (isLightTheme) scheme.onSurface else scheme.inverseOnSurface
        background = if (isLightTheme) lightBackground else darkBackground
        if (foreground.luminance() >= background.luminance()) {
            val swapped = foreground
            foreground = background
            background = swapped
        }
    }

    return QrColorPalette(foreground = foreground, background = background)
}

/** WCAG 相对对比度，用于判断前景是否足够清晰。 */
private fun contrastRatio(a: Color, b: Color): Float {
    val la = a.luminance()
    val lb = b.luminance()
    val lighter = if (la > lb) la else lb
    val darker = if (la > lb) lb else la
    return (lighter + 0.05f) / (darker + 0.05f)
}

/** 当前主题下二维码应使用的配色（含非反色与对比度兜底）。 */
@Composable
fun rememberQrColorPalette(): QrColorPalette {
    val scheme = MaterialTheme.colorScheme
    return remember(scheme) { qrColorPalette(scheme) }
}

/**
 * 登录二维码的绘制参数（屏幕显示与保存到相册共用同一套参数）。
 *
 * Material3 风格：
 * - 形状：数据点为圆角方块、定位眼内点为圆形、定位眼外框圆角取 0.5
 *   （库内会 clamp 到 [0f, 0.5f]，0.5 表示圆角半径等于边长的一半，视觉上是圆形）；
 * - 颜色：由 [qrColorPalette] 从主题里选出「深前景 + 浅背景」的一对颜色，
 *   既保持 Material 色系，又保证任何主题下都不是反色二维码；
 * - 不使用 logo。
 *
 * @param scale 二维码在画布内的缩放比例。< 1 时画布四周会留出背景色边距
 *        （屏幕显示时用，避免被外层圆角容器裁掉四角）。
 */
fun defaultQrOptions(
    palette: QrColorPalette,
    scale: Float = 0.92f,
): QrOptions {
    val darkBrush = QrBrush.solid(palette.foreground)
    val lightBrush = QrBrush.solid(palette.background)
    return QrOptions(
        colors = QrColors(
            dark = darkBrush,
            light = lightBrush,
            // 定位眼的圆点与外框同样使用前景色
            ball = darkBrush,
            frame = darkBrush,
        ),
        // 整张画布铺底色：导出为位图时与界面显示一致
        background = QrBackground(fill = SolidColor(palette.background)),
        shapes = QrShapes(
            darkPixel = QrPixelShape.roundCorners(),
            ball = QrBallShape.circle(),
            // corner 在库内会 coerceIn(0f, .5f)，取 0.5 得到接近圆形的外框
            frame = QrFrameShape.roundCorners(corner = 0.5f),
        ),
        // 显式固定纠错等级，保证屏幕绘制与导出位图使用同一份矩阵
        errorCorrectionLevel = QrErrorCorrectionLevel.Low,
        // 不使用 logo（显式写默认空值，避免以后误加）
        logo = QrLogo(),
    )
}

/** 屏幕显示用的二维码参数：配色跟随主题，且保证非反色。 */
@Composable
fun rememberQrCodeOptions(): QrOptions {
    val palette = rememberQrColorPalette()
    return remember(palette) { defaultQrOptions(palette) }
}

/**
 * 二维码的模块数（横向格数）。
 *
 * 用于按「静区 = 4 个模块」计算导出位图的边距。编码失败时返回 null，
 * 调用方回退到按比例留边。
 */
fun qrCodeModuleCount(content: String): Int? = runCatching {
    QrEncoder(errorCorrectionLevel = QrErrorCorrection.L).encode(content).width
}.getOrNull()

/**
 * 导出位图时按静区模块数计算的边距（像素）。
 *
 * @param sizePx 位图边长
 * @param moduleCount 二维码模块数；为空时按 8% 比例回退
 */
fun qrCodeQuietZonePx(sizePx: Int, moduleCount: Int?): Float {
    if (moduleCount == null || moduleCount <= 0) return sizePx * 0.08f
    return sizePx.toFloat() * QrQuietZoneModules / (moduleCount + 2 * QrQuietZoneModules)
}
