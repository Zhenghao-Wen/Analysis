package com.imcys.bilibilias.shared.feature.login

import androidx.compose.ui.graphics.Color
import com.imcys.bilibilias.shared.platform.storage.saveImageToPhotos
import io.github.alexzhirkevich.qrose.matrix.Matrix2D
import io.github.alexzhirkevich.qrose.matrix.qr.QrEncoder
import io.github.alexzhirkevich.qrose.matrix.qr.QrErrorCorrection
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGContextFillRect
import platform.CoreGraphics.CGContextSetFillColorWithColor
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSURL
import platform.UIKit.UIColor
import platform.UIKit.UIApplication
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetCurrentContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext

/**
 * iOS 侧的「保存二维码到相册」。
 *
 * 与 Android 对齐：**完全本地生成**，不再把二维码内容交给 pan.misakamoe.com。
 * 颜色由界面传入（与屏幕显示同一套 Material 配色，且保证「深前景 + 浅背景」非反色），
 * 四周按 [QrQuietZoneModules] 个模块留出静区，导出图片可直接扫码。
 *
 * 说明：这里直接用矩阵（[QrEncoder]）配合 CoreGraphics 绘制方点，
 * 与屏幕上 qrose painter 的圆角风格略有差异，但颜色、内容、边距一致；
 * 若后续需要与屏幕完全一致，可改为把 Compose painter 光栅化（需要 Skiko）。
 */
@OptIn(ExperimentalForeignApi::class)
internal actual fun saveQRCodeImageToGalleryByPlatform(
    qrCodeContent: String?,
    darkColorArgb: Long,
    lightColorArgb: Long,
): Boolean {
    if (qrCodeContent.isNullOrBlank()) return false
    return runCatching {
        val matrix = QrEncoder(errorCorrectionLevel = QrErrorCorrection.L).encode(qrCodeContent)
        val side = 1024.0
        val quiet = side * QrQuietZoneModules / (matrix.width + 2 * QrQuietZoneModules)
        val moduleSize = (side - 2 * quiet) / matrix.width

        UIGraphicsBeginImageContextWithOptions(CGSizeMake(side, side), true, 1.0)
        val drawn = drawMatrix(
            matrix = matrix,
            quiet = quiet,
            moduleSize = moduleSize,
            side = side,
            foreground = Color(darkColorArgb).toUIColor(),
            background = Color(lightColorArgb).toUIColor(),
        )
        val image = if (drawn) UIGraphicsGetImageFromCurrentImageContext() else null
        UIGraphicsEndImageContext()

        if (image == null) false else saveImageToPhotos(image)
    }.getOrDefault(false)
}

/**
 * 在当前 UIKit 图形上下文里画二维码。
 *
 * 背景铺满白色，深色模块按矩阵绘制；[quiet] 为四周静区像素，[moduleSize] 为单模块边长。
 */
@OptIn(ExperimentalForeignApi::class)
private fun drawMatrix(
    matrix: Matrix2D,
    quiet: Double,
    moduleSize: Double,
    side: Double,
    foreground: UIColor,
    background: UIColor,
): Boolean {
    val context = UIGraphicsGetCurrentContext() ?: return false

    CGContextSetFillColorWithColor(context, background.CGColor)
    CGContextFillRect(context, CGRectMake(0.0, 0.0, side, side))

    CGContextSetFillColorWithColor(context, foreground.CGColor)
    for (y in 0 until matrix.height) {
        for (x in 0 until matrix.width) {
            if (!matrix[x, y]) continue
            CGContextFillRect(
                context,
                CGRectMake(
                    quiet + x * moduleSize,
                    quiet + y * moduleSize,
                    // 多画一点点，避免相邻模块之间出现亚像素缝隙（扫描时会被当成细线）
                    moduleSize + 0.5,
                    moduleSize + 0.5,
                )
            )
        }
    }
    return true
}

internal actual fun goToScanQRByPlatform(): Boolean {
    return runCatching {
        val url = NSURL.URLWithString("bilibili://qrscan") ?: return false
        UIApplication.sharedApplication.canOpenURL(url).also { canOpen ->
            if (canOpen) {
                UIApplication.sharedApplication.openURL(url)
            }
        }
    }.getOrDefault(false)
}

/** Compose Color（sRGB）转 UIKit 颜色，保证导出与屏幕同一套配色。 */
@OptIn(ExperimentalForeignApi::class)
private fun Color.toUIColor(): UIColor = UIColor(
    red = red.toDouble(),
    green = green.toDouble(),
    blue = blue.toDouble(),
    alpha = alpha.toDouble(),
)
