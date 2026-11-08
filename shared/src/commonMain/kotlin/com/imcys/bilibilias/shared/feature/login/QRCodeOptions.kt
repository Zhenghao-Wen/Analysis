package com.imcys.bilibilias.shared.feature.login

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import io.github.alexzhirkevich.qrose.options.QrBackground
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
 * 登录二维码的绘制参数（屏幕显示与保存到相册**共用同一套参数**，保证两端效果一致）。
 *
 * Material3 风格：
 * - 形状：数据点为圆角方块、定位眼内点为圆形、定位眼外框圆角取 0.5（库内会 clamp 到
 *   [0f, 0.5f]，0.5 表示外框圆角半径等于边长的一半，视觉上是圆形）；
 * - 颜色：前景（数据点与定位眼）取主题 `primary`（动态取色时随壁纸变化），
 *   底色取 MD3 容器背景色；`light` 与 `background` 用同一颜色，
 *   使静区（quiet zone）与画布底色一致，导出图片与界面显示相同；
 * - 不使用 logo（保持默认的空 logo）。
 *
 * 注意：`light` 必须显式指定。qrose 中 `QrColors.light` 默认是 `Unspecified`（透明），
 * 透明底在深色主题下会与背景叠加，扫描端可能识别不了。
 *
 * @param darkColor 前景色（数据点、定位眼），界面传主题 primary
 * @param lightColor 底色（静区与画布背景），界面传 MD3 容器背景色
 */
fun defaultQrOptions(
    darkColor: Color,
    lightColor: Color,
): QrOptions {
    val darkBrush = QrBrush.solid(darkColor)
    val lightBrush = QrBrush.solid(lightColor)
    return QrOptions(
        colors = QrColors(
            dark = darkBrush,
            light = lightBrush,
            // 定位眼的圆点与外框跟随前景色
            ball = darkBrush,
            frame = darkBrush,
        ),
        // 整张画布铺底色：导出为位图时与界面显示一致
        background = QrBackground(fill = SolidColor(lightColor)),
        shapes = QrShapes(
            darkPixel = QrPixelShape.roundCorners(),
            ball = QrBallShape.circle(),
            // corner 在库内会 coerceIn(0f, .5f)，取 0.5 得到接近圆形的外框
            frame = QrFrameShape.roundCorners(corner = 0.5f),
        ),
        // 不使用 logo（显式写默认空值，避免以后误加）
        logo = io.github.alexzhirkevich.qrose.options.QrLogo(),
    )
}

/**
 * 界面显示用的二维码参数：跟随当前 MD3 主题（含动态取色、深浅色自动切换）。
 *
 * 保存到相册时传入同一组颜色（见 [saveQRCodeImageToGalleryByPlatform]），
 * 从而保证「界面看到的」与「保存下来的」完全一致。
 */
@Composable
fun rememberQrCodeOptions(): QrOptions {
    val primary = MaterialTheme.colorScheme.primary
    val container = MaterialTheme.colorScheme.surfaceContainer
    return remember(primary, container) {
        defaultQrOptions(darkColor = primary, lightColor = container)
    }
}
