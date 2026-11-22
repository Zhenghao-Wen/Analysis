package com.imcys.bilibilias.shared.platform.storage

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.UIKit.UIImage
import platform.UIKit.UIImageWriteToSavedPhotosAlbum

@OptIn(ExperimentalForeignApi::class)
internal fun saveImageUrlToPhotos(imageUrl: String?): Boolean {
    if (imageUrl.isNullOrBlank()) return false
    val url = NSURL.URLWithString(imageUrl) ?: return false
    val data = memScoped {
        val error = alloc<ObjCObjectVar<NSError?>>()
        NSData.dataWithContentsOfURL(url, options = 0u, error = error.ptr)
    } ?: return false
    return saveImageDataToPhotos(data)
}

@OptIn(ExperimentalForeignApi::class)
private fun saveImageDataToPhotos(data: NSData?): Boolean {
    return runCatching {
        data ?: return false
        val image = UIImage.imageWithData(data) ?: return false
        UIImageWriteToSavedPhotosAlbum(image, null, null, null)
        true
    }.getOrDefault(false)
}

/**
 * 把已经在本地生成好的 [UIImage] 写入相册。
 *
 * 供「本地渲染二维码后保存」等场景使用，避免再经过网络下载这一步。
 */
@OptIn(ExperimentalForeignApi::class)
internal fun saveImageToPhotos(image: UIImage): Boolean {
    return runCatching {
        UIImageWriteToSavedPhotosAlbum(image, null, null, null)
        true
    }.getOrDefault(false)
}
