package com.imcys.bilibilias.shared.feature.login

/**
 * 保存登录二维码到相册。
 *
 * 入参是**二维码承载的内容**（B 站扫码地址 + 回跳 deep link），不是图片地址。
 * 两端都在本地生成位图（Android 用 qrose painter 绘制到 Canvas，iOS 用矩阵直接绘制），
 * 不再请求任何云端出图服务。
 *
 * 颜色与边距由 [defaultQrOptions] / [QrQuietZoneModules] 统一决定：
 * 始终浅底深色，且四周保留 4 个模块的静区，保证导出图片可直接扫码。
 */
internal expect fun saveQRCodeImageToGalleryByPlatform(qrCodeContent: String?): Boolean

internal expect fun goToScanQRByPlatform(): Boolean
