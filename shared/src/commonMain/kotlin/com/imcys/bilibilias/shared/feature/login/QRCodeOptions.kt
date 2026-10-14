package com.imcys.bilibilias.shared.feature.login

import androidx.compose.ui.graphics.Color
import io.github.alexzhirkevich.qrose.options.QrBrush
import io.github.alexzhirkevich.qrose.options.QrColors
import io.github.alexzhirkevich.qrose.options.QrOptions
import io.github.alexzhirkevich.qrose.options.solid

/**
 * 登录二维码的绘制参数（屏幕显示与保存到相册共用，保证两端内容与样式一致）。
 *
 * 必须显式把浅色点设为白色：qrose 中 `QrColors.light` 默认为 `Unspecified`，
 * 即不绘制浅色像素（透明）。在深色主题下透明底会与界面背景叠加成深色，
 * 扫描端可能无法识别；因此固定为白底黑点。
 */
fun defaultQrOptions(): QrOptions = QrOptions(
    colors = QrColors(
        dark = QrBrush.Default,
        light = QrBrush.solid(Color.White),
    )
)
