package com.imcys.bilibilias.shared.feature.login

import android.app.Application
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import io.github.alexzhirkevich.qrose.QrCodePainter
import org.koin.mp.KoinPlatform.getKoin
import java.io.File
import java.io.FileOutputStream

/**
 * 保存登录二维码到相册。
 *
 * 入参是「二维码承载的内容」（B 站扫码地址 + 回跳 deep link），不是图片地址：
 * 二维码由本机用 qrose 纯本地编码生成，不再请求 pan.misakamoe.com 出图，
 * 因此也不会把带 qrcode_key 的登录地址发给第三方。
 */
internal actual fun saveQRCodeImageToGalleryByPlatform(
    qrCodeContent: String?,
    darkColorArgb: Long,
    lightColorArgb: Long,
): Boolean {
    if (qrCodeContent.isNullOrBlank()) return false
    return runCatching {
        val application = getKoin().get<Application>()
        val bitmap = renderQrCodeBitmap(qrCodeContent, darkColorArgb, lightColorArgb)
        saveBitmapToGallery(application, bitmap)
        true
    }.getOrDefault(false)
}

/**
 * 用 qrose 在本地把二维码内容渲染成位图（不联网）。
 *
 * 说明：
 * - 颜色由界面传入（[QrColorPalette]），与屏幕显示完全一致；
 *   palette 由 qrColorPalette() 按主题挑选，保证「深前景 + 浅背景」的非反色二维码；
 * - 四周按「4 个模块」的静区留白（[qrCodeQuietZonePx]），避免贴边导致识别率下降；
 * - 此处**新建** QrCodePainter 而不是复用界面上那个：CachedPainter 会缓存
 *   DrawScope 与绘制尺寸，复用到不同尺寸的 Canvas 上可能命中过期缓存。
 */
private fun renderQrCodeBitmap(
    content: String,
    darkColorArgb: Long,
    lightColorArgb: Long,
    sizePx: Int = 1024,
): Bitmap {
    val palette = QrColorPalette(
        foreground = Color(darkColorArgb),
        background = Color(lightColorArgb),
    )
    val quietPx = qrCodeQuietZonePx(sizePx, qrCodeModuleCount(content))
    val innerPx = (sizePx - 2 * quietPx).coerceAtLeast(1f)

    // 屏幕显示用 0.92 缩放留白；导出时缩放交给外层 quietPx 控制
    val painter = QrCodePainter(data = content, options = defaultQrOptions(palette, scale = 1f))
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    // 先用二维码背景色铺满，避免 JPEG 把透明区域压成黑色
    Canvas(bitmap).drawColor(palette.background.toArgb())
    CanvasDrawScope().draw(
        density = Density(density = 1f),
        layoutDirection = LayoutDirection.Ltr,
        canvas = androidx.compose.ui.graphics.Canvas(bitmap.asImageBitmap()),
        size = Size(sizePx.toFloat(), sizePx.toFloat())
    ) {
        translate(quietPx, quietPx) {
            with(painter) {
                draw(Size(innerPx, innerPx))
            }
        }
    }
    return bitmap
}

internal actual fun goToScanQRByPlatform(): Boolean {
    return runCatching {
        val application = getKoin().get<Application>()
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("bilibili://qrscan")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        application.startActivity(intent)
        true
    }.getOrDefault(false)
}

private fun saveBitmapToGallery(application: Application, bitmap: Bitmap) {
    val contentResolver = application.contentResolver
    val fileName = "QR_${System.currentTimeMillis()}.jpeg"
    val relativePath = "${Environment.DIRECTORY_PICTURES}/BILIBILIAS"

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
        }
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: return
        contentResolver.openOutputStream(uri, "w")?.use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        }
    } else {
        @Suppress("DEPRECATION")
        val picturesDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val dir = File(picturesDir, "BILIBILIAS")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, fileName)
        FileOutputStream(file).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
        }
        val values = ContentValues().apply {
            @Suppress("DEPRECATION")
            put(MediaStore.Images.Media.DATA, file.absolutePath)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }
        contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    }
}
