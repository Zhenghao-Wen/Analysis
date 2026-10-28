package com.imcys.bilibilias.common.utils

import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.Build
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.os.Environment.getExternalStorageDirectory
import android.os.Environment.getExternalStoragePublicDirectory
import android.os.StatFs
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.documentfile.provider.DocumentFile
import com.imcys.bilibilias.datastore.*
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import java.io.File
import java.util.UUID

data class StorageInfoData(
    val totalBytes: Long,
    val usedBytes: Long,
    val availableBytes: Long,
    val appBytes: Long,
    val downloadBytes: Long,
    val cacheTotalBytes: Long
)

/** SAF 目录统计的深度上限，避免误授权存储根目录时长时间遍历。 */
private const val SAF_SCAN_MAX_DEPTH = 6

/** 单个目录下的条目上限。 */
private const val SAF_SCAN_MAX_ENTRIES_PER_DIR = 3000

object StorageUtil: KoinComponent {

    private val appSettingsStore: DataStore<AppSettings> by inject(
        qualifier = named("app_settings_datastore")
    )

    /**
     * 获取文件夹占用空间（递归统计）
     */
    private fun getFolderSize(file: File?): Long {
        if (file == null || !file.exists()) return 0L
        if (file.isFile) return file.length()
        var size = 0L
        file.listFiles()?.forEach {
            size += getFolderSize(it)
        }
        return size
    }

    /**
     * 获取APP私有目录占用空间（filesDir、cacheDir、codeCacheDir、externalCacheDir、externalFilesDir）
     */
    private fun getAppUsedBytes(context: Context): Long {
        var size = 0L
        val dirs = listOf(
            context.filesDir,
            context.cacheDir,
            context.codeCacheDir,
            context.externalCacheDir,
            context.getExternalFilesDir(null)
        )
        dirs.forEach { size += getFolderSize(it) }
        return size
    }

    // 递归统计 DocumentFile 大小
    // 加深度与条目上限：若用户把整个存储根目录授权给应用，无界递归会遍历全部文件，
    // 表现为界面长时间卡在「加载中」。
    private fun getDocumentFileSize(docFile: DocumentFile?, depth: Int = 0): Long {
        if (docFile == null || !docFile.exists()) return 0L
        if (docFile.isFile) return docFile.length()
        if (depth > SAF_SCAN_MAX_DEPTH) return 0L
        var size = 0L
        var visited = 0
        docFile.listFiles().forEach {
            visited++
            if (visited > SAF_SCAN_MAX_ENTRIES_PER_DIR) return size
            size += getDocumentFileSize(it, depth + 1)
        }
        return size
    }

    /**
     * 获取Download/BILIBILIAS文件夹占用空间（兼容原有方式）
     */
    private fun getDownloadUsedBytes(): Long {
        val bilibiliasDir =
            File(getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS), "BILIBILIAS")
        return getFolderSize(bilibiliasDir)
    }

    /**
     * 统计缓存、video、audio文件夹总大小
     */
    fun getCacheTotalBytes(context: Context): Long {
        val cacheSize = getFolderSize(context.cacheDir) + (context.externalCacheDir?.let { getFolderSize(it) } ?: 0L)
        val videoSize = getFolderSize(context.getExternalFilesDir("video"))
        val audioSize = getFolderSize(context.getExternalFilesDir("audio"))
        return cacheSize + videoSize + audioSize
    }




    /**
     * 返回主存储卷的空间信息，单位 Byte。
     * 如果失败返回所有字段为 -1。
     */
    suspend fun getStorageInfoData(context: Context): StorageInfoData {
        val total: Long
        val avail: Long
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val sm = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
                val stats =
                    context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
                val primaryVolume: StorageVolume = sm.primaryStorageVolume
                val dir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    primaryVolume.directory!!
                } else {
                    getExternalStorageDirectory()
                }
                val uuid: UUID = sm.getUuidForPath(dir)
                total = stats.getTotalBytes(uuid)
                val statFs = StatFs(dir.absolutePath)
                avail = statFs.availableBytes
            } catch (e: Exception) {
                return StorageInfoData(-1L, -1L, -1L, -1L, -1L, -1L)
            }
        } else {
            val path = getExternalStorageDirectory()
            val stat = StatFs(path.absolutePath)
            total = stat.totalBytes
            avail = stat.availableBytes
        }
        val used = total - avail
        val appBytes = getAppUsedBytes(context)
        val downloadBytes = getASDownloadFolderSize(context)
        val cacheTotalBytes = getCacheTotalBytes(context)
        return StorageInfoData(total, used, avail, appBytes, downloadBytes, cacheTotalBytes)
    }


    /**
     * 将字节数转换为合适的单位（MB、GB、TB），返回最小满足的单位字符串（大写）。
     * 例如：不满1GB则返回多少MB，不满1MB则返回多少KB。
     */
    fun formatSize(bytes: Long): String {
        val KB = 1024L
        val MB = KB * 1024
        val GB = MB * 1024
        val TB = GB * 1024
        return when {
            bytes >= TB -> String.format("%.2f TB", bytes / TB.toDouble())
            bytes >= GB -> String.format("%.2f GB", bytes / GB.toDouble())
            bytes >= MB -> String.format("%.2f MB", bytes / MB.toDouble())
            bytes >= KB -> String.format("%.2f KB", bytes / KB.toDouble())
            else -> "$bytes B"
        }
    }

    /**
     * 递归删除文件夹及其所有内容
     */
    private fun deleteFolderRecursively(file: File?): Boolean {
        if (file == null || !file.exists()) return true
        if (file.isFile) return file.delete()
        var success = true
        file.listFiles()?.forEach {
            success = success && deleteFolderRecursively(it)
        }
        return file.delete() && success
    }

    /**
     * 清空缓存、video、audio文件夹下所有内容
     * 返回是否全部成功
     */
    fun clearCache(context: Context): Boolean {
        val cacheDirs = listOf(
            context.cacheDir,
            context.externalCacheDir,
            context.getExternalFilesDir("video"),
            context.getExternalFilesDir("audio")
        )
        var allSuccess = true
        cacheDirs.forEach { dir ->
            allSuccess = allSuccess && deleteFolderRecursively(dir)
            // 删除后重新创建文件夹，避免影响后续缓存写入
            if (dir != null && !dir.exists()) dir.mkdirs()
        }
        return allSuccess
    }

    /**
     * 检查是否已获得BILIBILIAS文件夹的SAF权限
     */
    suspend fun hasASDownloadSAFPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return true
        val uriString = run {
            val appSettings = appSettingsStore.data.first()
            appSettings.downloadUri
        }
        if (uriString.isNullOrEmpty()) return false
        val uri = uriString.toUri()
        val docFile = DocumentFile.fromTreeUri(context, uri)
        return docFile != null && docFile.exists() && docFile.isDirectory
    }

    /**
     * 统计Download/BILIBILIAS文件夹及其所有子文件夹的大小
     * 优先使用SAF权限统计，否则用传统方式
     */
    suspend fun getASDownloadFolderSize(context: Context): Long {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return getDownloadUsedBytes()
        }
        val uriString = run {
            val appSettings = appSettingsStore.data.first()
            appSettings.downloadUri
        }
        if (!uriString.isNullOrEmpty()) {
            val uri = uriString.toUri()
            val docFile = DocumentFile.fromTreeUri(context, uri)
            if (docFile != null && docFile.exists() && docFile.isDirectory) {
                return getDocumentFileSize(docFile)
            }
        }
        return getDownloadUsedBytes()
    }
}
