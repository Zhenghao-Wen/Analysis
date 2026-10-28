package com.imcys.bilibilias.shared.platform.permission

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.imcys.bilibilias.shared.platform.runtime.koinApplication

actual fun hasStoragePermission(): Boolean {
    val context = koinApplication
    return if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
        ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

actual fun shouldRequestStoragePermission(): Boolean {
    return Build.VERSION.SDK_INT <= Build.VERSION_CODES.P
}

@Composable
actual fun rememberDownloadDirectoryPermissionController(
    onGranted: (String) -> Unit,
    onDenied: () -> Unit
): DownloadDirectoryPermissionController {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri == null) {
            onDenied()
        } else {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (ignored: Exception) {
            }
            onGranted(uri.toString())
        }
    }

    return remember(context, launcher, onGranted, onDenied) {
        object : DownloadDirectoryPermissionController {
            override val shouldRequest: Boolean
                get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

            override fun request() {
                // 默认定位到 Download/BILIBILIAS，而不是存储根目录：
                // 授权到 /sdcard 根目录既无意义，也会让占用统计遍历整块存储。
                val initialUri = runCatching {
                    DocumentsContract.buildDocumentUri(
                        "com.android.externalstorage.documents",
                        "primary:Download/BILIBILIAS"
                    )
                }.getOrNull()
                launcher.launch(initialUri ?: "content://com.android.externalstorage.documents/root/primary".toUri())
            }
        }
    }
}

@Composable
actual fun rememberNotificationPermissionController(
    onGranted: () -> Unit,
    onDenied: () -> Unit
): NotificationPermissionController {
    val context = LocalContext.current

    // 检查权限状态
    fun checkPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    var hasPermission by remember { mutableStateOf(checkPermission()) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) onGranted() else onDenied()
    }

    return remember(context, launcher) {
        object : NotificationPermissionController {
            override val hasPermission: Boolean
                get() = hasPermission

            override val shouldShow: Boolean
                @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
                get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

            override fun request() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    onGranted()
                }
            }
        }
    }
}
