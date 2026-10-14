package com.imcys.bilibilias.shared.feature.login

import com.imcys.bilibilias.shared.platform.storage.saveImageUrlToPhotos
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

/**
 * iOS 侧的「保存二维码到相册」。
 *
 * 注意：当前仅完成签名对齐，**逻辑仍是旧的云端出图路径**（把内容拼成
 * pan.misakamoe.com 的图片地址后下载保存）。Android 侧已改为 qrose 本地生成，
 * iOS 侧因本机无 Xcode、无法验证 Skia 渲染与相册写入，暂未一并改造。
 *
 * TODO(iOS)：与 Android 对齐——用 qrose 本地渲染到位图，再写入相册，
 * 从而彻底移除对 pan.misakamoe.com 的依赖。
 */
private const val LEGACY_QR_IMAGE_PROVIDER = "https://pan.misakamoe.com/qrcode/?url="

internal actual fun saveQRCodeImageToGalleryByPlatform(qrCodeContent: String?): Boolean {
    if (qrCodeContent.isNullOrBlank()) return false
    // 旧行为：把二维码内容交给云端出图服务，再下载保存（待 iOS 侧改为本地生成）
    return saveImageUrlToPhotos(LEGACY_QR_IMAGE_PROVIDER + qrCodeContent.encodeForQuery())
}

/** 极简 query 转义，仅用于拼接上述旧服务地址。 */
private fun String.encodeForQuery(): String {
    val sb = StringBuilder()
    for (ch in this) {
        when {
            ch.isLetterOrDigit() || ch in "-_.~" -> sb.append(ch)
            else -> for (b in ch.toString().encodeToByteArray()) {
                sb.append('%')
                    .append((b.toInt() and 0xFF).toString(16).uppercase().padStart(2, '0'))
            }
        }
    }
    return sb.toString()
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
