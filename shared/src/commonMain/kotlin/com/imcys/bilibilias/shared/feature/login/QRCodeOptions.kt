package com.imcys.bilibilias.shared.feature.login

import androidx.compose.ui.graphics.Color
import io.github.alexzhirkevich.qrose.options.QrBallShape
import io.github.alexzhirkevich.qrose.options.QrBrush
import io.github.alexzhirkevich.qrose.options.QrColors
import io.github.alexzhirkevich.qrose.options.QrFrameShape
import io.github.alexzhirkevich.qrose.options.QrOptions
import io.github.alexzhirkevich.qrose.options.QrPixelShape
import io.github.alexzhirkevich.qrose.options.QrShapes
import io.github.alexzhirkevich.qrose.options.circle
import io.github.alexzhirkevich.qrose.options.roundCorners
import io.github.alexzhirkevich.qrose.options.solid

/**
 * 登录二维码的绘制参数（屏幕显示与保存到相册共用，保证两端内容与样式一致）。
 *
 * 样式：qrose 的 Material3 风格 —— 数据点用圆角方块、定位眼（ball/frame）用圆形，
 * 并由调用方传入主题色（定位眼可用 `primary` 取色），外围留出静区（`scale`）。
 *
 * 颜色注意：
 * - 浅色点**必须显式指定**。qrose 中 `QrColors.light` 默认是 `Unspecified`（透明），
 *   深色主题下透明底会与界面背景叠加，扫描端可能识别不了，因此默认给白色。
 * - 屏幕显示默认用「黑点 + 白底」，而不是把深浅反色：登录二维码必须保证绝大多数
 *   扫码器都能识别，反色二维码并非所有扫码器都支持。
 *   定位眼颜色可以跟随主题（见 [eyeColor]），但不建议改成低对比度的颜色。
 *
 * @param darkColor 数据点颜色
 * @param lightColor 背景（静区）颜色
 * @param eyeColor 定位眼（三个角上的方框与圆点）颜色，默认与数据点一致
 * @param quietZoneScale 二维码在画布内的缩放比例，小于 1 会在四周留出空白，
 *        避免被外层圆角容器裁掉四角
 */
fun defaultQrOptions(
    darkColor: Color = Color.Black,
    lightColor: Color = Color.White,
    eyeColor: Color = darkColor,
    quietZoneScale: Float = 0.92f,
): QrOptions {
    val darkBrush = QrBrush.solid(darkColor)
    val eyeBrush = QrBrush.solid(eyeColor)
    return QrOptions(
        colors = QrColors(
            dark = darkBrush,
            light = QrBrush.solid(lightColor),
            ball = eyeBrush,
            frame = eyeBrush,
        ),
        shapes = QrShapes(
            darkPixel = QrPixelShape.roundCorners(),
            ball = QrBallShape.circle(),
            // 注意：QrFrameShape.roundCorners 的 corner 参数没有默认值，必须显式传
            frame = QrFrameShape.roundCorners(corner = 0.35f),
        ),
        scale = quietZoneScale,
    )
}
