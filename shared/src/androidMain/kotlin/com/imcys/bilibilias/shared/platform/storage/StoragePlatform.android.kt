package com.imcys.bilibilias.shared.platform.storage

import android.app.usage.StorageStatsManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.provider.DocumentsContract
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.imcys.bilibilias.shared.util.StorageInfoData
import com.imcys.bilibilias.data.repository.AppSettingsRepository
import com.imcys.bilibilias.datastore.downloadUri
import com.imcys.bilibilias.shared.platform.runtime.koinApplication
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import org.koin.mp.KoinPlatform.getKoin
import java.io.File
import java.util.UUID
import kotlin.system.exitProcess

/** SAF 体积扫描的深度上限，防止误授权存储根目录时长时间卡死。 */
private const val SAF_SCAN_MAX_DEPTH = 6

/** SAF 体积扫描的条目上限。 */
private const val SAF_SCAN_MAX_ENTRIES = 3000

actual object StoragePlatform {
    private val appSettingsRepository: AppSettingsRepository
        get() = getKoin().get()

    actual suspend fun getStorageInfoData(): StorageInfoData {
        val total: Long
        val avail: Long
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val sm = koinApplication.getSystemService(android.content.Context.STORAGE_SERVICE) as StorageManager
                val stats =
                    koinApplication.getSystemService(android.content.Context.STORAGE_STATS_SERVICE) as StorageStatsManager
                val primaryVolume: StorageVolume = sm.primaryStorageVolume
                val dir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    primaryVolume.directory!!
                } else {
                    Environment.getExternalStorageDirectory()
                }
                val uuid: UUID = sm.getUuidForPath(dir)
                total = stats.getTotalBytes(uuid)
                val statFs = StatFs(dir.absolutePath)
                avail = statFs.availableBytes
            } catch (e: Exception) {
                return StorageInfoData(-1L, -1L, -1L, -1L, -1L, -1L)
            }
        } else {
            val path = Environment.getExternalStorageDirectory()
            val stat = StatFs(path.absolutePath)
            total = stat.totalBytes
            avail = stat.availableBytes
        }

        val used = total - avail
        val appBytes = getAppUsedBytes()
        val downloadBytes = getDownloadFolderSize()
        val cacheTotalBytes = getCacheTotalBytes()
        return StorageInfoData(total, used, avail, appBytes, downloadBytes, cacheTotalBytes)
    }

    actual suspend fun hasDownloadSAFPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return true
        val uriString = appSettingsRepository.appSettingsFlow.first().downloadUri
        if (uriString.isEmpty()) return false
        val docFile = DocumentFile.fromTreeUri(koinApplication, uriString.toUri())
        return docFile != null && docFile.exists() && docFile.isDirectory
    }

    actual fun clearCache(): Boolean {
        val cacheDirs = listOf(
            koinApplication.cacheDir,
            koinApplication.externalCacheDir,
            koinApplication.getExternalFilesDir("video"),
            koinApplication.getExternalFilesDir("audio")
        )
        var allSuccess = true
        cacheDirs.forEach { dir ->
            allSuccess = allSuccess && deleteFolderRecursively(dir)
            if (dir != null && !dir.exists()) dir.mkdirs()
        }
        return allSuccess
    }

    actual fun openDownloadDirectory(): Boolean {
        val targetDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "BILIBILIAS"
        ).apply {
            if (!exists()) mkdirs()
        }
        val relativePath = targetDir.absolutePath.substringAfter("/storage/emulated/0/")
        val downloadUri = DocumentsContract.buildDocumentUri(
            "com.android.externalstorage.documents",
            "primary:$relativePath"
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(downloadUri, "vnd.android.document/directory")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            koinApplication.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }

    actual fun restartApplication() {
        val packageManager = koinApplication.packageManager
        val intent = packageManager.getLaunchIntentForPackage(koinApplication.packageName)
        val mainIntent = Intent.makeRestartActivityTask(intent!!.component)
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        koinApplication.startActivity(mainIntent)
        exitProcess(0)
    }

    private fun getFolderSize(file: File?): Long {
        if (file == null || !file.exists()) return 0L
        if (file.isFile) return file.length()
        var size = 0L
        file.listFiles()?.forEach {
            size += getFolderSize(it)
        }
        return size
    }

    private fun getAppUsedBytes(): Long {
        var size = 0L
        val dirs = listOf(
            koinApplication.filesDir,
            koinApplication.cacheDir,
            koinApplication.codeCacheDir,
            koinApplication.externalCacheDir,
            koinApplication.getExternalFilesDir(null)
        )
        dirs.forEach { size += getFolderSize(it) }
        return size
    }

    /**
     * 统计 SAF 目录占用。
     *
     * 必须设上限：用户可能把**整个存储根目录**（如 /sdcard）授权给应用，
     * 此时无界递归会遍历所有文件，表现就是设置页一直卡在「加载中」。
     * 这里限制深度与条目数，并在遍历中响应协程取消。
     */
    private suspend fun getDocumentFileSizeBounded(root: DocumentFile?): Long {
        if (root == null || !root.exists()) return 0L
        if (root.isFile) return root.length()

        var size = 0L
        var visited = 0
        val queue = ArrayDeque<Pair<DocumentFile, Int>>()
        queue.addLast(root to 0)

        while (queue.isNotEmpty()) {
            currentCoroutineContext().ensureActive()
            val (dir, depth) = queue.removeFirst()
            if (depth > SAF_SCAN_MAX_DEPTH) continue
            val children = runCatching { dir.listFiles() }.getOrNull() ?: continue
            for (child in children) {
                visited++
                if (visited > SAF_SCAN_MAX_ENTRIES) {
                    // 达到上限就停下，宁可给出偏小的数字，也不让界面卡死
                    return size
                }
                if (child.isFile) {
                    size += runCatching { child.length() }.getOrDefault(0L)
                } else {
                    queue.addLast(child to depth + 1)
                }
            }
        }
        return size
    }

    /**
     * 授权的树是否是「某个存储卷的根目录」。
     *
     * `getTreeDocumentId` 对根目录返回形如 `primary:` 的 id，
     * 对子目录返回 `primary:Download/BILIBILIAS`。
     */
    private fun isStorageRootTree(uriString: String): Boolean {
        return runCatching {
            val treeId = DocumentsContract.getTreeDocumentId(uriString.toUri())
            treeId.endsWith(":")
        }.getOrDefault(false)
    }

    private fun getDownloadUsedBytes(): Long {
        val bilibiliasDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "BILIBILIAS"
        )
        return getFolderSize(bilibiliasDir)
    }

    private fun getCacheTotalBytes(): Long {
        val cacheSize = getFolderSize(koinApplication.cacheDir) +
                (koinApplication.externalCacheDir?.let { getFolderSize(it) } ?: 0L)
        val videoSize = getFolderSize(koinApplication.getExternalFilesDir("video"))
        val audioSize = getFolderSize(koinApplication.getExternalFilesDir("audio"))
        return cacheSize + videoSize + audioSize
    }

    private suspend fun getDownloadFolderSize(): Long {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return getDownloadUsedBytes()
        }
        val uriString = appSettingsRepository.appSettingsFlow.first().downloadUri
        if (uriString.isNotEmpty()) {
            // 授权到存储根目录时不统计：一是无意义（统计的是整块存储），
            // 二是遍历代价极大。此时回退到应用自己的下载目录。
            if (!isStorageRootTree(uriString)) {
                val docFile = DocumentFile.fromTreeUri(koinApplication, uriString.toUri())
                if (docFile != null && docFile.exists() && docFile.isDirectory) {
                    return getDocumentFileSizeBounded(docFile)
                }
            }
        }
        return getDownloadUsedBytes()
    }

    private fun deleteFolderRecursively(file: File?): Boolean {
        if (file == null || !file.exists()) return true
        if (file.isFile) return file.delete()
        var success = true
        file.listFiles()?.forEach {
            success = success && deleteFolderRecursively(it)
        }
        return file.delete() && success
    }
}
