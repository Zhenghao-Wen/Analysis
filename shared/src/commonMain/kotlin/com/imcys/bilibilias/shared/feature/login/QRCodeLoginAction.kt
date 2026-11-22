package com.imcys.bilibilias.shared.feature.login

/**
 * 保存登录二维码到相册。
 *
 * 入参是**二维码承载的内容**（B 站扫码地址 + 回跳 deep link），不是图片地址。
 * 两端都在本地生成位图（Android 用 qrose painter 绘制到 Canvas，iOS 用矩阵直接绘制），
 * 不再请求任何云端出图服务。
 *
 * [darkColorArgb] / [lightColorArgb] 是界面显示二维码时使用的同一组颜色（ARGB），
 * 由界面从 [rememberQrColorPalette] 取出后传入，保证导出图片与屏幕显示一致，
 * 且始终是「深前景 + 浅背景」的非反色二维码。
 */
internal expect fun saveQRCodeImageToGalleryByPlatform(
    qrCodeContent: String?,
    darkColorArgb: Long,
    lightColorArgb: Long,
): Boolean

internal expect fun goToScanQRByPlatform(): Boolean
