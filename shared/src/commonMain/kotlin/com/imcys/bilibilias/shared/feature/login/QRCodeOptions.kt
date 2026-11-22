package com.imcys.bilibilias.shared.feature.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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

/**
 * 二维码的高对比配色：**始终浅底深色**，不随应用主题变化。
 *
 * 原因：扫码器依赖深色模块与浅色底的对比度。深色主题下如果让前景色跟随主题
 * （例如 primary 在深色主题偏亮），或底色跟随深色容器，都会显著降低识别率，
 * 甚至出现「反色二维码」——部分扫码器不支持反色。
 */
val QrForegroundColor: Color = Color.Black
val QrBackgroundColor: Color = Color.White

/** 二维码四周应保留的静区宽度（单位：模块数），按规范至少 4 个模块。 */
const val QrQuietZoneModules: Int = 4

/**
 * 登录二维码的绘制参数（屏幕显示与保存到相册共用同一套参数）。
 *
 * Material3 风格：
 * - 形状：数据点为圆角方块、定位眼内点为圆形、定位眼外框圆角取 0.5
 *   （库内会 clamp 到 [0f, 0.5f]，0.5 表示圆角半径等于边长的一半，视觉上是圆形）；
 * - 颜色：前景固定黑色、底色固定白色（见 [QrForegroundColor] / [QrBackgroundColor]），
 *   保证任何主题下都可扫；`light` 与 `background` 用同一颜色，使静区与画布底色一致；
 * - 不使用 logo。
 *
 * @param scale 二维码在画布内的缩放比例。< 1 时画布四周会留出浅色边距
 *        （屏幕显示时用，避免被外层圆角容器裁掉四角）。
 */
fun defaultQrOptions(
    scale: Float = 0.92f,
): QrOptions {
    val darkBrush = QrBrush.solid(QrForegroundColor)
    val lightBrush = QrBrush.solid(QrBackgroundColor)
    return QrOptions(
        colors = QrColors(
            dark = darkBrush,
            light = lightBrush,
            // 定位眼的圆点与外框同样使用前景色
            ball = darkBrush,
            frame = darkBrush,
        ),
        // 整张画布铺底色：导出为位图时与界面显示一致
        background = QrBackground(fill = SolidColor(QrBackgroundColor)),
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

/** 屏幕显示用的二维码参数。颜色固定为浅底深色，因此无需依赖主题。 */
@Composable
fun rememberQrCodeOptions(): QrOptions = remember { defaultQrOptions() }

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
