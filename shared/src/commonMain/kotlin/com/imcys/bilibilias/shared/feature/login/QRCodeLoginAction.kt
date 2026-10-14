package com.imcys.bilibilias.shared.feature.login

/**
 * 保存登录二维码到相册。
 *
 * 入参是**二维码承载的内容**（B 站扫码地址 + 回跳 deep link），不是图片地址。
 * Android 侧由 qrose 本地编码生成位图；iOS 侧暂仍走旧的云端出图路径（见各自实现）。
 */
internal expect fun saveQRCodeImageToGalleryByPlatform(qrCodeContent: String?): Boolean

internal expect fun goToScanQRByPlatform(): Boolean